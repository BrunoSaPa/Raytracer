package raytracer.material;

import raytracer.core.Intersection;
import raytracer.utils.Color;
import raytracer.utils.Vector3D;

public class Material {
    private static final double BUMP_UV_DELTA = 1.0 / 1024.0;

    private final Color baseColor;
    private final double specularStrength;
    private final double shininess;
    private final Color specularColor;
    private final double roughness;
    private final double normalStrength;
    private final Texture2D albedoTexture;
    private final Texture2D normalTexture;
    private final Texture2D roughnessTexture;
    private final Texture2D bumpTexture;
    private final double bumpStrength;

    public Material(Color baseColor, double specularStrength, double shininess, Color specularColor) {
        this(baseColor, specularStrength, shininess, specularColor, 0.0, 1.0, null, null, null, null, 1.0);
    }

    public Material(Color baseColor, double specularStrength, double shininess, Color specularColor, Texture2D albedoTexture) {
        this(baseColor, specularStrength, shininess, specularColor, 0.0, 1.0, albedoTexture, null, null, null, 1.0);
    }

    public Material(
        Color baseColor,
        double specularStrength,
        double shininess,
        Color specularColor,
        double roughness,
        double normalStrength,
        Texture2D albedoTexture,
        Texture2D normalTexture,
        Texture2D roughnessTexture
    ) {
        this(baseColor, specularStrength, shininess, specularColor, roughness, normalStrength, albedoTexture, normalTexture, roughnessTexture, null, 1.0);
    }

    public Material(
        Color baseColor,
        double specularStrength,
        double shininess,
        Color specularColor,
        double roughness,
        double normalStrength,
        Texture2D albedoTexture,
        Texture2D normalTexture,
        Texture2D roughnessTexture,
        Texture2D bumpTexture,
        double bumpStrength
    ) {
        this.baseColor = baseColor == null ? Color.WHITE : baseColor;
        this.specularStrength = Math.max(0.0, specularStrength);
        this.shininess = Math.max(1.0, shininess);
        this.specularColor = specularColor == null ? Color.WHITE : specularColor;
        this.roughness = clamp01(roughness);
        this.normalStrength = Math.max(0.0, normalStrength);
        this.albedoTexture = albedoTexture;
        this.normalTexture = normalTexture;
        this.roughnessTexture = roughnessTexture;
        this.bumpTexture = bumpTexture;
        this.bumpStrength = Math.max(0.0, bumpStrength);
    }

    public static Material fromLegacy(Color color, double specularStrength, double shininess, Color specularColor) {
        return new Material(color, specularStrength, shininess, specularColor, 0.0, 1.0, null, null, null, null, 1.0);
    }

    public Color getBaseColor() {
        return baseColor;
    }

    public double getSpecularStrength() {
        return specularStrength;
    }

    public double getShininess() {
        return shininess;
    }

    public Color getSpecularColor() {
        return specularColor;
    }

    public double getRoughness() {
        return roughness;
    }

    public double getNormalStrength() {
        return normalStrength;
    }

    public boolean hasAlbedoTexture() {
        return albedoTexture != null;
    }

    public Texture2D getAlbedoTexture() {
        return albedoTexture;
    }

    public Texture2D getNormalTexture() {
        return normalTexture;
    }

    public Texture2D getRoughnessTexture() {
        return roughnessTexture;
    }

    public Texture2D getBumpTexture() {
        return bumpTexture;
    }

    public double getBumpStrength() {
        return bumpStrength;
    }

    public Color sampleAlbedo(Intersection hit) {
        if (albedoTexture == null || hit == null || !hit.hasTexCoords()) {
            return baseColor;
        }
        return albedoTexture.sample(hit.getTexU(), hit.getTexV());
    }

    public double sampleRoughness(Intersection hit) {
        if (roughnessTexture == null || hit == null || !hit.hasTexCoords()) {
            return roughness;
        }
        Color map = roughnessTexture.sample(hit.getTexU(), hit.getTexV());
        return clamp01((map.getR() + map.getG() + map.getB()) / 3.0);
    }

    public double sampleShininess(Intersection hit) {
        double r = sampleRoughness(hit);
        double attenuation = 1.0 - r;
        return Math.max(1.0, shininess * attenuation * attenuation);
    }

    public Vector3D sampleNormal(Intersection hit, Vector3D geometricNormal) {
        if (geometricNormal == null) {
            return null;
        }
        if (hit == null || !hit.hasTexCoords() || !hit.hasTangentBasis()) {
            return geometricNormal;
        }

        Vector3D n = geometricNormal.normalize();
        Vector3D tangent = hit.getTangent();
        Vector3D bitangent = hit.getBitangent();
        if (tangent == null || bitangent == null) {
            return n;
        }

        Vector3D t = tangent.subtract(n.multiply(n.dot(tangent))).normalize();
        if (t.length() == 0.0) {
            return n;
        }

        Vector3D b = n.cross(t).normalize();
        if (bitangent.dot(b) < 0.0) {
            b = b.multiply(-1.0);
        }

        if (normalTexture != null) {
            Color texel = normalTexture.sample(hit.getTexU(), hit.getTexV());
            double nx = (texel.getR() * 2.0 - 1.0) * normalStrength;
            double ny = (texel.getG() * 2.0 - 1.0) * normalStrength;
            double nz = texel.getB() * 2.0 - 1.0;
            Vector3D worldNormal = t.multiply(nx).add(b.multiply(ny)).add(n.multiply(nz)).normalize();
            return worldNormal.length() == 0.0 ? n : worldNormal;
        }

        if (bumpTexture != null && bumpStrength > 0.0) {
            double u = hit.getTexU();
            double v = hit.getTexV();
            double hc = sampleHeight(u, v);
            double hu = sampleHeight(u + BUMP_UV_DELTA, v);
            double hv = sampleHeight(u, v + BUMP_UV_DELTA);
            double scale = bumpStrength * normalStrength;
            double dHdU = (hu - hc) * scale;
            double dHdV = (hv - hc) * scale;
            Vector3D worldNormal = t.multiply(-dHdU).add(b.multiply(-dHdV)).add(n).normalize();
            return worldNormal.length() == 0.0 ? n : worldNormal;
        }

        return n;
    }

    private double sampleHeight(double u, double v) {
        Color texel = bumpTexture.sample(u, v);
        return (texel.getR() + texel.getG() + texel.getB()) / 3.0;
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}




