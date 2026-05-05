package raytracer.geometry;

import raytracer.core.Intersection;
import raytracer.core.Ray;
import raytracer.utils.Color;
import raytracer.utils.Point3D;
import raytracer.utils.Vector3D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
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

    public Point3D getCentroidUniqueVertices() {
        List<Point3D> vertices = getUniqueVertexReferences();
        if (vertices.isEmpty()) {
            return new Point3D(0.0, 0.0, 0.0);
        }

        double sumX = 0.0;
        double sumY = 0.0;
        double sumZ = 0.0;

        for (Point3D vertex : vertices) {
            sumX += vertex.x;
            sumY += vertex.y;
            sumZ += vertex.z;
        }

        int count = vertices.size();
        return new Point3D(sumX / count, sumY / count, sumZ / count);
    }

    public void translate(Vector3D delta) {
        for (Point3D vertex : getUniqueVertexReferences()) {
            vertex.x += delta.x;
            vertex.y += delta.y;
            vertex.z += delta.z;
        }
    }

    public void scaleUniform(double factor, Point3D pivot) {
        if (factor <= 0.0) {
            throw new IllegalArgumentException("Scale factor must be greater than zero.");
        }

        for (Point3D vertex : getUniqueVertexReferences()) {
            vertex.x = pivot.x + (vertex.x - pivot.x) * factor;
            vertex.y = pivot.y + (vertex.y - pivot.y) * factor;
            vertex.z = pivot.z + (vertex.z - pivot.z) * factor;
        }
    }

    public void scaleUniformFromCentroid(double factor) {
        scaleUniform(factor, getCentroidUniqueVertices());
    }

    public void fitToMaxDimension(double targetSize) {
        if (targetSize <= 0.0) {
            throw new IllegalArgumentException("Target size must be greater than zero.");
        }

        List<Point3D> vertices = getUniqueVertexReferences();
        if (vertices.isEmpty()) {
            return;
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (Point3D vertex : vertices) {
            minX = Math.min(minX, vertex.x);
            minY = Math.min(minY, vertex.y);
            minZ = Math.min(minZ, vertex.z);
            maxX = Math.max(maxX, vertex.x);
            maxY = Math.max(maxY, vertex.y);
            maxZ = Math.max(maxZ, vertex.z);
        }

        double extentX = maxX - minX;
        double extentY = maxY - minY;
        double extentZ = maxZ - minZ;
        //get biggest side
        double maxExtent = Math.max(extentX, Math.max(extentY, extentZ));

        if (maxExtent <= 0.0) {
            return;
        }

        scaleUniformFromCentroid(targetSize / maxExtent);
    }

    private List<Point3D> getUniqueVertexReferences() {
        IdentityHashMap<Point3D, Boolean> seen = new IdentityHashMap<>();
        List<Point3D> unique = new ArrayList<>();

        for (Triangle triangle : getTriangles()) {
            addVertexReferenceIfNew(unique, seen, triangle.getV0());
            addVertexReferenceIfNew(unique, seen, triangle.getV1());
            addVertexReferenceIfNew(unique, seen, triangle.getV2());
        }

        return unique;
    }

    private void addVertexReferenceIfNew(List<Point3D> unique, IdentityHashMap<Point3D, Boolean> seen, Point3D vertex) {
        if (!seen.containsKey(vertex)) {
            seen.put(vertex, Boolean.TRUE);
            unique.add(vertex);
        }
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public Intersection intersect(Ray ray) {
        Intersection closest = null;

        for (Triangle triangle : getTriangles()) {
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
            closest.getNormal(),
            closest.getBaryU(),
            closest.getBaryV(),
            closest.getBaryW()
        );
    }
}
