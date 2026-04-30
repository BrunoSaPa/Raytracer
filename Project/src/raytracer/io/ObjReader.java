package raytracer.io;

import raytracer.core.Scene;
import raytracer.geometry.MeshObject3D;
import raytracer.geometry.TriangleCullingMode;
import raytracer.utils.Color;
import raytracer.utils.Point3D;

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
        MeshObject3D mesh = new MeshObject3D(color, cullingMode);

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
                } else if (trimmed.startsWith("f ")) {
                    parseFaceAndAddTriangles(trimmed, vertices, mesh, lineNumber);
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

        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        vertices.add(new Point3D(x, y, z));
    }

    private static void parseFaceAndAddTriangles(String line, List<Point3D> vertices, MeshObject3D mesh, int lineNumber) {
        String[] parts = line.split("\\s+");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Face needs at least 3 vertices at line " + lineNumber + ": " + line);
        }

        //convert to 0 based since f accounts for the 0 space, i need to start the indices from 0 aswell
        int[] positionIndices = new int[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            positionIndices[i - 1] = parsePositionIndex(parts[i], vertices.size(), lineNumber);
        }

        for (int i = 1; i < positionIndices.length - 1; i++) {
            Point3D v0 = vertices.get(positionIndices[0]);
            Point3D v1 = vertices.get(positionIndices[i]);
            Point3D v2 = vertices.get(positionIndices[i + 1]);
            mesh.addTriangle(v0, v1, v2);
        }
    }

    private static int parsePositionIndex(String faceToken, int vertexCount, int lineNumber) {
        //since i dont account for vertex normals or textures yet, i am only concerned about their position
        if (faceToken.contains("/")) {
            throw new IllegalArgumentException("Only position only faces are supported (f v v v). Invalid token at line " + lineNumber + ": " + faceToken);
        }

        int objIndex = Integer.parseInt(faceToken);
        //as seen in some obj readers, index could be negative, meaning that we start backwards from the end of the vertex list, so we need to convert that to a 0 based index aswell ( https://www.scratchapixel.com/lessons/3d-basic-rendering/obj-file-format/obj-file-format.html )
        int index = objIndex > 0 ? objIndex - 1 : vertexCount + objIndex;


        //probe thta that index is valid and within range
        if (index < 0 || index >= vertexCount) {
            throw new IllegalArgumentException("Face index out of range at line " + lineNumber + ": " + faceToken);
        }

        return index;
    }
}



