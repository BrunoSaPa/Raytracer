package raytracer.lighting;

import raytracer.utils.Color;
import raytracer.utils.Point3D;

public interface Light {
    Color getColor();
    double getIntensity();
    LightSample sampleAt(Point3D shadedPoint);
}

