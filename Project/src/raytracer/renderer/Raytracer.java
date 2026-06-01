package raytracer.renderer;

import raytracer.core.Intersection;
import raytracer.core.Ray;
import raytracer.core.Scene;
import raytracer.lighting.Light;
import raytracer.lighting.LightSample;
import raytracer.lighting.SoftShadowSettings;
import raytracer.material.Material;

import raytracer.utils.Color;
import raytracer.utils.Point3D;
import raytracer.utils.Vector3D;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

public class Raytracer {
    private Scene scene;
    private Camera camera;
    private int width;
    private int height;
    private Color backgroundColor;
    private final int threadCount;
    private final int tileSize;
    private final SoftShadowSettings softShadowSettings;
    private final int maxRayDepth;
    static final double SHADOW_EPSILON = 1e-4;
    static final double SECONDARY_RAY_EPSILON = 1e-4;
    static final int SHADOW_TRANSMITTANCE_MAX_STEPS = 32;
    static final double MIN_SHADOW_TRANSMITTANCE = 1e-3;
    static final float ambient =  0.05f;

    public Raytracer(Scene scene, Camera camera, int width, int height, Color backgroundColor) {
        this(
            scene,
            camera,
            width,
            height,
            backgroundColor,
            Math.max(1, Runtime.getRuntime().availableProcessors()),
            32,
            1,
            0.0,
            0.0,
            0.0,
            4
        );
    }

    public Raytracer(Scene scene, Camera camera, int width, int height, Color backgroundColor, int threadCount, int tileSize) {
        this(scene, camera, width, height, backgroundColor, threadCount, tileSize, 1, 0.0, 0.0, 0.0, 4);
    }

    public Raytracer(
        Scene scene,
        Camera camera,
        int width,
        int height,
        Color backgroundColor,
        int threadCount,
        int tileSize,
        int softShadowSamples,
        double pointLightRadius,
        double spotLightRadius,
        double directionalLightAngleDegrees,
        int maxRayDepth
    ) {
        this.scene = scene;
        this.camera = camera;
        this.width = width;
        this.height = height;
        this.backgroundColor = backgroundColor;
        this.threadCount = Math.max(1, threadCount);
        this.tileSize = Math.max(8, tileSize);
        this.softShadowSettings = new SoftShadowSettings(
            softShadowSamples,
            pointLightRadius,
            spotLightRadius,
            directionalLightAngleDegrees
        );
        this.maxRayDepth = Math.max(0, maxRayDepth);
    }

    public void render(String outputPath) {
        long renderStartNs = System.nanoTime();
        System.out.println("Rendering with " + threadCount + " threads (tile size: " + tileSize + ")");
        System.out.println(
            "Shadow mode: " + (softShadowSettings.getSoftShadowSamples() > 1 ? "soft" : "hard")
                + " (samples=" + softShadowSettings.getSoftShadowSamples()
                + ", pointRadius=" + softShadowSettings.getPointLightRadius()
                + ", spotRadius=" + softShadowSettings.getSpotLightRadius()
                + ", directionalAngle=" + softShadowSettings.getDirectionalLightAngleDegrees() + " deg)"
        );
        System.out.println("Recursion max depth: " + maxRayDepth);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] frameBuffer = new int[width * height];

        int tilesX = (width + tileSize - 1) / tileSize;
        int tilesY = (height + tileSize - 1) / tileSize;
        int totalTiles = tilesX * tilesY;
        AtomicInteger nextTile = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> tasks = new ArrayList<>(threadCount);

        for (int workerId = 0; workerId < threadCount; workerId++) {
            tasks.add(executor.submit(() -> {
                while (true) {
                    int tileIndex = nextTile.getAndIncrement();
                    if (tileIndex >= totalTiles) {
                        return;
                    }

                    int tileX = tileIndex % tilesX;
                    int tileY = tileIndex / tilesX;

                    int startX = tileX * tileSize;
                    int startY = tileY * tileSize;
                    int endX = Math.min(startX + tileSize, width);
                    int endY = Math.min(startY + tileSize, height);

                    for (int y = startY; y < endY; y++) {
                        int rowOffset = y * width;
                        for (int x = startX; x < endX; x++) {
                            Ray ray = camera.generateRay(x, y);
                            Color pixelColor = tracePrimaryRay(ray);
                            frameBuffer[rowOffset + x] = pixelColor.toRGB();
                        }
                    }
                }
            }));
        }

        try {
            for (Future<?> task : tasks) {
                task.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Render interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Render task failed", e.getCause());
        } finally {
            executor.shutdown();
        }

        image.setRGB(0, 0, width, height, frameBuffer, 0, width);

        long renderEndNs = System.nanoTime();

        try {
            long writeStartNs = System.nanoTime();
            ImageIO.write(image, "png", new File(outputPath));
            long writeEndNs = System.nanoTime();
            System.out.println("Image saved to: " + outputPath);
            double renderMs = (renderEndNs - renderStartNs) / 1_000_000.0;
            double writeMs  = (writeEndNs - writeStartNs) / 1_000_000.0;
            double totalMs  = (writeEndNs - renderStartNs) / 1_000_000.0;

            System.out.printf("Render: %.2f ms%n", renderMs);
            System.out.printf("Image write: %.2f ms%n", writeMs);
            System.out.printf("Total: %.2f ms%n", totalMs);
        } catch (Exception e) {
            System.err.println("Error saving image: " + e.getMessage());
        }
    }

    private Color tracePrimaryRay(Ray ray) {
        Intersection closest = findClosestIntersection(ray, camera.getNearPlane(), camera.getFarPlane());
        return traceRay(ray, closest, 0, 1.0);
    }

    private Color traceRay(Ray ray, Intersection closest, int depth, double currentIor) {
        if (closest == null) {
            return backgroundColor;
        }

        Material material = closest.getObject().getMaterial();
        Color objectColor = material.sampleAlbedo(closest);

        Vector3D geometricNormal = closest.getNormal();
        if (geometricNormal == null) {
            return objectColor;
        }

        Vector3D normal = material.sampleNormal(closest, geometricNormal);
        Color objectSpecularColor = material.getSpecularColor();
        double objectSpecularStrength = material.getSpecularStrength();
        double objectShininess = material.sampleShininess(closest);
        boolean hasSpecular = objectSpecularStrength > 0.0;
        //get vector from point to where ray was originated which is V in the blinn-phong model, if no specular component is needed, we can skip this calculation
        Vector3D viewDirection = hasSpecular ? ray.getDirection().multiply(-1.0).normalize() : null;
        Color litColor = scene.getLights().isEmpty() ? objectColor : objectColor.multiply(ambient);

        for (Light light : scene.getLights()) {
            int sampleCount = light.getSoftSampleCount(softShadowSettings);
            Color lightAccumulation = Color.BLACK;

            for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
                LightSample sample = light.sampleSoftAt(closest.getPoint(), sampleIndex, sampleCount, softShadowSettings);

                //light too far away, no contribution or too close to surface no contribution
                if (sample.getMaxDistance() <= 0.0) {
                    continue;
                }

                double nDotL = normal.dot(sample.getDirectionToLight());
                //light behind normal, no contribution
                if (nDotL <= 0.0) {
                    continue;
                }

                Color shadowTransmittance = traceShadowTransmittance(closest.getPoint(), geometricNormal, sample, sample.getMaxDistance());
                if (isNearlyBlack(shadowTransmittance)) {
                    continue;
                }

                double diffuseScale = sample.getRadianceScale() * nDotL;
                Color contribution = sample.getColor().multiply(objectColor).multiply(shadowTransmittance).multiply(diffuseScale);

                if (!hasSpecular) {
                    lightAccumulation = lightAccumulation.add(contribution);
                    continue;
                }

                Vector3D halfVector = sample.getDirectionToLight().add(viewDirection).normalize();
                double nDotH = Math.max(0.0, normal.dot(halfVector));
                //calculate contribution for specular
                double specularScale = sample.getRadianceScale() * objectSpecularStrength * Math.pow(nDotH, objectShininess);
                Color specularContribution = sample.getColor().multiply(objectSpecularColor).multiply(shadowTransmittance).multiply(specularScale);

                lightAccumulation = lightAccumulation.add(contribution).add(specularContribution);
            }

            if (sampleCount > 1) {
                lightAccumulation = lightAccumulation.multiply(1.0 / sampleCount);
            }
            litColor = litColor.add(lightAccumulation);
        }

        if (depth >= maxRayDepth) {
            return litColor;
        }

        double effectiveMetalness = clamp01(material.getMetalness());
        //metalness increases reflection and dupresses refraction and transparency
        double effectiveReflectivity = clamp01(material.getReflectivity() + effectiveMetalness * (1.0 - material.getReflectivity()));
        double effectiveTransmission = clamp01(material.getTransmission() * (1.0 - effectiveMetalness));
        if (effectiveReflectivity <= 0.0 && effectiveTransmission <= 0.0) {
            return litColor;
        }

        //incident
        Vector3D i = ray.getDirection().normalize();
        //normal
        Vector3D n = normal.normalize();
        //check if we are entering or leaving media, so we now if we need to flip normal
        boolean frontFace = i.dot(n) < 0.0;
        Vector3D orientedNormal = frontFace ? n : n.multiply(-1.0);

        double n1 = currentIor;
        double n2 = frontFace ? Math.max(1.0, material.getIor()) : 1.0;
        double cosTheta = Math.max(0.0, -i.dot(orientedNormal));
        double fresnel = effectiveTransmission > 0.0 ? schlickFresnel(cosTheta, n1, n2) : 1.0;

        double reflectionWeight = effectiveTransmission > 0.0 ? effectiveReflectivity + (1.0 - effectiveReflectivity) * fresnel : effectiveReflectivity;
        double refractionWeight = effectiveTransmission > 0.0 ? effectiveTransmission * (1.0 - fresnel) : 0.0;
        double secondarySum = reflectionWeight + refractionWeight;
        //prevent more light contribution than it recieved, it cannot create more light, so the sum should be the 100% of the incident light, not more
        if (secondarySum > 1.0) {
            reflectionWeight /= secondarySum;
            refractionWeight /= secondarySum;
            secondarySum = 1.0;
        }
        double baseWeight = Math.max(0.0, 1.0 - secondarySum);

        Color result = litColor.multiply(baseWeight);

        if (reflectionWeight > 0.0) {
            Vector3D reflectionDirection = reflect(i, orientedNormal).normalize();
            Point3D reflectionOrigin = closest.getPoint().add(orientedNormal.multiply(SECONDARY_RAY_EPSILON));
            Intersection reflectionHit = findClosestIntersection(new Ray(reflectionOrigin, reflectionDirection), SECONDARY_RAY_EPSILON, Double.POSITIVE_INFINITY);
            Color reflectionColor = traceRay(new Ray(reflectionOrigin, reflectionDirection), reflectionHit, depth + 1, currentIor);
            Color reflectionTint = mixColor(Color.WHITE, objectColor, effectiveMetalness);
            result = result.add(reflectionColor.multiply(reflectionTint).multiply(reflectionWeight));
        }

        if (refractionWeight > 0.0) {
            Vector3D refractionDirection = refract(i, orientedNormal, n1, n2);
            if (refractionDirection != null) {
                Point3D refractionOrigin = closest.getPoint().add(refractionDirection.multiply(SECONDARY_RAY_EPSILON));
                Intersection refractionHit = findClosestIntersection(new Ray(refractionOrigin, refractionDirection), SECONDARY_RAY_EPSILON, Double.POSITIVE_INFINITY);
                Color refractionColor = traceRay(new Ray(refractionOrigin, refractionDirection), refractionHit, depth + 1, n2);
                // Beer lmbert attenuation, when entering the medium, attenuate by traveled thickness before next hit, so thicker seems more tinted
                if (frontFace && refractionHit != null) {
                    Color absorption = material.computeAbsorptionTransmittance(refractionHit.getDistance());
                    refractionColor = refractionColor.multiply(absorption);
                }
                result = result.add(refractionColor.multiply(refractionWeight));
            } else {
                //total internal reflection fallback
                Vector3D reflectionDirection = reflect(i, orientedNormal).normalize();
                Point3D reflectionOrigin = closest.getPoint().add(orientedNormal.multiply(SECONDARY_RAY_EPSILON));
                Intersection reflectionHit = findClosestIntersection(new Ray(reflectionOrigin, reflectionDirection), SECONDARY_RAY_EPSILON, Double.POSITIVE_INFINITY);
                Color reflectionColor = traceRay(new Ray(reflectionOrigin, reflectionDirection), reflectionHit, depth + 1, currentIor);
                result = result.add(reflectionColor.multiply(refractionWeight));
            }
        }

        return result;
    }

    private Intersection findClosestIntersection(Ray ray, double minDistance, double maxDistance) {
        return scene.intersect(ray, minDistance, maxDistance);
    }

    private Vector3D reflect(Vector3D incident, Vector3D normal) {
        return incident.subtract(normal.multiply(2.0 * incident.dot(normal)));
    }

    private Vector3D refract(Vector3D incident, Vector3D normal, double n1, double n2) {
        double eta = n1 / n2;
        double cosI = -incident.dot(normal);
        double k = 1.0 - eta * eta * (1.0 - cosI * cosI);
        if (k < 0.0) {
            return null;
        }
        return incident.multiply(eta).add(normal.multiply(eta * cosI - Math.sqrt(k))).normalize();
    }

    private double schlickFresnel(double cosTheta, double n1, double n2) {
        double r0 = (n1 - n2) / (n1 + n2);
        r0 *= r0;
        double oneMinusCos = 1.0 - clamp01(cosTheta);
        return r0 + (1.0 - r0) * Math.pow(oneMinusCos, 5.0);
    }

    private Color mixColor(Color a, Color b, double t) {
        double w = clamp01(t);
        return a.multiply(1.0 - w).add(b.multiply(w));
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private Color traceShadowTransmittance(Point3D point, Vector3D geometricNormal, LightSample lightSample, double maxDistance) {
        // Offset along surface normal to reduce self shadow
        Vector3D n = geometricNormal == null ? lightSample.getDirectionToLight() : geometricNormal.normalize();
        if (n.dot(lightSample.getDirectionToLight()) < 0.0) {
            n = n.multiply(-1.0);
        }
        Vector3D direction = lightSample.getDirectionToLight();
        Point3D origin = point.add(n.multiply(SHADOW_EPSILON));
        Color transmittance = Color.WHITE;

        double remainingDistance = maxDistance;
        for (int step = 0; step < SHADOW_TRANSMITTANCE_MAX_STEPS; step++) {
            //blocker is a ray blocker, could be completelly opaque or see through, we need to check the material properties to know how much light it will block and how much it will let through
            Intersection blocker = scene.intersect(new Ray(origin, direction), SHADOW_EPSILON, remainingDistance);
            if (blocker == null) {
                return transmittance;
            }

            Material blockerMaterial = blocker.getObject().getMaterial();
            double transmission = clamp01(blockerMaterial.getTransmission() * (1.0 - blockerMaterial.getMetalness()));
            if (transmission <= 0.0) {
                return Color.BLACK;
            }

            Color tint = blockerMaterial.sampleAlbedo(blocker);
            transmittance = transmittance.multiply(tint).multiply(transmission);
            if (isNearlyBlack(transmittance)) {
                return Color.BLACK;
            }

            origin = blocker.getPoint().add(direction.multiply(SHADOW_EPSILON));
            if (!Double.isInfinite(remainingDistance)) {
                remainingDistance -= blocker.getDistance();
                if (remainingDistance <= SHADOW_EPSILON) {
                    return transmittance;
                }
            }
        }

        return transmittance;
    }

    //skip early if nearly black
    private boolean isNearlyBlack(Color color) {
        return color.getR() < MIN_SHADOW_TRANSMITTANCE && color.getG() < MIN_SHADOW_TRANSMITTANCE && color.getB() < MIN_SHADOW_TRANSMITTANCE;
    }
}
