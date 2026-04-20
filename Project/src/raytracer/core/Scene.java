package raytracer.core;

import raytracer.geometry.Object3D;
import java.util.ArrayList;
import java.util.List;

public class Scene {
    private List<Object3D> objects;

    public Scene() {
        this.objects = new ArrayList<>();
    }

    public void addObject(Object3D object) {
        objects.add(object);
    }

    public List<Object3D> getObjects() {
        return objects;
    }
}
