package raytracer.geometry;

import raytracer.core.Intersection;
import raytracer.core.Ray;
import raytracer.utils.Color;

public interface Object3D {
    Intersection intersect(Ray ray);
    Color getColor();
}
