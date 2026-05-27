package raytracer.geometry;

import raytracer.accel.AABB;
import raytracer.core.Intersection;
import raytracer.core.Ray;
import raytracer.material.Material;
import raytracer.utils.Color;

public interface Object3D {
    Intersection intersect(Ray ray);
    Color getColor();

    default AABB getBounds() {
        return null;
    }

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

    default Material getMaterial() {
        return Material.fromLegacy(getColor(), getSpecularStrength(), getShininess(), getSpecularColor());
    }
}
