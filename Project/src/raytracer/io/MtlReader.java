package raytracer.io;

import raytracer.material.Material;
import raytracer.material.Texture2D;
import raytracer.utils.Color;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MtlReader {
    private MtlReader() {
    }

    public static Map<String, Material> loadMaterials(String mtlPath) throws IOException {
        Map<String, Material> materials = new HashMap<>();
        File mtlFile = new File(mtlPath).getAbsoluteFile();
        File baseDir = mtlFile.getParentFile();

        String currentName = null;
        Color kd = Color.WHITE;
        Color ks = Color.WHITE;
        double ns = 32.0;
        Texture2D albedoMap = null;
        Texture2D normalMap = null;
        Texture2D roughnessMap = null;
        Texture2D bumpMap = null;
        double bumpStrength = 1.0;

        try (BufferedReader reader = new BufferedReader(new FileReader(mtlFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                String[] tokens = trimmed.split("\\s+");
                if (tokens.length < 2) {
                    continue;
                }

                String key = tokens[0].toLowerCase(Locale.ROOT);
                if ("newmtl".equals(key)) {
                    if (currentName != null) {
                        materials.put(currentName.toLowerCase(Locale.ROOT), buildMaterial(kd, ks, ns, albedoMap, normalMap, roughnessMap, bumpMap, bumpStrength));
                    }
                    currentName = tokens[1];
                    kd = Color.WHITE;
                    ks = Color.WHITE;
                    ns = 32.0;
                    albedoMap = null;
                    normalMap = null;
                    roughnessMap = null;
                    bumpMap = null;
                    bumpStrength = 1.0;
                } else if ("kd".equals(key) && tokens.length >= 4) {
                    kd = new Color(parseDouble(tokens[1]), parseDouble(tokens[2]), parseDouble(tokens[3]));
                } else if ("ks".equals(key) && tokens.length >= 4) {
                    ks = new Color(parseDouble(tokens[1]), parseDouble(tokens[2]), parseDouble(tokens[3]));
                } else if ("ns".equals(key) && tokens.length >= 2) {
                    ns = Math.max(1.0, parseDouble(tokens[1]));
                } else if ("map_kd".equals(key)) {
                    albedoMap = loadTextureFromLine(trimmed, baseDir);
                } else if ("map_pr".equals(key)) {
                    roughnessMap = loadTextureFromLine(trimmed, baseDir);
                } else if ("norm".equals(key) || "map_normal".equals(key)) {
                    normalMap = loadTextureFromLine(trimmed, baseDir);
                } else if ("map_bump".equals(key) || "bump".equals(key)) {
                    bumpMap = loadTextureFromLine(trimmed, baseDir);
                    bumpStrength = parseBumpStrengthFromLine(trimmed);
                }
            }
        }

        if (currentName != null) {
            materials.put(currentName.toLowerCase(Locale.ROOT), buildMaterial(kd, ks, ns, albedoMap, normalMap, roughnessMap, bumpMap, bumpStrength));
        }

        return materials;
    }

    private static Material buildMaterial(
        Color kd,
        Color ks,
        double ns,
        Texture2D albedoMap,
        Texture2D normalMap,
        Texture2D roughnessMap,
        Texture2D bumpMap,
        double bumpStrength
    ) {
        double specularStrength = Math.max(0.0, Math.min(1.0, (ks.getR() + ks.getG() + ks.getB()) / 3.0));
        return new Material(kd, specularStrength, ns, ks, 0.0, 1.0, albedoMap, normalMap, roughnessMap, bumpMap, bumpStrength);
    }

    private static double parseBumpStrengthFromLine(String line) {
        String[] tokens = line.split("\\s+");
        for (int i = 1; i < tokens.length - 1; i++) {
            if ("-bm".equalsIgnoreCase(tokens[i])) {
                return Math.max(0.0, parseDouble(tokens[i + 1]));
            }
        }
        return 1.0;
    }

    private static Texture2D loadTextureFromLine(String line, File baseDir) throws IOException {
        String[] tokens = line.split("\\s+");
        for (int i = tokens.length - 1; i >= 1; i--) {
            String token = tokens[i];
            if (token.startsWith("-")) {
                continue;
            }
            File textureFile = resolvePath(token, baseDir);
            if (textureFile.exists()) {
                return Texture2D.load(textureFile.getPath());
            }
        }
        return null;
    }

    private static File resolvePath(String rawPath, File baseDir) {
        File direct = new File(rawPath);
        if (direct.isAbsolute() || direct.exists()) {
            return direct;
        }
        return baseDir == null ? direct : new File(baseDir, rawPath);
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }
}

