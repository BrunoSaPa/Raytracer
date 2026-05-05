package raytracer.lighting;

import raytracer.utils.Color;
import raytracer.utils.Vector3D;

public class LightSample {
    private final Vector3D directionToLight;
    private final double maxDistance;
    private final Color color;
    private final double intensity;
    private final double attenuation;
    private final double spotFactor;

    public LightSample(
        Vector3D directionToLight,
        double maxDistance,
        Color color,
        double intensity,
        double attenuation,
        double spotFactor
    ) {
        this.directionToLight = directionToLight.normalize();
        this.maxDistance = maxDistance;
        this.color = color;
        this.intensity = intensity;
        this.attenuation = attenuation;
        this.spotFactor = spotFactor;
    }

    public Vector3D getDirectionToLight() {
        return directionToLight;
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public Color getColor() {
        return color;
    }

    public double getIntensity() {
        return intensity;
    }

    public double getAttenuation() {
        return attenuation;
    }

    public double getSpotFactor() {
        return spotFactor;
    }

    public double getRadianceScale() {
        return intensity * attenuation * spotFactor;
    }
}

