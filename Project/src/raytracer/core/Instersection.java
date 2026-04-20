package raytracer.core;

import raytracer.geometry.Object3D;
import raytracer.math.Vector3D;

public class Instersection {
    private double distance;
    private Vector3D point;
    private Object3D object;

    public Instersection(double distance, Vector3D point, Object3D object) {
        this.distance = distance;
        this.point = point;
        this.object = object;
    }

    public double getDistance() { return distance; }
    public Vector3D getPoint() { return point; }
    public Object3D getObject() { return object; }
}
