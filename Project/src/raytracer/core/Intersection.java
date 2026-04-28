package raytracer.core;

import raytracer.geometry.Object3D;
import raytracer.utils.Point3D;

public class Intersection {
    private double distance;
    private Point3D point;   // a hit location in 3D space
    private Object3D object;
    private double baryU;
    private double baryV;
    private double baryW;

    public Intersection(double distance, Point3D point, Object3D object) {
        this(distance, point, object, Double.NaN, Double.NaN, Double.NaN);
    }

    public Intersection(double distance, Point3D point, Object3D object, double baryU, double baryV, double baryW) {
        this.distance = distance;
        this.point = point;
        this.object = object;
        this.baryU = baryU;
        this.baryV = baryV;
        this.baryW = baryW;
    }

    public double getDistance() { return distance; }
    public Point3D getPoint() { return point; }
    public Object3D getObject() { return object; }
    public double getBaryU() { return baryU; }
    public double getBaryV() { return baryV; }
    public double getBaryW() { return baryW; }

    public boolean hasBarycentric() {
        return !(Double.isNaN(baryU) || Double.isNaN(baryV) || Double.isNaN(baryW));
    }
}
