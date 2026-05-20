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

    public SceneLoadResult(
        Scene scene,
        Camera camera,
        int width,
        int height,
        Color backgroundColor,
        String outputPath,
        int threadCount,
        int tileSize
    ) {
        this.scene = scene;
        this.camera = camera;
        this.width = width;
        this.height = height;
        this.backgroundColor = backgroundColor;
        this.outputPath = outputPath;
        this.threadCount = threadCount;
        this.tileSize = tileSize;
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
}

