package raytracer.core;

import raytracer.accel.AABB;
import raytracer.accel.ObjectBvh;
import raytracer.geometry.Object3D;
import raytracer.lighting.Light;

import java.util.ArrayList;
import java.util.List;

public class Scene {
    private final List<Object3D> objects;
    private final List<Object3D> unboundedObjects;
    private final List<Light> lights;
    private ObjectBvh objectBvh;
    private boolean accelerationDirty;

    public Scene() {
        this.objects = new ArrayList<>();
        this.unboundedObjects = new ArrayList<>();
        this.lights = new ArrayList<>();
        this.accelerationDirty = true;
    }

    public void addObject(Object3D object) {
        objects.add(object);
        accelerationDirty = true;
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

    public Intersection intersect(Ray ray, double minDistance, double maxDistance) {
        rebuildAccelerationIfNeeded();

        Intersection closest = objectBvh == null ? null : objectBvh.intersect(ray, minDistance, maxDistance);
        double closestDistance = closest == null ? maxDistance : closest.getDistance();


        //objects that dont have valid bounds
        for (Object3D obj : unboundedObjects) {
            Intersection hit = obj.intersect(ray);
            if (hit == null) {
                continue;
            }

            double t = hit.getDistance();
            if (t >= minDistance && t <= closestDistance) {
                closest = hit;
                closestDistance = t;
            }
        }

        return closest;
    }

    private void rebuildAccelerationIfNeeded() {
        if (!accelerationDirty) {
            return;
        }

        unboundedObjects.clear();
        List<Object3D> boundedObjects = new ArrayList<>();
        for (Object3D object : objects) {
            AABB bounds = object.getBounds();
            if (bounds == null || !bounds.isValid()) {
                unboundedObjects.add(object);
            } else {
                boundedObjects.add(object);
            }
        }

        objectBvh = boundedObjects.isEmpty() ? null : new ObjectBvh(boundedObjects);
        accelerationDirty = false;
    }
}
