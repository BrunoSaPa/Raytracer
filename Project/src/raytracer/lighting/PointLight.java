package raytracer.lighting;

import raytracer.utils.Color;
import raytracer.utils.Point3D;
import raytracer.utils.Vector3D;

public class PointLight implements Light {
    private final Point3D position;
    private final Color color;
    private final double intensity;

    public PointLight(Point3D position, Color color, double intensity) {
        this.position = position;
        this.color = color;
        this.intensity = intensity;
    }

    public Point3D getPosition() {
        return position;
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
        Vector3D toLightVector = position.subtract(shadedPoint);
        double distance = toLightVector.length();

        if (distance <= 1e-8) {
            return new LightSample(new Vector3D(0, 0, 0), 0.0, color, intensity, 0.0, 1.0);
        }

        Vector3D toLight = toLightVector.normalize();

        double attenuation = 1.0 / (distance * distance);

        return new LightSample(toLight, distance, color, intensity, attenuation, 1.0);
    }

    @Override
    public int getSoftSampleCount(SoftShadowSettings settings) {
        if (settings.getSoftShadowSamples() <= 1 || settings.getPointLightRadius() <= 0.0) {
            return 1;
        }
        return settings.getSoftShadowSamples();
    }

    @Override
    public LightSample sampleSoftAt(Point3D shadedPoint, int sampleIndex, int sampleCount, SoftShadowSettings settings) {
        if (sampleCount <= 1 || settings.getPointLightRadius() <= 0.0) {
            return sampleAt(shadedPoint);
        }

        Vector3D randomUnit = SoftShadowSettings.sampleUnitSphere(
            sampleIndex,
            sampleCount,
            SoftShadowSettings.POINT_LIGHT_SCRAMBLE_SALT
        );
        Point3D sampledPosition = position.add(randomUnit.multiply(settings.getPointLightRadius()));
        Vector3D toLightVector = sampledPosition.subtract(shadedPoint);
        double distance = toLightVector.length();
        if (distance <= 1e-8) {
            return new LightSample(new Vector3D(0, 0, 0), 0.0, color, intensity, 0.0, 1.0);
        }

        Vector3D toLight = toLightVector.normalize();
        double attenuation = 1.0 / (distance * distance);
        return new LightSample(toLight, distance, color, intensity, attenuation, 1.0);
    }
}

