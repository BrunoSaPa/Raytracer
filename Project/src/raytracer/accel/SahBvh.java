package raytracer.accel;

import raytracer.core.Intersection;
import raytracer.core.Ray;
import raytracer.utils.Point3D;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class SahBvh<T> {
    private static final double TRAVERSAL_COST = 1.0;
    private static final double INTERSECTION_COST = 1.0;
    private static final double CENTROID_EPSILON = 1e-12;

    public interface Adapter<T> {
        AABB bounds(T primitive);

        Point3D centroid(T primitive);

        Intersection intersect(T primitive, Ray ray);
    }

    private final int binCount;
    private final int maxLeafSize;
    private final Adapter<T> adapter;
    private final List<PrimitiveRef<T>> primitives;
    private final BvhNode root;

    public SahBvh(List<T> source, int binCount, int maxLeafSize, Adapter<T> adapter) {
        this.binCount = Math.max(4, binCount);
        this.maxLeafSize = Math.max(1, maxLeafSize);
        this.adapter = adapter;
        this.primitives = new ArrayList<>(source.size());
        for (T item : source) {
            AABB bounds = adapter.bounds(item);
            if (bounds == null || !bounds.isValid()) {
                continue;
            }
            Point3D centroid = adapter.centroid(item);
            primitives.add(new PrimitiveRef<>(item, bounds, centroid));
        }
        this.root = primitives.isEmpty() ? null : buildRecursive(0, primitives.size());
    }

    public Intersection intersect(Ray ray, double minDistance, double maxDistance) {
        if (root == null || !root.getBounds().intersects(ray, minDistance, maxDistance)) {
            return null;
        }

        Intersection closest = null;
        double closestDistance = maxDistance;
        ArrayDeque<BvhNode> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            BvhNode node = stack.pop();
            if (!node.getBounds().intersects(ray, minDistance, closestDistance)) {
                continue;
            }

            if (node.isLeaf()) {
                for (int i = node.getStart(); i < node.getEnd(); i++) {
                    Intersection hit = adapter.intersect(primitives.get(i).primitive, ray);
                    if (hit == null) {
                        continue;
                    }

                    double t = hit.getDistance();
                    if (t >= minDistance && t <= closestDistance) {
                        closest = hit;
                        closestDistance = t;
                    }
                }
                continue;
            }

            BvhNode left = node.getLeft();
            BvhNode right = node.getRight();
            if (left != null) {
                stack.push(left);
            }
            if (right != null) {
                stack.push(right);
            }
        }

        return closest;
    }

    private BvhNode buildRecursive(int start, int end) {
        AABB nodeBounds = new AABB();
        AABB centroidBounds = new AABB();
        for (int i = start; i < end; i++) {
            PrimitiveRef<T> primitive = primitives.get(i);
            nodeBounds.include(primitive.bounds);
            centroidBounds.include(primitive.centroid);
        }

        int primitiveCount = end - start;
        if (primitiveCount <= maxLeafSize || centroidBounds.maxExtent() <= CENTROID_EPSILON) {
            return new BvhNode(nodeBounds, start, end, null, null);
        }

        SplitCandidate bestSplit = findBestSplit(start, end, nodeBounds, centroidBounds);
        if (bestSplit == null) {
            return new BvhNode(nodeBounds, start, end, null, null);
        }

        int mid = partition(start, end, bestSplit.axis, centroidBounds, bestSplit.splitBin);
        if (mid <= start || mid >= end) {
            return new BvhNode(nodeBounds, start, end, null, null);
        }

        BvhNode left = buildRecursive(start, mid);
        BvhNode right = buildRecursive(mid, end);
        return new BvhNode(nodeBounds, start, end, left, right);
    }

    private SplitCandidate findBestSplit(int start, int end, AABB parentBounds, AABB centroidBounds) {
        SplitCandidate best = null;
        double parentArea = parentBounds.surfaceArea();
        if (parentArea <= 0.0) {
            return null;
        }

        double leafCost = (end - start) * INTERSECTION_COST;

        for (int axis = 0; axis < 3; axis++) {
            double cMin = axisValue(centroidBounds.getMinX(), centroidBounds.getMinY(), centroidBounds.getMinZ(), axis);
            double cMax = axisValue(centroidBounds.getMaxX(), centroidBounds.getMaxY(), centroidBounds.getMaxZ(), axis);
            double extent = cMax - cMin;
            if (extent <= CENTROID_EPSILON) {
                continue;
            }

            Bin[] bins = new Bin[binCount];
            for (int i = 0; i < binCount; i++) {
                bins[i] = new Bin();
            }

            for (int i = start; i < end; i++) {
                PrimitiveRef<T> primitive = primitives.get(i);
                double centroidAxis = axisValue(primitive.centroid.x, primitive.centroid.y, primitive.centroid.z, axis);
                int binIndex = toBinIndex(centroidAxis, cMin, cMax);
                bins[binIndex].count++;
                bins[binIndex].bounds.include(primitive.bounds);
            }

            AABB[] leftBounds = new AABB[binCount - 1];
            int[] leftCounts = new int[binCount - 1];
            AABB runningLeftBounds = new AABB();
            int runningLeftCount = 0;
            for (int i = 0; i < binCount - 1; i++) {
                runningLeftBounds.include(bins[i].bounds);
                runningLeftCount += bins[i].count;
                leftBounds[i] = copyBounds(runningLeftBounds);
                leftCounts[i] = runningLeftCount;
            }

            AABB runningRightBounds = new AABB();
            int runningRightCount = 0;
            for (int i = binCount - 1; i >= 1; i--) {
                runningRightBounds.include(bins[i].bounds);
                runningRightCount += bins[i].count;

                int splitBin = i - 1;
                int leftCount = leftCounts[splitBin];
                int rightCount = runningRightCount;
                if (leftCount == 0 || rightCount == 0) {
                    continue;
                }

                double leftArea = leftBounds[splitBin].surfaceArea();
                double rightArea = runningRightBounds.surfaceArea();
                double splitCost = TRAVERSAL_COST
                    + (leftArea / parentArea) * leftCount * INTERSECTION_COST
                    + (rightArea / parentArea) * rightCount * INTERSECTION_COST;

                if (splitCost >= leafCost) {
                    continue;
                }

                if (best == null || splitCost < best.cost) {
                    best = new SplitCandidate(axis, splitBin, splitCost);
                }
            }
        }

        return best;
    }

    private int partition(int start, int end, int axis, AABB centroidBounds, int splitBin) {
        double cMin = axisValue(centroidBounds.getMinX(), centroidBounds.getMinY(), centroidBounds.getMinZ(), axis);
        double cMax = axisValue(centroidBounds.getMaxX(), centroidBounds.getMaxY(), centroidBounds.getMaxZ(), axis);

        List<PrimitiveRef<T>> temp = new ArrayList<>(end - start);
        for (int i = 0; i < (end - start); i++) {
            temp.add(null);
        }

        int leftWrite = 0;
        int rightWrite = end - start;

        for (int i = start; i < end; i++) {
            PrimitiveRef<T> primitive = primitives.get(i);
            double centroidAxis = axisValue(primitive.centroid.x, primitive.centroid.y, primitive.centroid.z, axis);
            int binIndex = toBinIndex(centroidAxis, cMin, cMax);
            if (binIndex <= splitBin) {
                temp.set(leftWrite++, primitive);
            } else {
                temp.set(--rightWrite, primitive);
            }
        }

        for (int i = 0; i < temp.size(); i++) {
            primitives.set(start + i, temp.get(i));
        }

        return start + leftWrite;
    }

    private int toBinIndex(double value, double min, double max) {
        double normalized = (value - min) / (max - min);
        int bin = (int) Math.floor(normalized * binCount);
        if (bin < 0) {
            return 0;
        }
        return Math.min(binCount - 1, bin);
    }

    private double axisValue(double x, double y, double z, int axis) {
        if (axis == 0) {
            return x;
        }
        if (axis == 1) {
            return y;
        }
        return z;
    }

    private AABB copyBounds(AABB src) {
        return new AABB(src.getMinX(), src.getMinY(), src.getMinZ(), src.getMaxX(), src.getMaxY(), src.getMaxZ());
    }

    private static class Bin {
        private final AABB bounds = new AABB();
        private int count;
    }

    private static class PrimitiveRef<T> {
        private final T primitive;
        private final AABB bounds;
        private final Point3D centroid;

        private PrimitiveRef(T primitive, AABB bounds, Point3D centroid) {
            this.primitive = primitive;
            this.bounds = bounds;
            this.centroid = centroid;
        }
    }

    private static class SplitCandidate {
        private final int axis;
        private final int splitBin;
        private final double cost;

        private SplitCandidate(int axis, int splitBin, double cost) {
            this.axis = axis;
            this.splitBin = splitBin;
            this.cost = cost;
        }
    }
}

