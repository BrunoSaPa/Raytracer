package raytracer.geometry;

import raytracer.core.Instersection;
import raytracer.core.Ray;
import raytracer.utils.Point3D;
import raytracer.utils.Vector3D;
import raytracer.utils.Color;

public class Sphere implements Object3D {
    private Point3D center;
    private double radius;
    private Color color;

    public Sphere(Point3D center, double radius, Color color) {
        this.center = center;
        this.radius = radius;
        this.color = color;
    }

    public double getRadius() { return radius; }
    public Point3D getCenter() { return center; }

    @Override
    public Color getColor() { return color; }

    @Override
    public Instersection intersect(Ray ray) {
        //For this solution i implemented the geometric solution seen in class
        Point3D O = ray.getOrigin();
        Vector3D D = ray.getDirection();

        //1.vector from ray origin to sphere center
        Vector3D L = center.subtract(O);

        //2.projection of L onto D
        double tca = L.dot(D);

        //case: sphere is behind ray origin
        if (tca < 0) return null;

        // 3. squared perpendicular distance from center to ray
        double d2 = L.dot(L) - (tca * tca);

        double r2 = radius * radius;

        //case: ray misses the sphere
        if (d2 > r2) return null;

        // distance from perpendicular point to sphere surface
        double thc = Math.sqrt(r2 - d2);

        //intersection distances
        double t0 = tca - thc;
        double t1 = tca + thc;

        //pick the closest positive t (front point)
        double t;
        if (t0 > 0) {
            t = t0;
        } else if (t1 > 0) {
            t = t1;
        } else {
            return null; //both behind camera
        }

        Point3D hitPoint = ray.getPoint(t);
        return new Instersection(t, hitPoint, this);
    }
}
