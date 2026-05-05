package raytracer.core;

import raytracer.geometry.Object3D;
import raytracer.lighting.Light;
import java.util.ArrayList;
import java.util.List;

public class Scene {
    private List<Object3D> objects;
    private List<Light> lights;

    public Scene() {
        this.objects = new ArrayList<>();
        this.lights = new ArrayList<>();
    }

    public void addObject(Object3D object) {
        objects.add(object);
    }

    public List<Object3D> getObjects() {
        return objects;
    }

    public void addLight(Light light) {
        lights.add(light);
    }

    public List<Light> getLights() {
        return lights;
    }
}
