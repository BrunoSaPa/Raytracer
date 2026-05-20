package raytracer.accel;

public class BvhNode {
    private final AABB bounds;
    private final int start;
    private final int end;
    private final BvhNode left;
    private final BvhNode right;

    public BvhNode(AABB bounds, int start, int end, BvhNode left, BvhNode right) {
        this.bounds = bounds;
        this.start = start;
        this.end = end;
        this.left = left;
        this.right = right;
    }

    public AABB getBounds() {
        return bounds;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public BvhNode getLeft() {
        return left;
    }

    public BvhNode getRight() {
        return right;
    }

    public boolean isLeaf() {
        return left == null && right == null;
    }
}

