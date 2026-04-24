package raytracer.utils;

public class Point3D extends Tuple3D {

    public Point3D(double x, double y, double z) {
        super(x, y, z);
    }

    //point + vector = point  (moving a point along a direction)
    public Point3D add(Vector3D v) {
        return new Point3D(this.x + v.x, this.y + v.y, this.z + v.z);
    }

    //point - point = vector  (displacement between two points)
    public Vector3D subtract(Point3D other) {
        return new Vector3D(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    // point - vector = Point  (moving a point in the opposite direction)
    public Point3D subtract(Vector3D v) {
        return new Point3D(this.x - v.x, this.y - v.y, this.z - v.z);
    }
}

