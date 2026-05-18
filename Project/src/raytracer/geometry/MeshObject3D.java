package raytracer.geometry;

import raytracer.core.Intersection;
import raytracer.core.Ray;
import raytracer.utils.Color;
import raytracer.utils.Point3D;
import raytracer.utils.Vector3D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class MeshObject3D implements Object3D {
    private final List<Triangle> triangles;
    private final Color color;
    private final TriangleCullingMode cullingMode;
    private final Map<Integer, IdentityHashMap<Point3D, Vector3D>> smoothedVertexNormalsByGroup;
    private boolean smoothingNormalsDirty;
    private double specularStrength;
    private double shininess;
    private Color specularColor;

    public MeshObject3D(Color color, TriangleCullingMode cullingMode) {
        this(color, cullingMode, 0.0, 32.0, Color.WHITE);
    }

    public MeshObject3D(Color color, TriangleCullingMode cullingMode, double shininess) {
        this(color, cullingMode, 0.0, shininess, Color.WHITE);
    }

    public MeshObject3D(
        Color color,
        TriangleCullingMode cullingMode,
        double specularStrength,
        double shininess,
        Color specularColor
    ) {
        this.triangles = new ArrayList<>();
        this.color = color;
        this.cullingMode = cullingMode;
        this.smoothedVertexNormalsByGroup = new HashMap<>();
        this.smoothingNormalsDirty = true;
        this.specularStrength = Math.max(0.0, specularStrength);
        this.shininess = Math.max(1.0, shininess);
        this.specularColor = specularColor == null ? Color.WHITE : specularColor;
    }

    public void addTriangle(
        Point3D v0,
        Point3D v1,
        Point3D v2,
        int smoothingGroupId
    ) {
        addTriangle(v0, v1, v2, smoothingGroupId, null, null, null);
    }

    public void addTriangle(
        Point3D v0,
        Point3D v1,
        Point3D v2,
        int smoothingGroupId,
        Vector3D normal0,
        Vector3D normal1,
        Vector3D normal2
    ) {
        triangles.add(new Triangle(v0, v1, v2, color, cullingMode, smoothingGroupId, normal0, normal1, normal2));
        smoothingNormalsDirty = true;
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
        smoothingNormalsDirty = true;
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
        smoothingNormalsDirty = true;
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
    public double getSpecularStrength() {
        return specularStrength;
    }

    @Override
    public double getShininess() {
        return shininess;
    }

    @Override
    public Color getSpecularColor() {
        return specularColor;
    }

    public void setSpecularStrength(double specularStrength) {
        this.specularStrength = Math.max(0.0, specularStrength);
    }

    public void setShininess(double shininess) {
        this.shininess = Math.max(1.0, shininess);
    }

    public void setSpecularColor(Color specularColor) {
        this.specularColor = specularColor == null ? Color.WHITE : specularColor;
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

        Vector3D shadingNormal = resolveShadingNormal(closest);

        return new Intersection(
            closest.getDistance(),
            closest.getPoint(),
            this,
            shadingNormal,
            closest.getBaryU(),
            closest.getBaryV(),
            closest.getBaryW()
        );
    }

    private Vector3D resolveShadingNormal(Intersection hit) {

        if (hit == null) {
            return null;
        }
        //i am asumming that hit comes from a triangle, might need to change if i can do hits on quads or any other primitive
        Triangle triangle = (Triangle) hit.getObject();

        //if vertex normals are provided i will use those instead of calculating them if not provided.
        if (triangle.hasProvidedVertexNormals() && hit.hasBarycentric()) {
            double u = hit.getBaryU();
            double v = hit.getBaryV();
            double w = hit.getBaryW();

            Vector3D interpolated = triangle.getNormal0().multiply(w).add(triangle.getNormal1().multiply(u)).add(triangle.getNormal2().multiply(v));
            return interpolated.normalize();
        }

        if (triangle.getSmoothingGroupId() <= 0 || !hit.hasBarycentric()) {
            return triangle.getNormal();
        }

        rebuildSmoothedNormalsIfNeeded();

        int groupId = triangle.getSmoothingGroupId();
        Vector3D n0 = getSmoothedNormal(triangle.getV0(), groupId, triangle.getNormal());
        Vector3D n1 = getSmoothedNormal(triangle.getV1(), groupId, triangle.getNormal());
        Vector3D n2 = getSmoothedNormal(triangle.getV2(), groupId, triangle.getNormal());

        double u = hit.getBaryU();
        double v = hit.getBaryV();
        double w = hit.getBaryW();

        //Moller-Trumbore barycentrics map as v0=w, v1=u, v2=v
        Vector3D interpolated = n0.multiply(w).add(n1.multiply(u)).add(n2.multiply(v));
        return interpolated.normalize();
    }

    private Vector3D getSmoothedNormal(Point3D vertex, int groupId, Vector3D fallback) {
        IdentityHashMap<Point3D, Vector3D> groupNormals = smoothedVertexNormalsByGroup.get(groupId);
        Vector3D normal = groupNormals == null ? null : groupNormals.get(vertex);
        return normal != null ? normal : fallback;
    }

    private void rebuildSmoothedNormalsIfNeeded() {
        if (!smoothingNormalsDirty) {
            return;
        }

        smoothedVertexNormalsByGroup.clear();

        for (Triangle triangle : getTriangles()) {
            if (triangle.getSmoothingGroupId() <= 0) {
                continue;
            }

            Vector3D faceNormal = triangle.getNormal();
            accumulateSmoothedNormal(triangle.getV0(), triangle.getSmoothingGroupId(), faceNormal);
            accumulateSmoothedNormal(triangle.getV1(), triangle.getSmoothingGroupId(), faceNormal);
            accumulateSmoothedNormal(triangle.getV2(), triangle.getSmoothingGroupId(), faceNormal);
        }

        for (IdentityHashMap<Point3D, Vector3D> groupNormals : smoothedVertexNormalsByGroup.values()) {
            for (Map.Entry<Point3D, Vector3D> entry : groupNormals.entrySet()) {
                entry.setValue(entry.getValue().normalize());
            }
        }

        smoothingNormalsDirty = false;
    }

    private void accumulateSmoothedNormal(Point3D vertex, int groupId, Vector3D faceNormal) {
        if (vertex == null || groupId <= 0) {
            return;
        }

        //if first time for that group create new map
        IdentityHashMap<Point3D, Vector3D> groupNormals = smoothedVertexNormalsByGroup.computeIfAbsent(
            groupId,
            ignored -> new IdentityHashMap<>()
        );

        Vector3D current = groupNormals.get(vertex);
        if (current == null) {
            //first triangle for this vertex in this group, start with face normal
            groupNormals.put(vertex, faceNormal);
        } else {
            //vertex shared, so adding the face of the normal to existing ones
            groupNormals.put(vertex, current.add(faceNormal));
        }
    }
}
