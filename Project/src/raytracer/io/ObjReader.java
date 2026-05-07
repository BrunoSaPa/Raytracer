package raytracer.io;

import raytracer.core.Scene;
import raytracer.geometry.MeshObject3D;
import raytracer.geometry.TriangleCullingMode;
import raytracer.utils.Color;
import raytracer.utils.Point3D;
import raytracer.utils.Vector3D;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ObjReader {

    private ObjReader() {
    }

    public static int loadIntoScene(String objPath, Scene scene) throws IOException {
        MeshObject3D mesh = loadAsMesh(objPath, Color.WHITE, TriangleCullingMode.BACK_FACE);
        scene.addObject(mesh);
        return mesh.getTriangleCount();
    }

    public static MeshObject3D loadAsMesh(String objPath, Color color, TriangleCullingMode cullingMode) throws IOException {
        List<Point3D> vertices = new ArrayList<>();
        List<double[]> textureCoords = new ArrayList<>();
        List<Vector3D> normals = new ArrayList<>();
        MeshObject3D mesh = new MeshObject3D(color, cullingMode);
        int currentSmoothingGroupId = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(objPath))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();

                //skip # because some obj files generally contain this indicating meta data about the file itself, not necesarry for mesh construction
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                if (trimmed.startsWith("v ")) {
                    parseVertex(trimmed, vertices, lineNumber);
                } else if (trimmed.startsWith("vt ")) {
                    parseTextureCoord(trimmed, textureCoords, lineNumber);
                } else if (trimmed.startsWith("vn ")) {
                    parseNormal(trimmed, normals, lineNumber);
                } else if (trimmed.startsWith("s ")) {
                    currentSmoothingGroupId = parseSmoothingGroup(trimmed, lineNumber);
                } else if (trimmed.startsWith("f ")) {
                    parseFaceAndAddTriangles(trimmed, vertices, textureCoords, normals, mesh, currentSmoothingGroupId, lineNumber);
                }
            }
        }

        return mesh;
    }

    private static void parseVertex(String line, List<Point3D> vertices, int lineNumber) {
        String[] parts = line.split("\\s+");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid vertex at line " + lineNumber + ": " + line);
        }

        double x = parseDouble(parts[1], "vertex x", lineNumber);
        double y = parseDouble(parts[2], "vertex y", lineNumber);
        double z = parseDouble(parts[3], "vertex z", lineNumber);
        vertices.add(new Point3D(x, y, z));
    }

    private static void parseTextureCoord(String line, List<double[]> textureCoords, int lineNumber) {
        String[] parts = line.split("\\s+");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid texture coordinate at line " + lineNumber + ": " + line);
        }

        double u = parseDouble(parts[1], "texture u", lineNumber);
        double v = parseDouble(parts[2], "texture v", lineNumber);
        double w = parts.length >= 4 ? parseDouble(parts[3], "texture w", lineNumber) : 0.0;
        textureCoords.add(new double[]{u, v, w});
    }

    private static void parseNormal(String line, List<Vector3D> normals, int lineNumber) {
        String[] parts = line.split("\\s+");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid normal at line " + lineNumber + ": " + line);
        }

        double x = parseDouble(parts[1], "normal x", lineNumber);
        double y = parseDouble(parts[2], "normal y", lineNumber);
        double z = parseDouble(parts[3], "normal z", lineNumber);
        normals.add(new Vector3D(x, y, z));
    }

    private static void parseFaceAndAddTriangles(
        String line,
        List<Point3D> vertices,
        List<double[]> textureCoords,
        List<Vector3D> normals,
        MeshObject3D mesh,
        int smoothingGroupId,
        int lineNumber
    ) {
        String[] parts = line.split("\\s+");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Face needs at least 3 vertices at line " + lineNumber + ": " + line);
        }

        int[] positionIndices = new int[parts.length - 1];
        int[] normalIndices = new int[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            int[] parsed = parseFaceVertexIndices(parts[i], vertices.size(), textureCoords.size(), normals.size(), lineNumber);
            positionIndices[i - 1] = parsed[0];
            normalIndices[i - 1] = parsed[1];
        }

        for (int i = 1; i < positionIndices.length - 1; i++) {
            Point3D v0 = vertices.get(positionIndices[0]);
            Point3D v1 = vertices.get(positionIndices[i]);
            Point3D v2 = vertices.get(positionIndices[i + 1]);

            Vector3D n0 = normalIndices[0] >= 0 ? normals.get(normalIndices[0]) : null;
            Vector3D n1 = normalIndices[i] >= 0 ? normals.get(normalIndices[i]) : null;
            Vector3D n2 = normalIndices[i + 1] >= 0 ? normals.get(normalIndices[i + 1]) : null;

            mesh.addTriangle(v0, v1, v2, smoothingGroupId, n0, n1, n2);
        }
    }

    private static int parseSmoothingGroup(String line, int lineNumber) {
        String[] parts = line.split("\\s+");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid smoothing group at line " + lineNumber + ": " + line);
        }

        String token = parts[1].toLowerCase();
        if ("off".equals(token) || "0".equals(token)) {
            return 0;
        }
        if ("on".equals(token)) {
            return 1;
        }

        try {
            int groupId = Integer.parseInt(token);
            if (groupId < 0) {
                throw new IllegalArgumentException("Smoothing group id must be >= 0 at line " + lineNumber + ": " + line);
            }
            return groupId;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid smoothing group token at line " + lineNumber + ": " + token, ex);
        }
    }

    private static int[] parseFaceVertexIndices(String faceToken, int vertexCount, int textureCount, int normalCount, int lineNumber) {
        String[] tokenParts = faceToken.split("/", -1);
        if (tokenParts.length < 1 || tokenParts.length > 3 || tokenParts[0].isEmpty()) {
            throw new IllegalArgumentException("Invalid face token at line " + lineNumber + ": " + faceToken);
        }

        int positionIndex = parseRequiredIndex(tokenParts[0], vertexCount, "position", lineNumber, faceToken);
        int normalIndex = -1;

        if (tokenParts.length >= 2 && !tokenParts[1].isEmpty()) {
            parseRequiredIndex(tokenParts[1], textureCount, "texture", lineNumber, faceToken);
        }

        if (tokenParts.length == 3 && !tokenParts[2].isEmpty()) {
            normalIndex = parseRequiredIndex(tokenParts[2], normalCount, "normal", lineNumber, faceToken);
        }

        return new int[]{positionIndex, normalIndex};
    }

    private static int parseRequiredIndex(String rawIndex, int elementCount, String label, int lineNumber, String token) {
        int objIndex;
        try {
            objIndex = Integer.parseInt(rawIndex);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + label + " index at line " + lineNumber + ": " + token, ex);
        }

        int index = objIndex > 0 ? objIndex - 1 : elementCount + objIndex;

        if (index < 0 || index >= elementCount) {
            throw new IllegalArgumentException(label + " index out of range at line " + lineNumber + ": " + token);
        }

        return index;
    }

    private static double parseDouble(String rawValue, String label, int lineNumber) {
        try {
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + label + " value at line " + lineNumber + ": " + rawValue, ex);
        }
    }
}
