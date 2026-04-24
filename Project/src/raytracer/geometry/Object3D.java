package raytracer.geometry;

import raytracer.core.Instersection;
import raytracer.core.Ray;
import raytracer.utils.Color;

public interface Object3D {
    Instersection intersect(Ray ray);
    Color getColor();
}
