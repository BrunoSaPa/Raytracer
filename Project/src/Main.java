import raytracer.core.Scene;
import raytracer.geometry.Sphere;
import raytracer.math.Vector3D;
import raytracer.renderer.Camera;
import raytracer.renderer.Raytracer;

import raytracer.math.Color;

public class Main {
    public static void main(String[] args) {
        //image
        int width = 1080;
        int height = 1080;

        //camera
        Vector3D cameraPosition = new Vector3D(0, 0, 0);
        Vector3D lookAt = new Vector3D(0, 0, -1);// looking down to z with the z:-1
        Vector3D worldUp = new Vector3D(0, 1, 0);
        double fov = 60;//fov in deg
        double nearPlane = 0.1;
        double farPlane = 1000.0;

        Camera camera = new Camera(cameraPosition, lookAt, worldUp, fov, nearPlane, farPlane, width, height);

        //scene setup
        Scene scene = new Scene();

        //spheres
        Sphere sphere = new Sphere(new Vector3D(1.5, 1, -5), 0.1, Color.RED);
        Sphere sphere2 = new Sphere(new Vector3D(0, 1, -5), 0.25, Color.BLUE);
        scene.addObject(sphere);
        scene.addObject(sphere2);

        //render
        Color backgroundColor = Color.BLACK;
        Raytracer raytracer = new Raytracer(scene, camera, width, height, backgroundColor);
        raytracer.render("output/render.png");
    }
}
