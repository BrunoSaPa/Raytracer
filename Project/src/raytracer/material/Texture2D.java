package raytracer.material;

import raytracer.utils.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Texture2D {
    private final BufferedImage image;

    private Texture2D(BufferedImage image) {
        this.image = image;
    }

    public static Texture2D load(String path) throws IOException {
        BufferedImage image = ImageIO.read(new File(path));
        if (image == null) {
            throw new IOException("Failed to load texture image: " + path);
        }
        return new Texture2D(image);
    }

    public Color sample(double u, double v) {
        if (image.getWidth() <= 0 || image.getHeight() <= 0) {
            return Color.WHITE;
        }

        double wrappedU = wrap01(u);
        double wrappedV = wrap01(v);
        int x = (int) (wrappedU * (image.getWidth() - 1));
        int y = (int) ((1.0 - wrappedV) * (image.getHeight() - 1));

        //i coulve done bilinear, but nearest pixel is ok too ;)
        int rgb = image.getRGB(x, y);
        double r = ((rgb >> 16) & 0xFF) / 255.0;
        double g = ((rgb >> 8) & 0xFF) / 255.0;
        double b = (rgb & 0xFF) / 255.0;
        return new Color(r, g, b);
    }

    private double wrap01(double value) {
        double wrapped = value - Math.floor(value);
        return wrapped < 0.0 ? wrapped + 1.0 : wrapped;
    }
}

