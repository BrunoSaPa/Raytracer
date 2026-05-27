package raytracer.utils;

public class UV {
    public final double u;
    public final double v;

    public UV(double u, double v) {
        this.u = u;
        this.v = v;
    }

    public UV multiply(double scalar) {
        return new UV(u * scalar, v * scalar);
    }

    public UV add(UV other) {
        return new UV(u + other.u, v + other.v);
    }
}

