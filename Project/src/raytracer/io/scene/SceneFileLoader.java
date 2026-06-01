package raytracer.io.scene;

import raytracer.core.Scene;
import raytracer.geometry.MeshGroup3D;
import raytracer.geometry.MeshObject3D;
import raytracer.geometry.Sphere;
import raytracer.geometry.TriangleCullingMode;
import raytracer.io.ObjReader;
import raytracer.lighting.DirectionalLight;
import raytracer.lighting.PointLight;
import raytracer.lighting.SpotLight;
import raytracer.material.Material;
import raytracer.material.Texture2D;
import raytracer.renderer.Camera;
import raytracer.utils.Color;
import raytracer.utils.Point3D;
import raytracer.utils.Vector3D;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        int softShadowSamples = 1;
        double pointLightRadius = 0.0;
        double spotLightRadius = 0.0;
        double directionalLightAngleDegrees = 0.0;
        int maxRayDepth = 4;

        Point3D cameraPosition = new Point3D(0, 0, 0.5);
        Point3D cameraLookAt = new Point3D(0, 0, -1);
        Vector3D worldUp = new Vector3D(0, 1, 0);
        double fov = 80.0;
        double near = 0.01;
        double far = 1000.0;
        Map<String, Material> materialsByName = new HashMap<>();

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

                String[] tokens = tokenize(trimmed);
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
                    } else if ("shadowsamples".equals(key)) {
                        requireTokenCount(tokens, 3, lineNumber, "render shadowSamples value");
                        softShadowSamples = parseInt(tokens[2], lineNumber, "render shadowSamples");
                    } else if ("pointlightradius".equals(key)) {
                        requireTokenCount(tokens, 3, lineNumber, "render pointLightRadius value");
                        pointLightRadius = parseDouble(tokens[2], lineNumber, "render pointLightRadius");
                    } else if ("spotlightradius".equals(key)) {
                        requireTokenCount(tokens, 3, lineNumber, "render spotLightRadius value");
                        spotLightRadius = parseDouble(tokens[2], lineNumber, "render spotLightRadius");
                    } else if ("directionalangle".equals(key)) {
                        requireTokenCount(tokens, 3, lineNumber, "render directionalAngle degrees");
                        directionalLightAngleDegrees = parseDouble(tokens[2], lineNumber, "render directionalAngle");
                    } else if ("maxdepth".equals(key)) {
                        requireTokenCount(tokens, 3, lineNumber, "render maxDepth value");
                        maxRayDepth = parseInt(tokens[2], lineNumber, "render maxDepth");
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
                    parseMeshLine(scene, tokens, lineNumber, baseDir, materialsByName);
                } else if ("sphere".equals(category)) {
                    parseSphereLine(scene, tokens, lineNumber, baseDir, materialsByName);
                } else if ("material".equals(category)) {
                    parseMaterialLine(materialsByName, tokens, lineNumber, baseDir);
                } else {
                    throw parseError(lineNumber, "Unknown category: " + category);
                }
            }
        }

        validate(
            width,
            height,
            near,
            far,
            threadCount,
            tileSize,
            softShadowSamples,
            pointLightRadius,
            spotLightRadius,
            directionalLightAngleDegrees,
            maxRayDepth
        );

        Camera camera = new Camera(cameraPosition, cameraLookAt, worldUp, fov, near, far, width, height);
        return new SceneLoadResult(
            scene,
            camera,
            width,
            height,
            background,
            outputPath,
            threadCount,
            tileSize,
            softShadowSamples,
            pointLightRadius,
            spotLightRadius,
            directionalLightAngleDegrees,
            maxRayDepth
        );
    }

    private static void parseMeshLine(
        Scene scene,
        String[] tokens,
        int lineNumber,
        File baseDir,
        Map<String, Material> materialsByName
    ) throws IOException {
        requireTokenCount(tokens, 6, lineNumber, "mesh path r g b culling [options]");

        String rawPath = tokens[1];
        File meshFile = resolvePath(rawPath, baseDir);

        Color color = parseColor(tokens, 2, lineNumber);
        TriangleCullingMode cullingMode = parseCulling(tokens[5], lineNumber);

        double fit = 0.0;
        double scale = 1.0;
        Vector3D translate = new Vector3D(0, 0, 0);
        Vector3D rotate = new Vector3D(0, 0, 0);
        MaterialOverrides overrides = new MaterialOverrides();
        String materialName = null;

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
                overrides.specularStrength = parseDouble(value, lineNumber, "mesh spec");
            } else if ("shininess".equals(option)) {
                overrides.shininess = parseDouble(value, lineNumber, "mesh shininess");
            } else if ("roughness".equals(option)) {
                overrides.roughness = parseDouble(value, lineNumber, "mesh roughness");
            } else if ("normalstrength".equals(option)) {
                overrides.normalStrength = parseDouble(value, lineNumber, "mesh normalstrength");
            } else if ("speccolor".equals(option)) {
                overrides.specularColor = parseCsvColor(value, lineNumber, "mesh speccolor");
            } else if ("material".equals(option)) {
                materialName = value.toLowerCase(Locale.ROOT);
            } else if ("albedomap".equals(option)) {
                overrides.albedoMapPath = value;
                overrides.albedoMapSet = true;
            } else if ("normalmap".equals(option)) {
                overrides.normalMapPath = value;
                overrides.normalMapSet = true;
            } else if ("roughnessmap".equals(option)) {
                overrides.roughnessMapPath = value;
                overrides.roughnessMapSet = true;
            } else if ("reflectivity".equals(option)) {
                overrides.reflectivity = parseDouble(value, lineNumber, "mesh reflectivity");
            } else if ("transmission".equals(option)) {
                overrides.transmission = parseDouble(value, lineNumber, "mesh transmission");
            } else if ("ior".equals(option)) {
                overrides.ior = parseDouble(value, lineNumber, "mesh ior");
            } else if ("metalness".equals(option)) {
                overrides.metalness = parseDouble(value, lineNumber, "mesh metalness");
            } else {
                throw parseError(lineNumber, "Unknown mesh option: " + option);
            }
        }

        double loadShininess = overrides.shininess != null ? overrides.shininess : 32.0;
        List<MeshObject3D> meshes = ObjReader.loadAsMeshes(meshFile.getPath(), color, cullingMode, loadShininess);
        MeshGroup3D meshGroup = new MeshGroup3D(meshes);

        if (fit > 0.0) {
            meshGroup.fitToMaxDimension(fit);
        }
        if (scale != 1.0) {
            meshGroup.scaleUniformFromCentroid(scale);
        }

        Point3D rotationPivot = meshGroup.getCentroid();
        if (rotate.x != 0.0) {
            meshGroup.rotateX(rotate.x, rotationPivot);
        }
        if (rotate.y != 0.0) {
            meshGroup.rotateY(rotate.y, rotationPivot);
        }
        if (rotate.z != 0.0) {
            meshGroup.rotateZ(rotate.z, rotationPivot);
        }
        if (translate.x != 0.0 || translate.y != 0.0 || translate.z != 0.0) {
            meshGroup.translate(translate);
        }

        Material namedMaterial = resolveMaterial(materialName, materialsByName, lineNumber);
        if (namedMaterial != null || overrides.hasAnyOverride()) {
            ResolvedMaterialOverrides resolvedOverrides = resolveMaterialOverrides(overrides, baseDir);
            for (MeshObject3D mesh : meshes) {
                Material baseMaterial = namedMaterial != null ? namedMaterial : mesh.getMaterial();
                mesh.setMaterial(applyMaterialOverrides(baseMaterial, resolvedOverrides, color));
            }
        }

        for (MeshObject3D mesh : meshes) {
            System.out.println("Adding mesh with: " + mesh.getTriangleCount() + " triangles.");
            scene.addObject(mesh);
        }
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

    private static void parseSphereLine(
        Scene scene,
        String[] tokens,
        int lineNumber,
        File baseDir,
        Map<String, Material> materialsByName
    ) throws IOException {
        requireTokenCount(tokens, 8, lineNumber, "sphere cx cy cz radius r g b [options]");

        Point3D center = parsePoint(tokens, 1, lineNumber);
        double radius = parseDouble(tokens[4], lineNumber, "sphere radius");
        Color color = parseColor(tokens, 5, lineNumber);

        MaterialOverrides overrides = new MaterialOverrides();
        String materialName = null;

        for (int i = 8; i < tokens.length; i++) {
            String[] kv = tokens[i].split("=", 2);
            if (kv.length != 2) {
                throw parseError(lineNumber, "Invalid sphere option token: " + tokens[i]);
            }

            String option = kv[0].toLowerCase(Locale.ROOT);
            String value = kv[1];
            if ("spec".equals(option)) {
                overrides.specularStrength = parseDouble(value, lineNumber, "sphere spec");
            } else if ("shininess".equals(option)) {
                overrides.shininess = parseDouble(value, lineNumber, "sphere shininess");
            } else if ("roughness".equals(option)) {
                overrides.roughness = parseDouble(value, lineNumber, "sphere roughness");
            } else if ("normalstrength".equals(option)) {
                overrides.normalStrength = parseDouble(value, lineNumber, "sphere normalstrength");
            } else if ("speccolor".equals(option)) {
                overrides.specularColor = parseCsvColor(value, lineNumber, "sphere speccolor");
            } else if ("material".equals(option)) {
                materialName = value.toLowerCase(Locale.ROOT);
            } else if ("albedomap".equals(option)) {
                overrides.albedoMapPath = value;
                overrides.albedoMapSet = true;
            } else if ("normalmap".equals(option)) {
                overrides.normalMapPath = value;
                overrides.normalMapSet = true;
            } else if ("roughnessmap".equals(option)) {
                overrides.roughnessMapPath = value;
                overrides.roughnessMapSet = true;
            } else if ("reflectivity".equals(option)) {
                overrides.reflectivity = parseDouble(value, lineNumber, "sphere reflectivity");
            } else if ("transmission".equals(option)) {
                overrides.transmission = parseDouble(value, lineNumber, "sphere transmission");
            } else if ("ior".equals(option)) {
                overrides.ior = parseDouble(value, lineNumber, "sphere ior");
            } else if ("metalness".equals(option)) {
                overrides.metalness = parseDouble(value, lineNumber, "sphere metalness");
            } else {
                throw parseError(lineNumber, "Unknown sphere option: " + option);
            }
        }

        Sphere sphere = new Sphere(center, radius, color);
        Material namedMaterial = resolveMaterial(materialName, materialsByName, lineNumber);
        if (namedMaterial != null || overrides.hasAnyOverride()) {
            ResolvedMaterialOverrides resolvedOverrides = resolveMaterialOverrides(overrides, baseDir);
            Material baseMaterial = namedMaterial != null ? namedMaterial : sphere.getMaterial();
            sphere.setMaterial(applyMaterialOverrides(baseMaterial, resolvedOverrides, color));
        }
        scene.addObject(sphere);
    }

    private static void parseMaterialLine(
        Map<String, Material> materialsByName,
        String[] tokens,
        int lineNumber,
        File baseDir
    ) throws IOException {
        requireTokenCount(tokens, 3, lineNumber, "material name r g b [options] OR material name inherit=baseName [options]");
        String name = tokens[1].toLowerCase(Locale.ROOT);
        Color explicitBaseColor = null;
        String inheritName = null;
        int optionsStart;

        if (tokens.length >= 5 && !tokens[2].contains("=") && !tokens[3].contains("=") && !tokens[4].contains("=")) {
            explicitBaseColor = parseColor(tokens, 2, lineNumber);
            optionsStart = 5;
        } else if (isInheritToken(tokens[2])) {
            inheritName = parseInheritName(tokens[2], lineNumber, "material inherit");
            optionsStart = 3;
        } else {
            throw parseError(lineNumber, "Invalid material syntax. Use RGB base color or inherit=baseName.");
        }

        MaterialOverrides overrides = new MaterialOverrides();

        for (int i = optionsStart; i < tokens.length; i++) {
            String[] kv = tokens[i].split("=", 2);
            if (kv.length != 2) {
                throw parseError(lineNumber, "Invalid material option token: " + tokens[i]);
            }
            String option = kv[0].toLowerCase(Locale.ROOT);
            String value = kv[1];
            if ("inherit".equals(option)) {
                inheritName = parseInheritName(tokens[i], lineNumber, "material inherit");
            } else if ("spec".equals(option)) {
                overrides.specularStrength = parseDouble(value, lineNumber, "material spec");
            } else if ("shininess".equals(option)) {
                overrides.shininess = parseDouble(value, lineNumber, "material shininess");
            } else if ("roughness".equals(option)) {
                overrides.roughness = parseDouble(value, lineNumber, "material roughness");
            } else if ("normalstrength".equals(option)) {
                overrides.normalStrength = parseDouble(value, lineNumber, "material normalstrength");
            } else if ("speccolor".equals(option)) {
                overrides.specularColor = parseCsvColor(value, lineNumber, "material speccolor");
            } else if ("albedomap".equals(option)) {
                overrides.albedoMapPath = value;
                overrides.albedoMapSet = true;
            } else if ("normalmap".equals(option)) {
                overrides.normalMapPath = value;
                overrides.normalMapSet = true;
            } else if ("roughnessmap".equals(option)) {
                overrides.roughnessMapPath = value;
                overrides.roughnessMapSet = true;
            } else if ("reflectivity".equals(option)) {
                overrides.reflectivity = parseDouble(value, lineNumber, "material reflectivity");
            } else if ("transmission".equals(option)) {
                overrides.transmission = parseDouble(value, lineNumber, "material transmission");
            } else if ("ior".equals(option)) {
                overrides.ior = parseDouble(value, lineNumber, "material ior");
            } else if ("metalness".equals(option)) {
                overrides.metalness = parseDouble(value, lineNumber, "material metalness");
            } else {
                throw parseError(lineNumber, "Unknown material option: " + option);
            }
        }

        Material inheritedBase = resolveMaterial(inheritName, materialsByName, lineNumber);
        Color fallbackBaseColor = explicitBaseColor != null
            ? explicitBaseColor
            : (inheritedBase != null ? inheritedBase.getBaseColor() : Color.WHITE);

        ResolvedMaterialOverrides resolvedOverrides = resolveMaterialOverrides(overrides, baseDir);
        Material material = applyMaterialOverrides(inheritedBase, resolvedOverrides, fallbackBaseColor);
        materialsByName.put(name, material);
    }

    private static Material applyMaterialOverrides(Material baseMaterial, ResolvedMaterialOverrides overrides, Color fallbackBaseColor) {
        Color baseColor = baseMaterial != null ? baseMaterial.getBaseColor() : fallbackBaseColor;
        double specular = baseMaterial != null ? baseMaterial.getSpecularStrength() : 0.0;
        double shininess = baseMaterial != null ? baseMaterial.getShininess() : 32.0;
        Color specColor = baseMaterial != null ? baseMaterial.getSpecularColor() : Color.WHITE;
        double roughness = baseMaterial != null ? baseMaterial.getRoughness() : 0.0;
        double normalStrength = baseMaterial != null ? baseMaterial.getNormalStrength() : 1.0;
        Texture2D albedoMap = baseMaterial != null ? baseMaterial.getAlbedoTexture() : null;
        Texture2D normalMap = baseMaterial != null ? baseMaterial.getNormalTexture() : null;
        Texture2D roughnessMap = baseMaterial != null ? baseMaterial.getRoughnessTexture() : null;
        Texture2D bumpMap = baseMaterial != null ? baseMaterial.getBumpTexture() : null;
        double bumpStrength = baseMaterial != null ? baseMaterial.getBumpStrength() : 1.0;
        double reflectivity = baseMaterial != null ? baseMaterial.getReflectivity() : 0.0;
        double transmission = baseMaterial != null ? baseMaterial.getTransmission() : 0.0;
        double ior = baseMaterial != null ? baseMaterial.getIor() : 1.5;
        double metalness = baseMaterial != null ? baseMaterial.getMetalness() : 0.0;

        if (overrides.specularStrength != null) {
            specular = overrides.specularStrength;
        }
        if (overrides.shininess != null) {
            shininess = overrides.shininess;
        }
        if (overrides.specularColor != null) {
            specColor = overrides.specularColor;
        }
        if (overrides.roughness != null) {
            roughness = overrides.roughness;
        }
        if (overrides.normalStrength != null) {
            normalStrength = overrides.normalStrength;
        }
        if (overrides.albedoMapSet) {
            albedoMap = overrides.albedoMap;
        }
        if (overrides.normalMapSet) {
            normalMap = overrides.normalMap;
        }
        if (overrides.roughnessMapSet) {
            roughnessMap = overrides.roughnessMap;
        }
        if (overrides.reflectivity != null) {
            reflectivity = overrides.reflectivity;
        }
        if (overrides.transmission != null) {
            transmission = overrides.transmission;
        }
        if (overrides.ior != null) {
            ior = overrides.ior;
        }
        if (overrides.metalness != null) {
            metalness = overrides.metalness;
        }

        return new Material(
            baseColor,
            specular,
            shininess,
            specColor,
            roughness,
            normalStrength,
            albedoMap,
            normalMap,
            roughnessMap,
            bumpMap,
            bumpStrength,
            reflectivity,
            transmission,
            ior,
            metalness
        );
    }

    private static ResolvedMaterialOverrides resolveMaterialOverrides(MaterialOverrides overrides, File baseDir) throws IOException {
        ResolvedMaterialOverrides resolved = new ResolvedMaterialOverrides();
        resolved.specularStrength = overrides.specularStrength;
        resolved.shininess = overrides.shininess;
        resolved.specularColor = overrides.specularColor;
        resolved.roughness = overrides.roughness;
        resolved.normalStrength = overrides.normalStrength;
        resolved.reflectivity = overrides.reflectivity;
        resolved.transmission = overrides.transmission;
        resolved.ior = overrides.ior;
        resolved.metalness = overrides.metalness;

        resolved.albedoMapSet = overrides.albedoMapSet;
        resolved.normalMapSet = overrides.normalMapSet;
        resolved.roughnessMapSet = overrides.roughnessMapSet;

        if (overrides.albedoMapSet) {
            resolved.albedoMap = loadOptionalTexture(overrides.albedoMapPath, baseDir);
        }
        if (overrides.normalMapSet) {
            resolved.normalMap = loadOptionalTexture(overrides.normalMapPath, baseDir);
        }
        if (overrides.roughnessMapSet) {
            resolved.roughnessMap = loadOptionalTexture(overrides.roughnessMapPath, baseDir);
        }
        return resolved;
    }

    private static Texture2D loadOptionalTexture(String path, File baseDir) throws IOException {
        if (path == null || path.isEmpty()) {
            return null;
        }
        File textureFile = resolvePath(path, baseDir);
        return Texture2D.load(textureFile.getPath());
    }

    private static Material resolveMaterial(String name, Map<String, Material> materialsByName, int lineNumber) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        Material material = materialsByName.get(name.toLowerCase(Locale.ROOT));
        if (material == null) {
            throw parseError(lineNumber, "Unknown material name: " + name);
        }
        return material;
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

    private static void validate(
        int width,
        int height,
        double near,
        double far,
        int threadCount,
        int tileSize,
        int softShadowSamples,
        double pointLightRadius,
        double spotLightRadius,
        double directionalLightAngleDegrees,
        int maxRayDepth
    ) {
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
        if (softShadowSamples <= 0) {
            throw new IllegalArgumentException("Render shadowSamples must be > 0.");
        }
        if (pointLightRadius < 0.0) {
            throw new IllegalArgumentException("Render pointLightRadius must be >= 0.");
        }
        if (spotLightRadius < 0.0) {
            throw new IllegalArgumentException("Render spotLightRadius must be >= 0.");
        }
        if (directionalLightAngleDegrees < 0.0 || directionalLightAngleDegrees >= 90.0) {
            throw new IllegalArgumentException("Render directionalAngle must be in [0, 90). ");
        }
        if (maxRayDepth < 0) {
            throw new IllegalArgumentException("Render maxDepth must be >= 0.");
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

    private static boolean isInheritToken(String token) {
        return token != null && token.toLowerCase(Locale.ROOT).startsWith("inherit=");
    }

    private static String parseInheritName(String token, int lineNumber, String label) {
        String[] kv = token.split("=", 2);
        if (kv.length != 2 || kv[1].trim().isEmpty()) {
            throw parseError(lineNumber, label + " must provide a base material name.");
        }
        return kv[1].trim().toLowerCase(Locale.ROOT);
    }

    private static String[] tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (Character.isWhitespace(ch) && !inQuotes) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(ch);
        }

        if (inQuotes) {
            throw new IllegalArgumentException("Unterminated quote in scene line: " + line);
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens.toArray(new String[0]);
    }

    private static final class MaterialOverrides {
        private Double specularStrength;
        private Double shininess;
        private Color specularColor;
        private Double roughness;
        private Double normalStrength;
        private Double reflectivity;
        private Double transmission;
        private Double ior;
        private Double metalness;
        private String albedoMapPath;
        private String normalMapPath;
        private String roughnessMapPath;
        private boolean albedoMapSet;
        private boolean normalMapSet;
        private boolean roughnessMapSet;

        private boolean hasAnyOverride() {
            return specularStrength != null
                || shininess != null
                || specularColor != null
                || roughness != null
                || normalStrength != null
                || reflectivity != null
                || transmission != null
                || ior != null
                || metalness != null
                || albedoMapSet
                || normalMapSet
                || roughnessMapSet;
        }
    }

    private static final class ResolvedMaterialOverrides {
        private Double specularStrength;
        private Double shininess;
        private Color specularColor;
        private Double roughness;
        private Double normalStrength;
        private Double reflectivity;
        private Double transmission;
        private Double ior;
        private Double metalness;
        private Texture2D albedoMap;
        private Texture2D normalMap;
        private Texture2D roughnessMap;
        private boolean albedoMapSet;
        private boolean normalMapSet;
        private boolean roughnessMapSet;
    }
}


