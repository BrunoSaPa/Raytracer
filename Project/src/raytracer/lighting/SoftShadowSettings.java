package raytracer.lighting;

import raytracer.utils.Vector3D;

public final class SoftShadowSettings {
    public static final int POINT_LIGHT_SCRAMBLE_SALT = 0x9E3779B9;
    public static final int SPOT_LIGHT_SCRAMBLE_SALT = 0x7F4A7C15;
    public static final int DIRECTIONAL_LIGHT_SCRAMBLE_SALT = 0x94D049BB;

    private final int softShadowSamples;
    private final double pointLightRadius;
    private final double spotLightRadius;
    private final double directionalLightAngleDegrees;
    private final double directionalLightAngleCos;

    public SoftShadowSettings(
        int softShadowSamples,
        double pointLightRadius,
        double spotLightRadius,
        double directionalLightAngleDegrees
    ) {
        this.softShadowSamples = Math.max(1, softShadowSamples);
        this.pointLightRadius = Math.max(0.0, pointLightRadius);
        this.spotLightRadius = Math.max(0.0, spotLightRadius);
        this.directionalLightAngleDegrees = Math.max(0.0, directionalLightAngleDegrees);
        this.directionalLightAngleCos = Math.cos(Math.toRadians(this.directionalLightAngleDegrees));
    }

    public int getSoftShadowSamples() {
        return softShadowSamples;
    }

    public double getPointLightRadius() {
        return pointLightRadius;
    }

    public double getSpotLightRadius() {
        return spotLightRadius;
    }

    public double getDirectionalLightAngleDegrees() {
        return directionalLightAngleDegrees;
    }

    public double getDirectionalLightAngleCos() {
        return directionalLightAngleCos;
    }

    public static Vector3D sampleUnitSphere(int sampleIndex, int sampleCount, int scrambleSalt) {
        double u1 = stratifiedU(sampleIndex, sampleCount);
        double u2 = radicalInverse(sampleIndex ^ scrambleSalt);
        double z = 1.0 - 2.0 * u1;
        double r = Math.sqrt(Math.max(0.0, 1.0 - z * z));
        double phi = 2.0 * Math.PI * u2;
        return new Vector3D(r * Math.cos(phi), r * Math.sin(phi), z);
    }

    public static Vector3D buildPerpendicular(Vector3D n) {
        Vector3D axis = Math.abs(n.z) < 0.999 ? new Vector3D(0, 0, 1) : new Vector3D(0, 1, 0);
        return axis.cross(n).normalize();
    }

    public static double stratifiedU(int sampleIndex, int sampleCount) {
        return (sampleIndex + 0.5) / sampleCount;
    }

    public static double radicalInverse(int sampleIndex) {
        int reversed = Integer.reverse(sampleIndex);
        return (reversed & 0xffffffffL) / 4294967296.0;
    }
}




