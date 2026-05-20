package raytracer.accel;

import raytracer.geometry.Triangle;
import raytracer.utils.Point3D;

import java.util.List;

public final class BoundsOps {
    private BoundsOps() {
    }

    public static AABB fromPoints(List<Point3D> points) {
        AABB bounds = new AABB();
        for (Point3D point : points) {
            bounds.include(point);
        }
        return bounds;
    }

    //could build form triangles, but it just uses the points like above
    public static AABB fromTriangle(Triangle triangle) {
        AABB bounds = new AABB();
        bounds.include(triangle.getV0());
        bounds.include(triangle.getV1());
        bounds.include(triangle.getV2());
        return bounds;
    }
}

