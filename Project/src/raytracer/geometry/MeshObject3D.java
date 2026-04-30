package raytracer.geometry;

import raytracer.core.Intersection;
import raytracer.core.Ray;
import raytracer.utils.Color;
import raytracer.utils.Point3D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MeshObject3D implements Object3D {
    private final List<Triangle> triangles;
    private final Color color;
    private final TriangleCullingMode cullingMode;

    public MeshObject3D(Color color, TriangleCullingMode cullingMode) {
        this.triangles = new ArrayList<>();
        this.color = color;
        this.cullingMode = cullingMode;
    }

    public void addTriangle(Point3D v0, Point3D v1, Point3D v2) {
        triangles.add(new Triangle(v0, v1, v2, color, cullingMode));
    }

    public int getTriangleCount() {
        return triangles.size();
    }

    public List<Triangle> getTriangles() {
        return Collections.unmodifiableList(triangles);
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public Intersection intersect(Ray ray) {
        Intersection closest = null;

        for (Triangle triangle : triangles) {
            Intersection hit = triangle.intersect(ray);
            if (hit != null && (closest == null || hit.getDistance() < closest.getDistance())) {
                closest = hit;
            }
        }

        if (closest == null) {
            return null;
        }

        return new Intersection(
            closest.getDistance(),
            closest.getPoint(),
            this,
            closest.getBaryU(),
            closest.getBaryV(),
            closest.getBaryW()
        );
    }
}
