package raytracer.lighting;

import raytracer.utils.Color;
import raytracer.utils.Point3D;
import raytracer.utils.Vector3D;

public class DirectionalLight implements Light {
    private final Vector3D direction;
    private final Color color;
    private final double intensity;


    public DirectionalLight(Vector3D direction, Color color, double intensity) {
        this.direction = direction.normalize();
        this.color = color;
        this.intensity = intensity;
    }

    public Vector3D getDirection() {
        return direction;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public double getIntensity() {
        return intensity;
    }

    @Override
    public LightSample sampleAt(Point3D shadedPoint) {
        Vector3D toLight = direction.multiply(-1.0).normalize();
        return new LightSample(toLight, Double.POSITIVE_INFINITY, color, intensity, 1.0, 1.0);
    }

    @Override
    public int getSoftSampleCount(SoftShadowSettings settings) {
        if (settings.getSoftShadowSamples() <= 1 || settings.getDirectionalLightAngleDegrees() <= 0.0) {
            return 1;
        }
        return settings.getSoftShadowSamples();
    }

    @Override
    public LightSample sampleSoftAt(Point3D shadedPoint, int sampleIndex, int sampleCount, SoftShadowSettings settings) {
        if (sampleCount <= 1 || settings.getDirectionalLightAngleDegrees() <= 0.0) {
            return sampleAt(shadedPoint);
        }

        Vector3D baseToLight = direction.multiply(-1.0).normalize();
        double u1 = SoftShadowSettings.stratifiedU(sampleIndex, sampleCount);
        double u2 = SoftShadowSettings.radicalInverse(sampleIndex ^ SoftShadowSettings.DIRECTIONAL_LIGHT_SCRAMBLE_SALT);
        double cosTheta = 1.0 - u1 * (1.0 - settings.getDirectionalLightAngleCos());
        double sinTheta = Math.sqrt(Math.max(0.0, 1.0 - cosTheta * cosTheta));
        double phi = 2.0 * Math.PI * u2;

        Vector3D tangent = SoftShadowSettings.buildPerpendicular(baseToLight);
        Vector3D bitangent = baseToLight.cross(tangent).normalize();
        Vector3D sampledDirection = tangent.multiply(Math.cos(phi) * sinTheta)
            .add(bitangent.multiply(Math.sin(phi) * sinTheta))
            .add(baseToLight.multiply(cosTheta))
            .normalize();

        return new LightSample(sampledDirection, Double.POSITIVE_INFINITY, color, intensity, 1.0, 1.0);
    }
}

