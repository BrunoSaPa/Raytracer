package raytracer.utils;


// PD; i know this class will be better in another package, maybe utils, but for simplicity ill put it here for now :)
public class Color {
    private double r;
    private double g;
    private double b;

    public static final Color BLACK = new Color(0, 0, 0);
    public static final Color RED = new Color(1, 0, 0);
    public static final Color BLUE = new Color(0, 0, 1);

    public Color(double r, double g, double b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public double getR() { return r; }
    public double getG() { return g; }
    public double getB() { return b; }

    public Color add(Color other) {
        return new Color(this.r + other.r, this.g + other.g, this.b + other.b);
    }

    public Color multiply(double scalar) {
        return new Color(this.r * scalar, this.g * scalar, this.b * scalar);
    }

    public Color multiply(Color other) {
        return new Color(this.r * other.r, this.g * other.g, this.b * other.b);
    }

    private int clamp(double value) {
        return (int) Math.max(0, Math.min(255, value * 255));
    }

//converts rgb ints to something renderimage can understand
    public int toRGB() {
        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }
}

