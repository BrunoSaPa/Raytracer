package raytracer.geometry;

import raytracer.accel.AABB;
import raytracer.core.Intersection;
import raytracer.core.Ray;
import raytracer.material.Material;
import raytracer.utils.Color;
import raytracer.utils.Point3D;
import raytracer.utils.UV;
import raytracer.utils.Vector3D;

public class Triangle implements Object3D {
    private static final double EPSILON = 1e-8;

    private Point3D v0;
    private Point3D v1;
    private Point3D v2;
    private Color color;
    private TriangleCullingMode cullingMode;
    private int smoothingGroupId;
    private Vector3D normal0;
    private Vector3D normal1;
    private Vector3D normal2;
    private UV uv0;
    private UV uv1;
    private UV uv2;
    private double specularStrength;
    private double shininess;
    private Color specularColor;
    private Material material;

    public Triangle(Point3D v0, Point3D v1, Point3D v2, Color color) {
        this(v0, v1, v2, color, TriangleCullingMode.NONE);
    }

    public Triangle(Point3D v0, Point3D v1, Point3D v2, Color color, TriangleCullingMode cullingMode) {
        this(v0, v1, v2, color, cullingMode, 0, null, null, null);
    }

    public Triangle(
        Point3D v0,
        Point3D v1,
        Point3D v2,
        Color color,
        TriangleCullingMode cullingMode,
        int smoothingGroupId
    ) {
        this(v0, v1, v2, color, cullingMode, smoothingGroupId, null, null, null);
    }

    public Triangle(
        Point3D v0,
        Point3D v1,
        Point3D v2,
        Color color,
        TriangleCullingMode cullingMode,
        int smoothingGroupId,
        Vector3D normal0,
        Vector3D normal1,
        Vector3D normal2
    ) {
        this(v0, v1, v2, color, cullingMode, smoothingGroupId, normal0, normal1, normal2, null, null, null);
    }

    public Triangle(
        Point3D v0,
        Point3D v1,
        Point3D v2,
        Color color,
        TriangleCullingMode cullingMode,
        int smoothingGroupId,
        Vector3D normal0,
        Vector3D normal1,
        Vector3D normal2,
        UV uv0,
        UV uv1,
        UV uv2
    ) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.color = color;
        this.cullingMode = cullingMode;
        this.smoothingGroupId = smoothingGroupId;
        this.normal0 = normal0 == null ? null : normal0.normalize();
        this.normal1 = normal1 == null ? null : normal1.normalize();
        this.normal2 = normal2 == null ? null : normal2.normalize();
        this.uv0 = uv0;
        this.uv1 = uv1;
        this.uv2 = uv2;
        this.specularStrength = 0.0;
        this.shininess = 32.0;
        this.specularColor = Color.WHITE;
        this.material = Material.fromLegacy(this.color, this.specularStrength, this.shininess, this.specularColor);
    }

    public Point3D getV0() { return v0; }
    public Point3D getV1() { return v1; }
    public Point3D getV2() { return v2; }
    public int getSmoothingGroupId() { return smoothingGroupId; }
    public Vector3D getNormal0() { return normal0; }
    public Vector3D getNormal1() { return normal1; }
    public Vector3D getNormal2() { return normal2; }
    public UV getUv0() { return uv0; }
    public UV getUv1() { return uv1; }
    public UV getUv2() { return uv2; }
    public boolean hasProvidedVertexNormals() { return normal0 != null && normal1 != null && normal2 != null; }
    public boolean hasUVs() { return uv0 != null && uv1 != null && uv2 != null; }
    public TriangleCullingMode getCullingMode() { return cullingMode; }

    public void setCullingMode(TriangleCullingMode cullingMode) {
        this.cullingMode = cullingMode;
    }

    @Override
    public Color getColor() {
        return material.getBaseColor();
    }

    @Override
    public double getSpecularStrength() {
        return material.getSpecularStrength();
    }

    @Override
    public double getShininess() {
        return material.getShininess();
    }

    @Override
    public Color getSpecularColor() {
        return material.getSpecularColor();
    }

    @Override
    public Material getMaterial() {
        return material;
    }

    public void setSpecularStrength(double specularStrength) {
        this.specularStrength = Math.max(0.0, specularStrength);
        refreshMaterial();
    }

    public void setShininess(double shininess) {
        this.shininess = Math.max(1.0, shininess);
        refreshMaterial();
    }

    public void setSpecularColor(Color specularColor) {
        this.specularColor = specularColor == null ? Color.WHITE : specularColor;
        refreshMaterial();
    }

    public void setMaterial(Material material) {
        if (material == null) {
            return;
        }
        this.material = material;
        this.color = material.getBaseColor();
        this.specularStrength = material.getSpecularStrength();
        this.shininess = material.getShininess();
        this.specularColor = material.getSpecularColor();
    }

    private void refreshMaterial() {
        Color baseColor = material != null ? material.getBaseColor() : this.color;
        double roughness = material != null ? material.getRoughness() : 0.0;
        double normalStrength = material != null ? material.getNormalStrength() : 1.0;
        this.material = new Material(
            baseColor,
            this.specularStrength,
            this.shininess,
            this.specularColor,
            roughness,
            normalStrength,
            material != null ? material.getAlbedoTexture() : null,
            material != null ? material.getNormalTexture() : null,
            material != null ? material.getRoughnessTexture() : null,
            material != null ? material.getBumpTexture() : null,
            material != null ? material.getBumpStrength() : 1.0
        );
    }

    public Vector3D getNormal() {
        Vector3D edge1 = v1.subtract(v0);
        Vector3D edge2 = v2.subtract(v0);
        return edge1.cross(edge2).normalize();
    }

    public Point3D getCentroid() {
        return new Point3D(
            (v0.x + v1.x + v2.x) / 3.0,
            (v0.y + v1.y + v2.y) / 3.0,
            (v0.z + v1.z + v2.z) / 3.0
        );
    }

    @Override
    public AABB getBounds() {
        AABB bounds = new AABB();
        bounds.include(v0);
        bounds.include(v1);
        bounds.include(v2);
        return bounds;
    }

    @Override
    public Intersection intersect(Ray ray) {
        Vector3D edge1 = v1.subtract(v0);
        Vector3D edge2 = v2.subtract(v0);

        Vector3D pvec = ray.getDirection().cross(edge2);
        double det = edge1.dot(pvec);

        // det sign tells if ray hits front or back side relative to triangle normal/winding
        if (cullingMode == TriangleCullingMode.BACK_FACE) {
            if (det <= EPSILON) return null;
        } else if (cullingMode == TriangleCullingMode.FRONT_FACE) {
            if (det >= -EPSILON) return null;
        } else {
            if (Math.abs(det) <= EPSILON) return null;
        }

        double invDet = 1.0 / det;

        Vector3D tvec = ray.getOrigin().subtract(v0);
        double u = tvec.dot(pvec) * invDet;
        if (u < 0.0 || u > 1.0) return null;

        Vector3D qvec = tvec.cross(edge1);
        double v = ray.getDirection().dot(qvec) * invDet;
        if (v < 0.0 || (u + v) > 1.0) return null;

        double t = edge2.dot(qvec) * invDet;
        if (t <= EPSILON) return null;

        double w = 1.0 - u - v;
        Point3D hitPoint = ray.getPoint(t);
        Vector3D normal = getNormal();
        double texU = Double.NaN;
        double texV = Double.NaN;
        Vector3D tangent = null;
        Vector3D bitangent = null;
        if (hasUVs()) {
            texU = uv0.u * w + uv1.u * u + uv2.u * v;
            texV = uv0.v * w + uv1.v * u + uv2.v * v;
            Vector3D[] basis = computeTangentBasis(edge1, edge2);
            tangent = basis[0];
            bitangent = basis[1];
        }

        //just saving barycentric coords just so they can be used in the future
        return new Intersection(t, hitPoint, this, normal, u, v, w, texU, texV, tangent, bitangent);
    }

    private Vector3D[] computeTangentBasis(Vector3D edge1, Vector3D edge2) {
        if (!hasUVs()) {
            return new Vector3D[]{null, null};
        }

        double du1 = uv1.u - uv0.u;
        double dv1 = uv1.v - uv0.v;
        double du2 = uv2.u - uv0.u;
        double dv2 = uv2.v - uv0.v;

        double determinant = du1 * dv2 - du2 * dv1;
        if (Math.abs(determinant) <= EPSILON) {
            return new Vector3D[]{null, null};
        }

        double inv = 1.0 / determinant;
        Vector3D tangent = edge1.multiply(dv2).subtract(edge2.multiply(dv1)).multiply(inv).normalize();
        Vector3D bitangent = edge2.multiply(du1).subtract(edge1.multiply(du2)).multiply(inv).normalize();

        return new Vector3D[]{tangent, bitangent};
    }
}
