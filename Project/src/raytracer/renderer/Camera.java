package raytracer.renderer;

import raytracer.core.Ray;
import raytracer.math.Vector3D;

public class Camera {
    private Vector3D position;
    private Vector3D forward;
    private Vector3D up;
    private Vector3D right;
    private double fov; // in degrees
    private double nearPlane;
    private double farPlane;
    private int imageWidth;
    private int imageHeight;

    public Camera(Vector3D position, Vector3D lookAt, Vector3D worldUp, double fov, double nearPlane, double farPlane, int imageWidth, int imageHeight) {
        this.position = position;
        this.fov = fov;
        this.nearPlane = nearPlane;
        this.farPlane = farPlane;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;

        //build camera coordinate system
        this.forward = lookAt.subtract(position).normalize();
        //importatt here, i used cross product to calculate the orthogonal (which is a vector that is perpendicular to both directions given)
        this.right = forward.cross(worldUp).normalize();
        this.up = right.cross(forward).normalize();
    }

    public Ray generateRay(int pixelX, int pixelY) {
        double aspectRatio = (double) imageWidth / imageHeight;
        double scale = Math.tan(Math.toRadians(fov / 2.0));

        //map pixel to normalized coordinates [-1, 1]
        double cX = (2.0 * (pixelX + 0.5) / imageWidth - 1.0) * aspectRatio * scale;
        double cY = (1.0 - 2.0 * (pixelY + 0.5) / imageHeight) * scale;

        Vector3D direction = forward.add(right.multiply(cX)).add(up.multiply(cY)).normalize();
        return new Ray(position, direction);
    }

    public double getNearPlane() { return nearPlane; }
    public double getFarPlane() { return farPlane; }
    public Vector3D getPosition() { return position; }
}
