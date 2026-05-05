package raytracer.lighting;

import raytracer.utils.Color;
import raytracer.utils.Point3D;
import raytracer.utils.Vector3D;

public class SpotLight implements Light {
    private final Point3D position;
    private final Vector3D direction;
    private final double cutoffCos;
    private final Color color;
    private final double intensity;


    public SpotLight(Point3D position, Vector3D direction, double cutoffDegrees, Color color, double intensity) {
        this.position = position;
        this.direction = direction.normalize();
        this.cutoffCos = Math.cos(Math.toRadians(cutoffDegrees));
        this.color = color;
        this.intensity = intensity;
    }

    public Point3D getPosition() {
        return position;
    }

    public Vector3D getDirection() {
        return direction;
    }

    public double getCutoffCos() {
        return cutoffCos;
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
            return new LightSample(new Vector3D(0, 0, 0), 0.0, color, intensity, 0.0, 0.0);
        }

        Vector3D toLight = toLightVector.normalize();

        //for spot evaluation, compare spotlight axis with direction from light to shaded point
        Vector3D lightToPoint = shadedPoint.subtract(position).normalize();
        double cosTheta = direction.dot(lightToPoint);

        double spotFactor = cosTheta >= cutoffCos ? 1.0 : 0.0;
        double attenuation = 1.0;

        return new LightSample(toLight, distance, color, intensity, attenuation, spotFactor);
    }
}

