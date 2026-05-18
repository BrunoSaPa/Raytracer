package raytracer.geometry;

import raytracer.core.Intersection;
import raytracer.core.Ray;
import raytracer.utils.Color;

public interface Object3D {
    Intersection intersect(Ray ray);
    Color getColor();

    //falback values
    default double getSpecularStrength() {
        return 0.0;
    }

    default double getShininess() {
        return 0.0;
    }

    default Color getSpecularColor() {
        return Color.WHITE;
    }
}
