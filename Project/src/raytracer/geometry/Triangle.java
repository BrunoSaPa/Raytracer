package raytracer.geometry;

import raytracer.core.Intersection;
import raytracer.core.Ray;
import raytracer.utils.Color;
import raytracer.utils.Point3D;
import raytracer.utils.Vector3D;

public class Triangle implements Object3D {
    private static final double EPSILON = 1e-8;

    private Point3D v0;
    private Point3D v1;
    private Point3D v2;
    private Color color;
    private TriangleCullingMode cullingMode;

    public Triangle(Point3D v0, Point3D v1, Point3D v2, Color color) {
        this(v0, v1, v2, color, TriangleCullingMode.NONE);
    }

    public Triangle(Point3D v0, Point3D v1, Point3D v2, Color color, TriangleCullingMode cullingMode) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.color = color;
        this.cullingMode = cullingMode;
    }

    public Point3D getV0() { return v0; }
    public Point3D getV1() { return v1; }
    public Point3D getV2() { return v2; }
    public TriangleCullingMode getCullingMode() { return cullingMode; }

    public void setCullingMode(TriangleCullingMode cullingMode) {
        this.cullingMode = cullingMode;
    }

    @Override
    public Color getColor() {
        return color;
    }

    public Vector3D getNormal() {
        Vector3D edge1 = v1.subtract(v0);
        Vector3D edge2 = v2.subtract(v0);
        return edge1.cross(edge2).normalize();
    }

    @Override
    public Intersection intersect(Ray ray) {
        Vector3D edge1 = v1.subtract(v0);
        Vector3D edge2 = v2.subtract(v0);

        Vector3D pvec = ray.getDirection().cross(edge2);
        double det = edge1.dot(pvec);

        // det sign tells if ray hits front or back side relative to triangle normal/winding
        if (cullingMode == TriangleCullingMode.BACK_FACE) {
            if (det <= EPSILON) return null;
        } else if (cullingMode == TriangleCullingMode.FRONT_FACE) {
            if (det >= -EPSILON) return null;
        } else {
            if (Math.abs(det) <= EPSILON) return null;
        }

        double invDet = 1.0 / det;

        Vector3D tvec = ray.getOrigin().subtract(v0);
        double u = tvec.dot(pvec) * invDet;
        if (u < 0.0 || u > 1.0) return null;

        Vector3D qvec = tvec.cross(edge1);
        double v = ray.getDirection().dot(qvec) * invDet;
        if (v < 0.0 || (u + v) > 1.0) return null;

        double t = edge2.dot(qvec) * invDet;
        if (t <= EPSILON) return null;

        double w = 1.0 - u - v;
        Point3D hitPoint = ray.getPoint(t);

        //just saving barycentric coords just so they can be used in the future
        return new Intersection(t, hitPoint, this, u, v, w);
    }
}
