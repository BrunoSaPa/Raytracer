package raytracer.accel;

import raytracer.core.Ray;
import raytracer.utils.Point3D;

public class AABB {
    private static final double EPSILON = 1e-12;

    private double minX;
    private double minY;
    private double minZ;
    private double maxX;
    private double maxY;
    private double maxZ;

    public AABB() {
        reset();
    }

    public AABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public static AABB fromPoint(Point3D p) {
        return new AABB(p.x, p.y, p.z, p.x, p.y, p.z);
    }

    public void reset() {
        minX = Double.POSITIVE_INFINITY;
        minY = Double.POSITIVE_INFINITY;
        minZ = Double.POSITIVE_INFINITY;
        maxX = Double.NEGATIVE_INFINITY;
        maxY = Double.NEGATIVE_INFINITY;
        maxZ = Double.NEGATIVE_INFINITY;
    }

    public boolean isValid() {
        return minX <= maxX && minY <= maxY && minZ <= maxZ;
    }

    public void include(Point3D p) {
        minX = Math.min(minX, p.x);
        minY = Math.min(minY, p.y);
        minZ = Math.min(minZ, p.z);
        maxX = Math.max(maxX, p.x);
        maxY = Math.max(maxY, p.y);
        maxZ = Math.max(maxZ, p.z);
    }

    public void include(AABB other) {
        if (other == null || !other.isValid()) {
            return;
        }
        minX = Math.min(minX, other.minX);
        minY = Math.min(minY, other.minY);
        minZ = Math.min(minZ, other.minZ);
        maxX = Math.max(maxX, other.maxX);
        maxY = Math.max(maxY, other.maxY);
        maxZ = Math.max(maxZ, other.maxZ);
    }

    public double extentX() {
        return maxX - minX;
    }

    public double extentY() {
        return maxY - minY;
    }

    public double extentZ() {
        return maxZ - minZ;
    }

    public double maxExtent() {
        return Math.max(extentX(), Math.max(extentY(), extentZ()));
    }

    public Point3D centroid() {
        return new Point3D((minX + maxX) * 0.5, (minY + maxY) * 0.5, (minZ + maxZ) * 0.5);
    }

    public double surfaceArea() {
        if (!isValid()) {
            return 0.0;
        }
        double dx = extentX();
        double dy = extentY();
        double dz = extentZ();
        return 2.0 * (dx * dy + dy * dz + dz * dx);
    }

    public boolean intersects(Ray ray, double tMin, double tMax) {
        double ox = ray.getOrigin().x;
        double oy = ray.getOrigin().y;
        double oz = ray.getOrigin().z;
        double dx = ray.getDirection().x;
        double dy = ray.getDirection().y;
        double dz = ray.getDirection().z;

        double[] tx = axisInterval(ox, dx, minX, maxX, tMin, tMax);
        if (tx == null) {
            return false;
        }

        double[] ty = axisInterval(oy, dy, minY, maxY, tx[0], tx[1]);
        if (ty == null) {
            return false;
        }

        double[] tz = axisInterval(oz, dz, minZ, maxZ, ty[0], ty[1]);
        return tz != null;
    }

    private double[] axisInterval(double origin, double direction, double min, double max, double tMin, double tMax) {
        if (Math.abs(direction) < EPSILON) {
            if (origin < min || origin > max) {
                return null;
            }
            return new double[]{tMin, tMax};
        }

        double invDir = 1.0 / direction;
        double t0 = (min - origin) * invDir;
        double t1 = (max - origin) * invDir;
        if (t0 > t1) {
            double tmp = t0;
            t0 = t1;
            t1 = tmp;
        }

        double entry = Math.max(tMin, t0);
        double exit = Math.min(tMax, t1);
        if (exit < entry) {
            return null;
        }
        return new double[]{entry, exit};
    }

    public double getMinX() { return minX; }
    public double getMinY() { return minY; }
    public double getMinZ() { return minZ; }
    public double getMaxX() { return maxX; }
    public double getMaxY() { return maxY; }
    public double getMaxZ() { return maxZ; }
}

