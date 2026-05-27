package raytracer.core;

import raytracer.geometry.Object3D;
import raytracer.utils.Point3D;
import raytracer.utils.Vector3D;

public class Intersection {
    private double distance;
    private Point3D point;   // a hit location in 3D space
    private Object3D object;
    private Vector3D normal;
    private double baryU;
    private double baryV;
    private double baryW;
    private double texU;
    private double texV;
    private Vector3D tangent;
    private Vector3D bitangent;


    public Intersection(double distance, Point3D point, Object3D object, Vector3D normal) {
        this(
            distance,
            point,
            object,
            normal,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            null,
            null
        );
    }

    public Intersection(double distance, Point3D point, Object3D object, Vector3D normal, double baryU, double baryV, double baryW) {
        this(distance, point, object, normal, baryU, baryV, baryW, Double.NaN, Double.NaN, null, null);
    }

    public Intersection(
        double distance,
        Point3D point,
        Object3D object,
        Vector3D normal,
        double baryU,
        double baryV,
        double baryW,
        double texU,
        double texV
    ) {
        this(distance, point, object, normal, baryU, baryV, baryW, texU, texV, null, null);
    }

    public Intersection(
        double distance,
        Point3D point,
        Object3D object,
        Vector3D normal,
        double baryU,
        double baryV,
        double baryW,
        double texU,
        double texV,
        Vector3D tangent,
        Vector3D bitangent
    ) {
        this.distance = distance;
        this.point = point;
        this.object = object;
        this.normal = normal;
        this.baryU = baryU;
        this.baryV = baryV;
        this.baryW = baryW;
        this.texU = texU;
        this.texV = texV;
        this.tangent = tangent;
        this.bitangent = bitangent;
    }

    public double getDistance() { return distance; }
    public Point3D getPoint() { return point; }
    public Object3D getObject() { return object; }
    public Vector3D getNormal() { return normal; }
    public double getBaryU() { return baryU; }
    public double getBaryV() { return baryV; }
    public double getBaryW() { return baryW; }
    public double getTexU() { return texU; }
    public double getTexV() { return texV; }
    public Vector3D getTangent() { return tangent; }
    public Vector3D getBitangent() { return bitangent; }

    public boolean hasBarycentric() {
        return !(Double.isNaN(baryU) || Double.isNaN(baryV) || Double.isNaN(baryW));
    }

    public boolean hasTexCoords() {
        return !(Double.isNaN(texU) || Double.isNaN(texV));
    }

    public boolean hasTangentBasis() {
        return tangent != null && bitangent != null;
    }
}
