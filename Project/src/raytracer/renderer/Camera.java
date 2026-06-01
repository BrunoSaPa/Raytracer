package raytracer.renderer;

import raytracer.core.Ray;
import raytracer.utils.Point3D;
import raytracer.utils.Vector3D;

public class Camera {
    private Point3D position;//a location in 3D space
    private Vector3D forward;//direction vectors
    private Vector3D up;
    private Vector3D right;
    private double fov;
    private double nearPlane;
    private double farPlane;
    private int imageWidth;
    private int imageHeight;
    private double apertureRadius;
    private double focusDistance;

    public Camera(Point3D position, Point3D lookAt, Vector3D worldUp, double fov, double nearPlane, double farPlane, int imageWidth, int imageHeight) {
        this(position, lookAt, worldUp, fov, nearPlane, farPlane, imageWidth, imageHeight, 0.0, lookAt.subtract(position).length());
    }

    public Camera(
        Point3D position,
        Point3D lookAt,
        Vector3D worldUp,
        double fov,
        double nearPlane,
        double farPlane,
        int imageWidth,
        int imageHeight,
        double apertureRadius,
        double focusDistance
    ) {
        this.position = position;
        this.fov = fov;
        this.nearPlane = nearPlane;
        this.farPlane = farPlane;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.apertureRadius = Math.max(0.0, apertureRadius);
        double defaultFocusDistance = lookAt.subtract(position).length();
        this.focusDistance = focusDistance > 0.0 ? focusDistance : defaultFocusDistance;

        // Build camera coordinate system
        //lookAt - position: Point - Point = Vector (direction towards target)
        this.forward = lookAt.subtract(position).normalize();
        this.right = forward.cross(worldUp).normalize();
        this.up = right.cross(forward).normalize();
    }

    public Ray generateRay(int pixelX, int pixelY) {
        return generateRay(pixelX, pixelY, 0, 1);
    }

    public Ray generateRay(int pixelX, int pixelY, int sampleIndex, int sampleCount) {
        Vector3D pinholeDirection = computePinholeDirection(pixelX, pixelY);
        if (apertureRadius <= 0.0 || sampleCount <= 1) {
            return new Ray(position, pinholeDirection);
        }

        Point3D focusPoint = position.add(pinholeDirection.multiply(focusDistance));
        int scramble = hashPixel(pixelX, pixelY);
        double u = stratifiedU(sampleIndex, sampleCount);
        double v = radicalInverse(sampleIndex ^ scramble);
        double[] lensSample = concentricSampleDisk(u, v);

        Vector3D lensOffset = right.multiply(lensSample[0] * apertureRadius).add(up.multiply(lensSample[1] * apertureRadius));
        Point3D lensOrigin = position.add(lensOffset);
        Vector3D lensDirection = focusPoint.subtract(lensOrigin).normalize();
        return new Ray(lensOrigin, lensDirection);
    }

    private Vector3D computePinholeDirection(int pixelX, int pixelY) {
        double aspectRatio = (double) imageWidth / imageHeight;
        double scale = Math.tan(Math.toRadians(fov / 2.0));

        //map pixel to normalized coordinates [-1, 1]
        double cX = (2.0 * (pixelX + 0.5) / imageWidth - 1.0) * aspectRatio * scale;
        double cY = (1.0 - 2.0 * (pixelY + 0.5) / imageHeight) * scale;

        return forward.add(right.multiply(cX)).add(up.multiply(cY)).normalize();
    }

    private static double stratifiedU(int sampleIndex, int sampleCount) {
        return (sampleIndex + 0.5) / Math.max(1, sampleCount);
    }

    private static double radicalInverse(int sampleIndex) {
        int reversed = Integer.reverse(sampleIndex);
        return (reversed & 0xffffffffL) / 4294967296.0;
    }

    //hashin a pixel ensures near pixels dont use the same lens offsets
    private static int hashPixel(int x, int y) {
        int h = x * 0x1f1f1f1f ^ y * 0x9e3779b9;
        h ^= (h >>> 16);
        h *= 0x7feb352d;
        h ^= (h >>> 15);
        h *= 0x846ca68b;
        h ^= (h >>> 16);
        return h;
    }

    // using Peter Shirley's Concentric Disk Mapping
    private static double[] concentricSampleDisk(double u1, double u2) {
        double sx = 2.0 * u1 - 1.0;
        double sy = 2.0 * u2 - 1.0;
        if (sx == 0.0 && sy == 0.0) {
            return new double[]{0.0, 0.0};
        }

        double r;
        double theta;
        if (Math.abs(sx) > Math.abs(sy)) {
            r = sx;
            theta = (Math.PI / 4.0) * (sy / sx);
        } else {
            r = sy;
            theta = (Math.PI / 2.0) - (Math.PI / 4.0) * (sx / sy);
        }
        return new double[]{r * Math.cos(theta), r * Math.sin(theta)};
    }

    public double getNearPlane() { return nearPlane; }
    public double getFarPlane() { return farPlane; }
    public Point3D getPosition() { return position; }
}
