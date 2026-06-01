import raytracer.io.scene.SceneFileLoader;
import raytracer.io.scene.SceneLoadResult;
import raytracer.renderer.Raytracer;

public class Main {
    public static void main(String[] args) {
        String scenePath;
        if (args.length == 2 && "--scene".equals(args[0])) {
            scenePath = args[1];
        } else if (args.length == 1) {
            scenePath = args[0];
        } else {
            scenePath = "scenes/default.scene.txt";
            System.out.println("No scene argument provided. Using default: " + scenePath);
        }

        try {
            SceneLoadResult loaded = SceneFileLoader.load(scenePath);
            Raytracer raytracer = new Raytracer(
                loaded.getScene(),
                loaded.getCamera(),
                loaded.getWidth(),
                loaded.getHeight(),
                loaded.getBackgroundColor(),
                loaded.getThreadCount(),
                loaded.getTileSize(),
                loaded.getSoftShadowSamples(),
                loaded.getPointLightRadius(),
                loaded.getSpotLightRadius(),
                loaded.getDirectionalLightAngleDegrees(),
                loaded.getMaxRayDepth()
            );
            raytracer.render(loaded.getOutputPath());
        } catch (Exception e) {
            System.err.println("Failed to load scene file: " + e.getMessage());
        }
    }
}
