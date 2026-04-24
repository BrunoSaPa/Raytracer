package raytracer.renderer;

import raytracer.core.Instersection;
import raytracer.core.Ray;
import raytracer.core.Scene;
import raytracer.geometry.Object3D;

import raytracer.utils.Color;
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
        Instersection closest = null;

        for (Object3D obj : scene.getObjects()) {
            Instersection hit = obj.intersect(ray);
            if (hit != null) {
                double t = hit.getDistance();
                // Respect near and far planes
                if (t >= camera.getNearPlane() && t <= camera.getFarPlane()) {
                    if (closest == null || t < closest.getDistance()) {
                        closest = hit;
                    }
                }
            }
        }

        if (closest != null) {
            return closest.getObject().getColor();
        }
        return backgroundColor;
    }
}
