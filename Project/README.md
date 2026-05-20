# Java Raytracer

This project is a CPU raytracer in Java with OBJ mesh loading, smooth/flat shading support, hard shadows, Blinn-Phong specular, multithreaded tiled rendering, and two-level BVH acceleration.

## What it supports so far

- Ray object intersection for `Sphere`, `Triangle`, and `MeshObject3D`.
- OBJ loading (`v`, `vt`, `vn`, `f`) with smoothing groups and fan triangulation.
- Triangle culling modes: `none`, `back`, `front`.
- Shading:
  - Lambert diffuse.
  - Blinn-Phong specular (`specularStrength`, `shininess`, `specularColor`).
  - Ambient term.
  - Hard shadows.
- Normal sources at hit point (priority order):
  - OBJ `vn` interpolation.
  - Smoothing-group interpolated normals.
  - Flat face normal fallback.
- Lights:
  - `DirectionalLight`
  - `PointLight` (inverse-square attenuation)
  - `SpotLight` (cutoff cone + inverse-square attenuation)
- Mesh transforms after loading:
  - `translate`
  - `scaleUniform`
  - `fitToMaxDimension`
  - rotation around `X`, `Y`, `Z` (pivot or centroid)
- Acceleration:
  - Mesh-level binned SAH BVH (`BinnedSahBvh`) for triangles.
  - Scene-level object BVH (`ObjectBvh`) for `Object3D` instances.
  - Shared generic SAH builder/traverser (`SahBvh`).
- Rendering performance:
  - Tiled multithreading.
  - Configurable thread count and tile size.
  - Console timings for trace/render and image write.

## Build and run 
From project root:

```zsh
cd "/Users/brunosanchezpadilla/Documents/GitHub/UP/Raytracer/Project"
javac -d out $(find src -name "*.java")
java -cp out Main --scene scenes/default.scene.txt
```

The render is written to the output path defined in the scene file (default example uses `output/render.png`).

## Scene text format

Each non-empty line is one command. Lines starting with `#` are comments.

### Camera

```txt
camera position x y z
camera lookAt x y z
camera up x y z
camera fov degrees
camera near value
camera far value
```

### Render settings

```txt
render width int
render height int
render background r g b
render output path
render threads int
render tile int
```

### Lights

```txt
light directional dx dy dz r g b intensity
light point px py pz r g b intensity
light spot px py pz dx dy dz cutoffDeg r g b intensity
```

### Meshes

```txt
mesh path r g b culling [options]
```

- `culling`: `none`, `back`, `front`
- optional `key=value` mesh options:
  - `fit=double`
  - `scale=double`
  - `translate=x,y,z`
  - `rotate=x,y,z` (degrees around mesh centroid)
  - `spec=double`
  - `shininess=double`
  - `speccolor=r,g,b`

Example:

```txt
mesh mesh/bunny.obj 0 0 1 none fit=5 translate=0,0,-4 rotate=0,90,0 spec=1 shininess=64 speccolor=1,1,1
```

### Sphere (optional)

```txt
sphere cx cy cz radius r g b [options]
```

- optional sphere options: `spec=`, `shininess=`, `speccolor=`

## Current entrypoints

- Preferred: `Main --scene <sceneFile>`


