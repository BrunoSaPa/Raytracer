package raytracer.geometry;

import raytracer.core.Instersection;
import raytracer.core.Ray;
import raytracer.math.Color;

public interface Object3D {
    Instersection intersect(Ray ray);
    Color getColor();
}
