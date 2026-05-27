package raytracer.geometry;

import raytracer.utils.Point3D;
import raytracer.utils.Vector3D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

public class MeshGroup3D {
    private final List<MeshObject3D> meshes;

    public MeshGroup3D(List<MeshObject3D> meshes) {
        this.meshes = meshes == null ? new ArrayList<>() : new ArrayList<>(meshes);
    }


    public Point3D getCentroid() {
        List<Point3D> vertices = collectUniqueVertices();
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
        double inv = 1.0 / vertices.size();
        return new Point3D(sumX * inv, sumY * inv, sumZ * inv);
    }

    public void fitToMaxDimension(double targetSize) {
        if (targetSize <= 0.0) {
            return;
        }

        List<Point3D> vertices = collectUniqueVertices();
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
        double maxExtent = Math.max(extentX, Math.max(extentY, extentZ));
        if (maxExtent <= 0.0) {
            return;
        }

        scaleUniform(targetSize / maxExtent, getCentroid());
    }

    public void scaleUniformFromCentroid(double factor) {
        scaleUniform(factor, getCentroid());
    }

    public void scaleUniform(double factor, Point3D pivot) {
        for (MeshObject3D mesh : meshes) {
            mesh.scaleUniform(factor, pivot);
        }
    }

    public void rotateX(double angleDegrees, Point3D pivot) {
        for (MeshObject3D mesh : meshes) {
            mesh.rotateX(angleDegrees, pivot);
        }
    }

    public void rotateY(double angleDegrees, Point3D pivot) {
        for (MeshObject3D mesh : meshes) {
            mesh.rotateY(angleDegrees, pivot);
        }
    }

    public void rotateZ(double angleDegrees, Point3D pivot) {
        for (MeshObject3D mesh : meshes) {
            mesh.rotateZ(angleDegrees, pivot);
        }
    }

    public void translate(Vector3D delta) {
        for (MeshObject3D mesh : meshes) {
            mesh.translate(delta);
        }
    }

    private List<Point3D> collectUniqueVertices() {
        IdentityHashMap<Point3D, Boolean> seen = new IdentityHashMap<>();
        List<Point3D> vertices = new ArrayList<>();
        for (MeshObject3D mesh : meshes) {
            for (Triangle triangle : mesh.getTriangles()) {
                addVertexIfNew(vertices, seen, triangle.getV0());
                addVertexIfNew(vertices, seen, triangle.getV1());
                addVertexIfNew(vertices, seen, triangle.getV2());
            }
        }
        return vertices;
    }

    private void addVertexIfNew(List<Point3D> vertices, IdentityHashMap<Point3D, Boolean> seen, Point3D vertex) {
        if (!seen.containsKey(vertex)) {
            seen.put(vertex, Boolean.TRUE);
            vertices.add(vertex);
        }
    }
}

