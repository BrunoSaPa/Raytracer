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
import javax.imageio.ImageIO;

public class Raytracer {
    private Scene scene;
    private Camera camera;
    private int width;
    private int height;
    private Color backgroundColor;

    public Raytracer(Scene scene, Camera camera, int width, int height, Color backgroundColor) {
        this.scene = scene;
        this.camera = camera;
        this.width = width;
        this.height = height;
        this.backgroundColor = backgroundColor;
    }

    public void render(String outputPath) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Ray ray = camera.generateRay(x, y);
                Color pixelColor = traceRay(ray);
                image.setRGB(x, y, pixelColor.toRGB());
            }
        }

        try {
            ImageIO.write(image, "png", new File(outputPath));
            System.out.println("Image saved to: " + outputPath);
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
        Color litColor = Color.BLACK; //(0,0,0 so well just add whatever color it hits for contributions)

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
        //create a ray from the surface point towards the light
        Ray shadowRay = new Ray(point, lightSample.getDirectionToLight());

        //use a small epsilon to avoid self-intersection
        double epsilon = 1e-4;

        //check if any object intersects the shadow ray before reaching the light
        for (Object3D obj : scene.getObjects()) {
            Intersection hit = obj.intersect(shadowRay);
            if (hit != null && hit.getDistance() > epsilon && hit.getDistance() < maxDistance) {
                return true; //point is in shadow
            }
        }

        return false; //point is not in shadow
    }
}
