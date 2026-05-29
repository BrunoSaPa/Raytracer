package raytracer.io.scene;

import raytracer.core.Scene;
import raytracer.renderer.Camera;
import raytracer.utils.Color;

public class SceneLoadResult {
    private final Scene scene;
    private final Camera camera;
    private final int width;
    private final int height;
    private final Color backgroundColor;
    private final String outputPath;
    private final int threadCount;
    private final int tileSize;
    private final int softShadowSamples;
    private final double pointLightRadius;
    private final double spotLightRadius;
    private final double directionalLightAngleDegrees;

    public SceneLoadResult(
        Scene scene,
        Camera camera,
        int width,
        int height,
        Color backgroundColor,
        String outputPath,
        int threadCount,
        int tileSize,
        int softShadowSamples,
        double pointLightRadius,
        double spotLightRadius,
        double directionalLightAngleDegrees
    ) {
        this.scene = scene;
        this.camera = camera;
        this.width = width;
        this.height = height;
        this.backgroundColor = backgroundColor;
        this.outputPath = outputPath;
        this.threadCount = threadCount;
        this.tileSize = tileSize;
        this.softShadowSamples = softShadowSamples;
        this.pointLightRadius = pointLightRadius;
        this.spotLightRadius = spotLightRadius;
        this.directionalLightAngleDegrees = directionalLightAngleDegrees;
    }

    public Scene getScene() {
        return scene;
    }

    public Camera getCamera() {
        return camera;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public int getTileSize() {
        return tileSize;
    }

    public int getSoftShadowSamples() {
        return softShadowSamples;
    }

    public double getPointLightRadius() {
        return pointLightRadius;
    }

    public double getSpotLightRadius() {
        return spotLightRadius;
    }

    public double getDirectionalLightAngleDegrees() {
        return directionalLightAngleDegrees;
    }
}

