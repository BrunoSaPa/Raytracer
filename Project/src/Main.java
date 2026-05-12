import raytracer.core.Scene;
import raytracer.geometry.MeshObject3D;
import raytracer.geometry.Sphere;
import raytracer.geometry.Triangle;
import raytracer.geometry.TriangleCullingMode;
import raytracer.io.ObjReader;
import raytracer.lighting.DirectionalLight;
import raytracer.lighting.PointLight;
import raytracer.lighting.SpotLight;
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
        Point3D cameraPosition = new Point3D(0, 0, .5);
        Point3D lookAt = new Point3D(0, 0, -1); //target location
        Vector3D worldUp = new Vector3D(0, 1, 0);//direction
        double fov = 60;//fov in deg
        double nearPlane = 0.01;
        double farPlane = 1000.0;

        Camera camera = new Camera(cameraPosition, lookAt, worldUp, fov, nearPlane, farPlane, width, height);

        //scene setup
        Scene scene = new Scene();

        //basic lights for diffuse flat shading
        //scene.addLight(new DirectionalLight(new Vector3D(0, -1, 0), Color.WHITE, 0.6));
        scene.addLight(new PointLight(new Point3D(1, 0, 0), Color.BLUE, 5));
        scene.addLight(new PointLight(new Point3D(-1, 0, 0), Color.RED, 5));
        scene.addLight(new SpotLight(new Point3D(0, 0, 0), new Vector3D(0, 0, -1), 5, Color.WHITE, 10));

        if (args.length > 0) {
            try {
                MeshObject3D mesh = ObjReader.loadAsMesh(args[0], Color.RED, TriangleCullingMode.BACK_FACE);

                //max side size to 1.5, center around origin, then place the mesh in front of the camera.
                mesh.fitToMaxDimension(2.5);
                Point3D centroid = mesh.getCentroidUniqueVertices();
                mesh.translate(new Vector3D(-centroid.x, -centroid.y, -centroid.z - 4.0));


                //add floor plane
                MeshObject3D plane = ObjReader.loadAsMesh(args[1], Color.WHITE, TriangleCullingMode.BACK_FACE);
                plane.scaleUniformFromCentroid(10);



                //scene.addObject(mesh);
                //System.out.println("Loaded OBJ as one Object3D mesh with triangles: " + mesh.getTriangleCount());
                scene.addObject(plane);
                System.out.println("Loaded OBJ as one Object3D mesh with triangles: " + plane.getTriangleCount());

                //Sphere sphere = new Sphere(new Point3D(1, 1, -20), 40, Color.RED);
                //scene.addObject(sphere);


            } catch (Exception e) {
                System.err.println("Failed to load OBJ: " + e.getMessage());
                return;
            }
        } else {

            //if no arguments received, i just render what i had before (arguments meaning obj files)

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
        }

        //render
        Color backgroundColor = Color.BLACK;
        Raytracer raytracer = new Raytracer(scene, camera, width, height, backgroundColor);
        raytracer.render("output/render.png");
    }
}
