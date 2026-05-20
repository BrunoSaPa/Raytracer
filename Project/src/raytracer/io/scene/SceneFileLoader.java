package raytracer.io.scene;

import raytracer.core.Scene;
import raytracer.geometry.MeshObject3D;
import raytracer.geometry.Sphere;
import raytracer.geometry.TriangleCullingMode;
import raytracer.io.ObjReader;
import raytracer.lighting.DirectionalLight;
import raytracer.lighting.PointLight;
import raytracer.lighting.SpotLight;
import raytracer.renderer.Camera;
import raytracer.utils.Color;
import raytracer.utils.Point3D;
import raytracer.utils.Vector3D;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

public final class SceneFileLoader {
    private static final int DEFAULT_WIDTH = 1080;
    private static final int DEFAULT_HEIGHT = 1080;

    private SceneFileLoader() {
    }

    public static SceneLoadResult load(String filePath) throws IOException {
        Scene scene = new Scene();

        int width = DEFAULT_WIDTH;
        int height = DEFAULT_HEIGHT;
        Color background = Color.BLACK;
        String outputPath = "output/render.png";
        int threadCount = Math.max(1, Runtime.getRuntime().availableProcessors());
        int tileSize = 32;

        Point3D cameraPosition = new Point3D(0, 0, 0.5);
        Point3D cameraLookAt = new Point3D(0, 0, -1);
        Vector3D worldUp = new Vector3D(0, 1, 0);
        double fov = 80.0;
        double near = 0.01;
        double far = 1000.0;

        File baseDir = new File(filePath).getAbsoluteFile().getParentFile();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                String[] tokens = trimmed.split("\\s+");
                if (tokens.length < 3) {
                    throw parseError(lineNumber, "Expected at least 3 tokens.");
                }

                String category = tokens[0].toLowerCase(Locale.ROOT);
                String key = tokens[1].toLowerCase(Locale.ROOT);

                if ("camera".equals(category)) {
                    if ("position".equals(key)) {
                        requireTokenCount(tokens, 5, lineNumber, "camera position x y z");
                        cameraPosition = parsePoint(tokens, 2, lineNumber);
                    } else if ("lookat".equals(key)) {
                        requireTokenCount(tokens, 5, lineNumber, "camera lookAt x y z");
                        cameraLookAt = parsePoint(tokens, 2, lineNumber);
                    } else if ("up".equals(key)) {
                        requireTokenCount(tokens, 5, lineNumber, "camera up x y z");
                        worldUp = parseVector(tokens, 2, lineNumber);
                    } else if ("fov".equals(key)) {
                        requireTokenCount(tokens, 3, lineNumber, "camera fov value");
                        fov = parseDouble(tokens[2], lineNumber, "camera fov");
                    } else if ("near".equals(key)) {
                        requireTokenCount(tokens, 3, lineNumber, "camera near value");
                        near = parseDouble(tokens[2], lineNumber, "camera near");
                    } else if ("far".equals(key)) {
                        requireTokenCount(tokens, 3, lineNumber, "camera far value");
                        far = parseDouble(tokens[2], lineNumber, "camera far");
                    } else {
                        throw parseError(lineNumber, "Unknown camera key: " + key);
                    }
                } else if ("render".equals(category)) {
                    if ("width".equals(key)) {
                        requireTokenCount(tokens, 3, lineNumber, "render width value");
                        width = parseInt(tokens[2], lineNumber, "render width");
                    } else if ("height".equals(key)) {
                        requireTokenCount(tokens, 3, lineNumber, "render height value");
                        height = parseInt(tokens[2], lineNumber, "render height");
                    } else if ("output".equals(key)) {
                        requireTokenCount(tokens, 3, lineNumber, "render output path");
                        outputPath = tokens[2];
                    } else if ("background".equals(key)) {
                        requireTokenCount(tokens, 5, lineNumber, "render background r g b");
                        background = parseColor(tokens, 2, lineNumber);
                    } else if ("threads".equals(key)) {
                        requireTokenCount(tokens, 3, lineNumber, "render threads value");
                        threadCount = parseInt(tokens[2], lineNumber, "render threads");
                    } else if ("tile".equals(key)) {
                        requireTokenCount(tokens, 3, lineNumber, "render tile value");
                        tileSize = parseInt(tokens[2], lineNumber, "render tile");
                    } else {
                        throw parseError(lineNumber, "Unknown render key: " + key);
                    }
                } else if ("light".equals(category)) {
                    if ("directional".equals(key)) {
                        requireTokenCount(tokens, 9, lineNumber, "light directional dx dy dz r g b intensity");
                        Vector3D direction = parseVector(tokens, 2, lineNumber);
                        Color color = parseColor(tokens, 5, lineNumber);
                        double intensity = parseDouble(tokens[8], lineNumber, "directional intensity");
                        scene.addLight(new DirectionalLight(direction, color, intensity));
                    } else if ("point".equals(key)) {
                        requireTokenCount(tokens, 9, lineNumber, "light point px py pz r g b intensity");
                        Point3D position = parsePoint(tokens, 2, lineNumber);
                        Color color = parseColor(tokens, 5, lineNumber);
                        double intensity = parseDouble(tokens[8], lineNumber, "point intensity");
                        scene.addLight(new PointLight(position, color, intensity));
                    } else if ("spot".equals(key)) {
                        requireTokenCount(tokens, 13, lineNumber, "light spot px py pz dx dy dz cutoff r g b intensity");
                        Point3D position = parsePoint(tokens, 2, lineNumber);
                        Vector3D direction = parseVector(tokens, 5, lineNumber);
                        double cutoff = parseDouble(tokens[8], lineNumber, "spot cutoff");
                        Color color = parseColor(tokens, 9, lineNumber);
                        double intensity = parseDouble(tokens[12], lineNumber, "spot intensity");
                        scene.addLight(new SpotLight(position, direction, cutoff, color, intensity));
                    } else {
                        throw parseError(lineNumber, "Unknown light type: " + key);
                    }
                } else if ("mesh".equals(category)) {
                    parseMeshLine(scene, tokens, lineNumber, baseDir);
                } else if ("sphere".equals(category)) {
                    parseSphereLine(scene, tokens, lineNumber);
                } else {
                    throw parseError(lineNumber, "Unknown category: " + category);
                }
            }
        }

        validate(width, height, near, far, threadCount, tileSize);

        Camera camera = new Camera(cameraPosition, cameraLookAt, worldUp, fov, near, far, width, height);
        return new SceneLoadResult(scene, camera, width, height, background, outputPath, threadCount, tileSize);
    }

    private static void parseMeshLine(Scene scene, String[] tokens, int lineNumber, File baseDir) throws IOException {
        requireTokenCount(tokens, 7, lineNumber, "mesh path r g b culling [options]");

        String rawPath = tokens[1];
        File meshFile = resolvePath(rawPath, baseDir);

        Color color = parseColor(tokens, 2, lineNumber);
        TriangleCullingMode cullingMode = parseCulling(tokens[5], lineNumber);

        double fit = 0.0;
        double scale = 1.0;
        Vector3D translate = new Vector3D(0, 0, 0);
        Vector3D rotate = new Vector3D(0, 0, 0);
        double specular = 0.0;
        double shininess = 32.0;
        Color specColor = Color.WHITE;

        for (int i = 6; i < tokens.length; i++) {
            String[] kv = tokens[i].split("=", 2);
            if (kv.length != 2) {
                throw parseError(lineNumber, "Invalid mesh option token: " + tokens[i]);
            }

            String option = kv[0].toLowerCase(Locale.ROOT);
            String value = kv[1];

            if ("fit".equals(option)) {
                fit = parseDouble(value, lineNumber, "mesh fit");
            } else if ("scale".equals(option)) {
                scale = parseDouble(value, lineNumber, "mesh scale");
            } else if ("translate".equals(option)) {
                translate = parseCsvVector(value, lineNumber, "mesh translate");
            } else if ("rotate".equals(option)) {
                rotate = parseCsvVector(value, lineNumber, "mesh rotate");
            } else if ("spec".equals(option)) {
                specular = parseDouble(value, lineNumber, "mesh spec");
            } else if ("shininess".equals(option)) {
                shininess = parseDouble(value, lineNumber, "mesh shininess");
            } else if ("speccolor".equals(option)) {
                specColor = parseCsvColor(value, lineNumber, "mesh speccolor");
            } else {
                throw parseError(lineNumber, "Unknown mesh option: " + option);
            }
        }

        MeshObject3D mesh = ObjReader.loadAsMesh(meshFile.getPath(), color, cullingMode, shininess);
        if (fit > 0.0) {
            mesh.fitToMaxDimension(fit);
        }
        if (scale != 1.0) {
            mesh.scaleUniformFromCentroid(scale);
        }
        if (rotate.x != 0.0) {
            mesh.rotateXFromCentroid(rotate.x);
        }
        if (rotate.y != 0.0) {
            mesh.rotateYFromCentroid(rotate.y);
        }
        if (rotate.z != 0.0) {
            mesh.rotateZFromCentroid(rotate.z);
        }
        if (translate.x != 0.0 || translate.y != 0.0 || translate.z != 0.0) {
            mesh.translate(translate);
        }
        mesh.setSpecularStrength(specular);
        mesh.setShininess(shininess);
        mesh.setSpecularColor(specColor);

        scene.addObject(mesh);
    }

    private static File resolvePath(String rawPath, File baseDir) {
        File direct = new File(rawPath);
        if (direct.isAbsolute() || direct.exists()) {
            return direct;
        }
        if (baseDir == null) {
            return direct;
        }
        return new File(baseDir, rawPath);
    }

    private static void parseSphereLine(Scene scene, String[] tokens, int lineNumber) {
        requireTokenCount(tokens, 9, lineNumber, "sphere cx cy cz radius r g b [options]");

        Point3D center = parsePoint(tokens, 1, lineNumber);
        double radius = parseDouble(tokens[4], lineNumber, "sphere radius");
        Color color = parseColor(tokens, 5, lineNumber);

        double specular = 0.0;
        double shininess = 32.0;
        Color specColor = Color.WHITE;

        for (int i = 8; i < tokens.length; i++) {
            String[] kv = tokens[i].split("=", 2);
            if (kv.length != 2) {
                throw parseError(lineNumber, "Invalid sphere option token: " + tokens[i]);
            }

            String option = kv[0].toLowerCase(Locale.ROOT);
            String value = kv[1];
            if ("spec".equals(option)) {
                specular = parseDouble(value, lineNumber, "sphere spec");
            } else if ("shininess".equals(option)) {
                shininess = parseDouble(value, lineNumber, "sphere shininess");
            } else if ("speccolor".equals(option)) {
                specColor = parseCsvColor(value, lineNumber, "sphere speccolor");
            } else {
                throw parseError(lineNumber, "Unknown sphere option: " + option);
            }
        }

        Sphere sphere = new Sphere(center, radius, color);
        sphere.setSpecularStrength(specular);
        sphere.setShininess(shininess);
        sphere.setSpecularColor(specColor);
        scene.addObject(sphere);
    }

    private static Point3D parsePoint(String[] tokens, int startIndex, int lineNumber) {
        return new Point3D(
            parseDouble(tokens[startIndex], lineNumber, "x"),
            parseDouble(tokens[startIndex + 1], lineNumber, "y"),
            parseDouble(tokens[startIndex + 2], lineNumber, "z")
        );
    }

    private static Vector3D parseVector(String[] tokens, int startIndex, int lineNumber) {
        return new Vector3D(
            parseDouble(tokens[startIndex], lineNumber, "x"),
            parseDouble(tokens[startIndex + 1], lineNumber, "y"),
            parseDouble(tokens[startIndex + 2], lineNumber, "z")
        );
    }

    private static Color parseColor(String[] tokens, int startIndex, int lineNumber) {
        return new Color(
            parseDouble(tokens[startIndex], lineNumber, "r"),
            parseDouble(tokens[startIndex + 1], lineNumber, "g"),
            parseDouble(tokens[startIndex + 2], lineNumber, "b")
        );
    }

    private static Vector3D parseCsvVector(String value, int lineNumber, String label) {
        String[] parts = value.split(",");
        if (parts.length != 3) {
            throw parseError(lineNumber, label + " must have 3 comma-separated numbers.");
        }
        return new Vector3D(
            parseDouble(parts[0], lineNumber, label + " x"),
            parseDouble(parts[1], lineNumber, label + " y"),
            parseDouble(parts[2], lineNumber, label + " z")
        );
    }

    private static Color parseCsvColor(String value, int lineNumber, String label) {
        String[] parts = value.split(",");
        if (parts.length != 3) {
            throw parseError(lineNumber, label + " must have 3 comma-separated numbers.");
        }
        return new Color(
            parseDouble(parts[0], lineNumber, label + " r"),
            parseDouble(parts[1], lineNumber, label + " g"),
            parseDouble(parts[2], lineNumber, label + " b")
        );
    }

    private static TriangleCullingMode parseCulling(String token, int lineNumber) {
        String value = token.toLowerCase(Locale.ROOT);
        if ("none".equals(value)) {
            return TriangleCullingMode.NONE;
        }
        if ("back".equals(value) || "back_face".equals(value)) {
            return TriangleCullingMode.BACK_FACE;
        }
        if ("front".equals(value) || "front_face".equals(value)) {
            return TriangleCullingMode.FRONT_FACE;
        }
        throw parseError(lineNumber, "Invalid culling mode: " + token);
    }

    private static void requireTokenCount(String[] tokens, int expectedMinimum, int lineNumber, String usage) {
        if (tokens.length < expectedMinimum) {
            throw parseError(lineNumber, "Invalid syntax. Expected: " + usage);
        }
    }

    private static void validate(int width, int height, double near, double far, int threadCount, int tileSize) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Render width and height must be > 0.");
        }
        if (near <= 0.0 || far <= near) {
            throw new IllegalArgumentException("Camera near/far planes are invalid. Expected 0 < near < far.");
        }
        if (threadCount <= 0) {
            throw new IllegalArgumentException("Render threads must be > 0.");
        }
        if (tileSize <= 0) {
            throw new IllegalArgumentException("Render tile size must be > 0.");
        }
    }

    private static int parseInt(String rawValue, int lineNumber, String label) {
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException ex) {
            throw parseError(lineNumber, "Invalid integer for " + label + ": " + rawValue);
        }
    }

    private static double parseDouble(String rawValue, int lineNumber, String label) {
        try {
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException ex) {
            throw parseError(lineNumber, "Invalid number for " + label + ": " + rawValue);
        }
    }

    private static IllegalArgumentException parseError(int lineNumber, String message) {
        return new IllegalArgumentException("Scene parse error at line " + lineNumber + ": " + message);
    }
}


