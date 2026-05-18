package raytracer.renderer;

import raytracer.core.Intersection;
import raytracer.core.Ray;
import raytracer.core.Scene;
import raytracer.geometry.Object3D;
import raytracer.lighting.Light;
import raytracer.lighting.LightSample;

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
    static final double SHADOW_EPSILON = 1e-4;
    static final float ambient =  0.05f;

    public Raytracer(Scene scene, Camera camera, int width, int height, Color backgroundColor) {
        this(scene, camera, width, height, backgroundColor, Math.max(1, Runtime.getRuntime().availableProcessors()), 32);
    }

    public Raytracer(Scene scene, Camera camera, int width, int height, Color backgroundColor, int threadCount, int tileSize) {
        this.scene = scene;
        this.camera = camera;
        this.width = width;
        this.height = height;
        this.backgroundColor = backgroundColor;
        this.threadCount = Math.max(1, threadCount);
        this.tileSize = Math.max(8, tileSize);
    }

    public void render(String outputPath) {
        long renderStartNs = System.nanoTime();
        System.out.println("Rendering with " + threadCount + " threads (tile size: " + tileSize + ")");

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
                            Color pixelColor = traceRay(ray);
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

    private Color traceRay(Ray ray) {
        Intersection closest = findClosestIntersection(ray, camera.getNearPlane(), camera.getFarPlane());
        if (closest == null) {
            return backgroundColor;
        }

        //if no lights are set, i just use flat color per obj
        if (scene.getLights().isEmpty()) {
            return closest.getObject().getColor();
        }

        //normal should already be normalized
        Vector3D normal = closest.getNormal();
        if (normal == null) {
            return closest.getObject().getColor();
        }


        Color objectColor = closest.getObject().getColor();
        Color litColor = objectColor.multiply(ambient); //ambient term

        for (Light light : scene.getLights()) {
            LightSample sample = light.sampleAt(closest.getPoint());
            //light too far away, no contribution or too close to surface no contribution
            if (sample.getMaxDistance() <= 0.0) {
                continue;
            }

            double nDotL = normal.dot(sample.getDirectionToLight());
            //light behind normal, no contribution
            if (nDotL <= 0.0) {
                continue;
            }

            //check if the point is in shadow
            if (isInShadow(closest.getPoint(), sample, sample.getMaxDistance())) {
                continue;
            }

            double diffuseScale = sample.getRadianceScale() * nDotL;
            Color contribution = sample.getColor().multiply(objectColor).multiply(diffuseScale);
            litColor = litColor.add(contribution);
        }

        return litColor;
    }

    private Intersection findClosestIntersection(Ray ray, double minDistance, double maxDistance) {
        Intersection closest = null;

        for (Object3D obj : scene.getObjects()) {
            Intersection hit = obj.intersect(ray);
            if (hit == null) {
                continue;
            }

            double t = hit.getDistance();
            if (t >= minDistance && t <= maxDistance && (closest == null || t < closest.getDistance())) {
                closest = hit;
            }
        }

        return closest;
    }

    private boolean isInShadow(Point3D point, LightSample lightSample, double maxDistance) {
        //offset the origin slightly along the light direction to reduce self shadow
        Point3D origin = point.add(lightSample.getDirectionToLight().multiply(SHADOW_EPSILON));
        Ray shadowRay = new Ray(origin, lightSample.getDirectionToLight());

        //check if any object intersects the shadow ray before reaching the light
        for (Object3D obj : scene.getObjects()) {
            Intersection hit = obj.intersect(shadowRay);
            if (hit != null && hit.getDistance() > SHADOW_EPSILON && hit.getDistance() < maxDistance) {
                return true; //point is in shadow
            }
        }

        return false; //point is not in shadow
    }
}
