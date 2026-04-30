# Raytracer (Java)

Simple CPU raytracer written in Java.

## What this project does

- Renders a scene to `output/render.png`.
- Supports `Sphere` and `Triangle` intersection.
- Supports loading an OBJ mesh as a single `Object3D`.
- OBJ loader currently uses:
  - vertex positions: `v`
  - faces with position indices only: `f v v v`

## Project structure

- `src/Main.java`: app entry point
- `src/raytracer/core`: rays, scene, intersections
- `src/raytracer/geometry`: primitives and mesh container
- `src/raytracer/io/ObjReader.java`: OBJ reader
- `src/raytracer/renderer`: camera and renderer
- `src/raytracer/utils`: math and color helpers

## How to run

From the project root:

Make sure to include your mesh as a parameter when running `Main.java`



The output image is written to:

- `output/render.png`

## Notes

- If your OBJ uses `f v/t/n`, it will fail with the current parser.
- For now, loaded mesh triangles use color `WHITE` and culling mode `BACK_FACE`.

