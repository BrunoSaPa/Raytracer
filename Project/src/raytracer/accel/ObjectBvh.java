package raytracer.accel;

import raytracer.core.Intersection;
import raytracer.core.Ray;
import raytracer.geometry.Object3D;
import raytracer.utils.Point3D;

import java.util.List;

public class ObjectBvh {
    private static final int DEFAULT_BIN_COUNT = 12;
    private static final int DEFAULT_MAX_LEAF_SIZE = 4;

    private final SahBvh<Object3D> bvh;

    public ObjectBvh(List<Object3D> objects) {
        this(objects, DEFAULT_BIN_COUNT, DEFAULT_MAX_LEAF_SIZE);
    }

    public ObjectBvh(List<Object3D> objects, int binCount, int maxLeafSize) {
        this.bvh = new SahBvh<>(objects, binCount, maxLeafSize, new SahBvh.Adapter<Object3D>() {
            @Override
            public AABB bounds(Object3D primitive) {
                return primitive.getBounds();
            }

            @Override
            public Point3D centroid(Object3D primitive) {
                return primitive.getBounds().centroid();
            }

            @Override
            public Intersection intersect(Object3D primitive, Ray ray) {
                return primitive.intersect(ray);
            }
        });
    }

    public Intersection intersect(Ray ray, double minDistance, double maxDistance) {
        return bvh.intersect(ray, minDistance, maxDistance);
    }
}
