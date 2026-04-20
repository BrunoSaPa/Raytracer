package raytracer.core;

import raytracer.math.Vector3D;

public class Ray {
    private Vector3D origin;
    private Vector3D direction; //must be normalized

    public Ray(Vector3D origin, Vector3D direction) {
        this.origin = origin;
        this.direction = direction.normalize();
    }

    public Vector3D getOrigin() { return origin; }
    public Vector3D getDirection() { return direction; }

    public Vector3D getPoint(double t) {
        return origin.add(direction.multiply(t));
    }
}
