package raytracer.geometry;

public class Sphere implements Object3D {
    private double radius;

    public Sphere(double radius) {
        this.radius = radius;
    }

    public double getRadius() {
        return radius;
    }
}
