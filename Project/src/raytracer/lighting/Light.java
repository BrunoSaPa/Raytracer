package raytracer.lighting;

import raytracer.utils.Color;
import raytracer.utils.Point3D;

public interface Light {
    Color getColor();
    double getIntensity();
    LightSample sampleAt(Point3D shadedPoint);

    default int getSoftSampleCount(SoftShadowSettings settings) {
        return 1;
    }

    default LightSample sampleSoftAt(Point3D shadedPoint, int sampleIndex, int sampleCount, SoftShadowSettings settings) {
        return sampleAt(shadedPoint);
    }
}

