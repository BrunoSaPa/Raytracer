package raytracer.core;

import raytracer.geometry.Object3D;
import raytracer.utils.Point3D;

public class Instersection {
    private double distance;
    private Point3D point;   // a hit location in 3D space
    private Object3D object;

    public Instersection(double distance, Point3D point, Object3D object) {
        this.distance = distance;
        this.point = point;
        this.object = object;
    }

    public double getDistance() { return distance; }
    public Point3D getPoint() { return point; }
    public Object3D getObject() { return object; }
}
