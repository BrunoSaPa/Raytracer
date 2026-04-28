import raytracer.core.Scene;
import raytracer.geometry.Sphere;
import raytracer.geometry.Triangle;
import raytracer.geometry.TriangleCullingMode;
import raytracer.utils.Point3D;
import raytracer.utils.Vector3D;
import raytracer.renderer.Camera;
import raytracer.renderer.Raytracer;

import raytracer.utils.Color;

public class Main {
    public static void main(String[] args) {
        //image
        int width = 1080;
        int height = 1080;

        //camera
        Point3D cameraPosition = new Point3D(0, 0, 0);
        Point3D lookAt = new Point3D(0, 0, -1); //target location
        Vector3D worldUp = new Vector3D(0, 1, 0);//direction
        double fov = 60;//fov in deg
        double nearPlane = 0.1;
        double farPlane = 1000.0;

        Camera camera = new Camera(cameraPosition, lookAt, worldUp, fov, nearPlane, farPlane, width, height);

        //scene setup
        Scene scene = new Scene();

        //sphere centers are points in 3D space
        Sphere sphere = new Sphere(new Point3D(1.5, 1, -5), 0.1, Color.RED);
        Sphere sphere2 = new Sphere(new Point3D(0, 1, -5), 0.25, Color.BLUE);

        //triangles (right hand)
        Triangle triangleBackCull = new Triangle(
            new Point3D(-1.0, -0.5, -4.5),
            new Point3D(0.0, 1.0, -4.5),
            new Point3D(1.0, -0.5, -4.5),
            Color.RED,
            TriangleCullingMode.BACK_FACE
        );

        Triangle triangleNoCull = new Triangle(
            new Point3D(-1.5, -0.8, -6.0),
            new Point3D(-0.4, 0.8, -6.0),
            new Point3D(0.7, -0.8, -6.0),
            Color.BLUE
        );

        scene.addObject(sphere);
        scene.addObject(sphere2);
        scene.addObject(triangleBackCull);
        scene.addObject(triangleNoCull);

        //render
        Color backgroundColor = Color.BLACK;
        Raytracer raytracer = new Raytracer(scene, camera, width, height, backgroundColor);
        raytracer.render("output/render.png");
    }
}
