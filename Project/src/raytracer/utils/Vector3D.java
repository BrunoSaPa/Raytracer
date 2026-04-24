package raytracer.utils;

public class Vector3D extends Tuple3D {

    public Vector3D() {
        super(0, 0, 0);
    }

    public Vector3D(double x, double y, double z) {
        super(x, y, z);
    }

    //vector + vector = vector
    public Vector3D add(Vector3D other) {
        return new Vector3D(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    //vector - vector = vector
    public Vector3D subtract(Vector3D other) {
        return new Vector3D(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public Vector3D multiply(double scalar) {
        return new Vector3D(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public Vector3D normalize() {
        double len = length();
        if (len == 0) return new Vector3D(0, 0, 0);
        return this.multiply(1.0 / len);
    }

    public Vector3D cross(Vector3D other) {
        return new Vector3D(
            this.y * other.z - this.z * other.y,
            this.z * other.x - this.x * other.z,
            this.x * other.y - this.y * other.x
        );
    }
}
