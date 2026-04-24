package raytracer.core;

import raytracer.utils.Point3D;
import raytracer.utils.Vector3D;

public class Ray {
    private Point3D origin;
    private Vector3D direction;//must be normalized

    public Ray(Point3D origin, Vector3D direction) {
        this.origin = origin;
        this.direction = direction.normalize();
    }

    public Point3D getOrigin() { return origin; }
    public Vector3D getDirection() { return direction; }

    //origin + direction * t = a Point on the ray
    public Point3D getPoint(double t) {
        return origin.add(direction.multiply(t));
    }
}
