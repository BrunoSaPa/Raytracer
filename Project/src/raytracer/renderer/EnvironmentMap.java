package raytracer.renderer;

import raytracer.io.HdriReader;
import raytracer.utils.Color;
import raytracer.utils.Vector3D;

import java.io.IOException;

public final class EnvironmentMap {
	private final int width;
	private final int height;
	private final float[] rgb;
	private final double exposure;

	private EnvironmentMap(int width, int height, float[] rgb, double exposure) {
		this.width = width;
		this.height = height;
		this.rgb = rgb;
		this.exposure = exposure;
	}

	public static EnvironmentMap loadHdr(String path, double exposure) throws IOException {
		HdriReader.HdriImage image = HdriReader.load(path);
		return new EnvironmentMap(image.getWidth(), image.getHeight(), image.getRgb(), exposure);
	}

	public Color sampleDirection(Vector3D direction) {
		if (width <= 0 || height <= 0 || rgb.length == 0) {
			return Color.BLACK;
		}

		Vector3D dir = direction.normalize();
		//spherical trigonometry to get direction into 3d cords 0-1
		double u = 0.5 + Math.atan2(dir.z, dir.x) / (2.0 * Math.PI);
		double v = 0.5 - Math.asin(clamp(dir.y, -1.0, 1.0)) / Math.PI;

		double wrappedU = wrap01(u);
		double clampedV = clamp(v, 0.0, 1.0);

		double x = wrappedU * (width - 1);
		double y = clampedV * (height - 1);

		int x0 = (int) Math.floor(x);
		int y0 = (int) Math.floor(y);
		int x1 = (x0 + 1) % width;
		int y1 = Math.min(y0 + 1, height - 1);

		double tx = x - x0;
		double ty = y - y0;

		//get closest pixels
		Color c00 = getPixel(x0, y0);
		Color c10 = getPixel(x1, y0);
		Color c01 = getPixel(x0, y1);
		Color c11 = getPixel(x1, y1);

		//bilinear interpolation
		Color top = c00.multiply(1.0 - tx).add(c10.multiply(tx));
		Color bottom = c01.multiply(1.0 - tx).add(c11.multiply(tx));
		return top.multiply(1.0 - ty).add(bottom.multiply(ty)).multiply(exposure);
	}

	private Color getPixel(int x, int y) {
		int index = (y * width + x) * 3;
		return new Color(rgb[index], rgb[index + 1], rgb[index + 2]);
	}

	private double wrap01(double value) {
		double wrapped = value - Math.floor(value);
		return wrapped < 0.0 ? wrapped + 1.0 : wrapped;
	}

	private double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}

