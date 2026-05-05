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
}

