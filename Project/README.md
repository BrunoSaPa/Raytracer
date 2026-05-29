# Java Raytracer

This project is a CPU raytracer in Java with OBJ mesh loading, smooth/flat shading support, hard shadows, Blinn-Phong specular, material-based shading (including albedo textures), multithreaded tiled rendering, and two-level BVH acceleration.

## What it supports so far

- Ray object intersection for `Sphere`, `Triangle`, and `MeshObject3D`.
- OBJ loading (`v`, `vt`, `vn`, `f`) with smoothing groups and fan triangulation.
- OBJ material support (`mtllib` + `usemtl`) with `map_Kd`, bump/normal map tokens, and optional `map_Pr` roughness map.
- Per-face multi-material OBJ support by splitting one OBJ into multiple `MeshObject3D` instances (one per `usemtl` group).
- Triangle culling modes: `none`, `back`, `front`.
- Shading:
  - Lambert diffuse.
  - Blinn-Phong specular (`specularStrength`, `shininess`, `specularColor`).
  - Ambient term.
  - Hard shadows.
  - Soft shadows via stochastic area-light sampling.
- Materials:
  - Per-object `Material` assignment.
  - Named materials in scene files.
  - Optional `albedoMap` texture sampling using UVs.
  - Optional tangent-space `normalMap`.
  - `normalStrength` scalar control.
  - Optional `roughness` and `roughnessMap`.
- Texture coordinates:
  - OBJ `vt` is parsed and interpolated with barycentric coordinates on triangles.
  - Spherical UV projection for `Sphere`.
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
render shadowSamples int
render pointLightRadius double
render spotLightRadius double
render directionalAngle degrees
```

- Soft shadow controls are optional; defaults keep hard shadows.
- `shadowSamples > 1` enables Monte Carlo soft shadows for lights that have non zero emitter size:
  - point lights use `pointLightRadius`
  - spot lights use `spotLightRadius`
  - directional lights use `directionalAngle` (sun angular radius approximation)

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
  - `material=name`
  - `albedomap=relative/or/absolute/path.png`
  - `normalmap=relative/or/absolute/path.png`
  - `normalstrength=double` (>= 0, default 1)
  - `roughness=double` (0..1)
  - `roughnessmap=relative/or/absolute/path.png`
- Material layering behavior:
  - If OBJ has MTLs, they stay intact by default.
  - `material=name` replaces base material for all sub-meshes from that line.
  - Any material option (`spec`, `shininess`, maps, etc.) acts as an override on top of the current base (named material or per-face MTL).
  - Texture options can be cleared explicitly with empty values (example: `normalmap=`).

Example:

```txt
mesh mesh/bunny.obj 0 0 1 none fit=5 translate=0,0,-4 rotate=0,90,0 spec=1 shininess=64 speccolor=1,1,1
```

### Sphere (optional)

```txt
sphere cx cy cz radius r g b [options]
```

- optional sphere options: `spec=`, `shininess=`, `speccolor=`, `material=`, `albedomap=`, `normalmap=`, `normalstrength=`, `roughness=`, `roughnessmap=`
- Sphere material options follow the same override behavior as meshes.

### Materials

```txt
material name r g b [options]
```

or:

```txt
material name inherit=baseName [options]
```

- material options:
  - `inherit=baseName` (optional, can also be used in the header form above)
  - `spec=double`
  - `shininess=double`
  - `roughness=double` (0..1)
  - `speccolor=r,g,b`
  - `albedomap=relative/or/absolute/path.png`
  - `normalmap=relative/or/absolute/path.png`
  - `normalstrength=double` (>= 0, default 1)
  - `roughnessmap=relative/or/absolute/path.png`

## OBJ + MTL notes

- If an OBJ includes `mtllib` and `usemtl`, the loader reads the MTL file and resolves its texture maps.
- Texture paths in MTL are resolved relative to the MTL file location.
- Per-face `usemtl` changes are preserved by creating one mesh per material group (`ObjReader.loadAsMeshes(...)`).
- `ObjReader.loadAsMesh(...)` remains as backward-compatible merged output and does not preserve per-face material regions.
- Scene parser supports quoted tokens for paths with spaces (for example: `render output "output/my render.png"`).

Example:

```txt
material chrome 0.8 0.8 0.8 spec=1 shininess=128 speccolor=1,1,1
material chromeSoft inherit=chrome spec=0.2 roughness=0.4
material checker 1 1 1 spec=0.05 shininess=16 albedomap=textures/checker.png
mesh mesh/teapot.obj 1 1 1 none material=chrome fit=2.0 translate=0,0,-4
sphere 0 1 -3 0.8 1 1 1 material=checker
```

## Current entrypoints

- Preferred: `Main --scene <sceneFile>`


