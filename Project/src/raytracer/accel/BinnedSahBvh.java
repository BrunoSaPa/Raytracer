package raytracer.accel;

import raytracer.core.Intersection;
import raytracer.core.Ray;
import raytracer.geometry.Triangle;

import java.util.List;

public class BinnedSahBvh {
    private static final int DEFAULT_BIN_COUNT = 12;
    private static final int DEFAULT_MAX_LEAF_SIZE = 4;

    private final SahBvh<Triangle> bvh;

    public BinnedSahBvh(List<Triangle> triangles) {
        this(triangles, DEFAULT_BIN_COUNT, DEFAULT_MAX_LEAF_SIZE);
    }

    public BinnedSahBvh(List<Triangle> triangles, int binCount, int maxLeafSize) {
        this.bvh = new SahBvh<>(triangles, binCount, maxLeafSize, new SahBvh.Adapter<Triangle>() {
            @Override
            public AABB bounds(Triangle primitive) {
                return primitive.getBounds();
            }

            @Override
            public raytracer.utils.Point3D centroid(Triangle primitive) {
                return primitive.getCentroid();
            }

            @Override
            public Intersection intersect(Triangle primitive, Ray ray) {
                return primitive.intersect(ray);
            }
        });
    }

    public Intersection intersect(Ray ray, double minDistance, double maxDistance) {
        return bvh.intersect(ray, minDistance, maxDistance);
    }
}
