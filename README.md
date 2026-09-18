# NodeBox

> **Procedural Generative Art, Computational Geometry & Graphic Design Studio**

NodeBox is a visual, node-based procedural software application for creating generative art, motion graphics, computational geometry, and graphic design. It provides a non-destructive workflow where every operation is represented by a visual block of code.

![NodeBox Workspace](Screenshot%202026-09-18%20092736.png)

---

## Authors & Contributors

- **Mustapha Asbbar** ([@alezzacreative](https://github.com/alezzacreative)) — Modern Architecture, GPU Acceleration, Vector Geometry Engine, Theming & Creative Node Suite
- **Frederik De Bleser** ([@fdb](https://github.com/fdb)) — Original Author & Architecture
- **Experimental Media Research Group (EMRG)** — [emrg.be](https://www.emrg.be)

---

## Key Highlights & What's New

### 1. Modern Theming & Interface Engine
- **Dark & Light Themes**: Beautifully crafted themes with crisp typography, bespoke vector splitters, headers, and tabs across the entire application.
- **Unified UI Controls**: Anti-aliased rounded buttons (`ThemeButtonUI`), sleek floating scrollbars (`ThemeScrollBarUI`), minimalist dropdowns (`ThemeComboBoxUI`), and dark table headers (`ThemeTableHeaderUI`).
- **Network Radar Mini-Map**: Real-time HUD navigator overlay on the Network canvas with interactive panning and viewport frame tracking.
- **Port Auto-Spacing & Distinct Output Tabs**: Smart input port spacing that prevents tabs from floating beyond node boundaries, with signature emerald green (`#22B14C`) output ports.

### 2. Interactive Canvas UX & Blender-Style Shortcuts
- **Blender Modal Shortcuts**: Press **`G`** (Grab / Move), **`R`** (Rotate), or **`S`** (Scale / Resize) on the canvas to interactively manipulate shapes with live HUD feedback.
- **Axis Snapping**: Press **`X`** or **`Y`** during transformation to constrain movement or scaling strictly along an axis.
- **Aspect Ratio Locking**: Dedicated `[ ] Lock` aspect ratio toggle on Point controls and paired dimension ports (width/height, scale, roundness).
- **Transform & Bounds Gizmo**: Visual selection bounding box with 8 control handles and live dimension badge ($W \times H$).

### 3. GPU Hardware Acceleration
- **Native Pipeline Support**: Built-in support for **Direct3D** (Windows), **Apple Metal** (macOS), and **OpenGL** (Linux).
- **Direct VRAM Blitting**: Fast hardware rendering with anti-aliasing and zero ghosting during rapid pan/zoom operations.
- **Preferences Switch**: Enabled by default in *Preferences > Rendering & Performance*, displaying live hardware pipeline diagnostics.

### 4. Advanced Geometry Modifiers & Cavalry Cloner
- **`round_corners`**: Procedural corner styling for shapes, polygons, and text outlines with 5 distinct modes:
  - `round` (Fillet): Tangent cubic Bézier circular arcs.
  - `chamfer` (Bevel): Flat diagonal corner cuts.
  - `scoop` (Concave / Inset): Inverted arcs curving into the shape.
  - `dogbone` (CNC Relief): Circular outward clearance lobes for mechanical joints and industrial styling.
  - `squircle` (Superellipse / G2 Curvature): Continuous curvature transitions matching iOS app icons.
  - Features angle threshold filtering and automatic edge length clamping.
- **`duplicator`**: Cavalry & Patternodes-style procedural cloner:
  - **Distribution Patterns**: Linear, Grid (with brick/hex row stagger), Radial (with tangent orientation), Spiral (Archimedean & $137.5^\circ$ golden ratio phyllotaxis), and Along Path.
  - **Step Modifiers**: Cumulative per-clone step rotation, scale percentage, and opacity fade.
  - **Jitter & Noise**: Deterministic pseudo-random position, rotation, and scale variations with random seed.
- **`boolean`**: Constructive Solid Geometry (CSG Union, Difference, Intersection, and Exclude).
- **`stroke_style`**: Parametric dash patterns, line caps, corner joins, and directional arrowheads.
- **`morph`**: Smooth shape tweening between arbitrary differing vector topologies.
- **`smooth`**: Chaikin corner-cutting subdivision smoothing.
- **`offset_path`**: Insets or dilates vector contours by exact distance offsets.

### 5. Organic Generative Art, Physics & Noise
- **`noise`**: 2D/3D Simplex noise, Worley cellular distance noise, and multi-octave Fractal Brownian Motion (fBm).
- **`flow_field`**: Traces organic Simplex noise streamlines from seed points.
- **`lsystem`**: Lindenmayer string-rewriting engine with turtle graphics branching.
- **`spring`**: Physical second-order damped harmonic oscillator differential equation for lifelike motion.
- **`attractor`**: Multi-falloff gravitational point displacement (linear, inverse, vortex, spiral).

### 6. Computational Geometry & Data Visualization
- **`delaunay` & `voronoi`**: Bowyer-Watson 2D triangulation and bounded Voronoi diagram cells with inset padding.
- **`convex_hull`**: Andrew's monotone chain convex bounding polygon.
- **`trace_contours`**: Marching Squares vector isoline tracer from raster luminance.
- **`gradient` & `conic_gradient`**: Multi-stop linear and radial gradients with perceptual **Oklab** color space and angular sweep gradients.
- **`barchart` & `donut`**: Parametric vector bar charts, pie charts, and donut charts.

### 7. Graphic Design & Typography Suite
- **Editorial Typography**: `typeset` (multi-column text flow), `fit_text` (auto-scaling box fit), and `text_on_path`.
- **Patterns & Textures**: `halftone`, `truchet_tiles`, `guilloche` (security spirographs), and `metaballs`.
- **Color Palettes**: `harmony` (color wheel schemes), `extract_palette` (K-Means image extraction), and `blend_mode`.
- **Layout & Composition**: `modular_grid`, `align_distribute`, and `pack_circles`.
- **Finishing & Effects**: `roughen`, `long_shadow`, and `extrude_3d` with facet shading.

### 8. Multi-Artboard & Page Layout System
- **`artboard`**: Define multiple designated export frames with industry presets (Instagram Story, Post, Twitter/X Header, YouTube Thumbnail, A4, US Letter, Business Card, or Custom).
- **Batch Export**: File > **Export All Artboards...** automatically renders and saves every artboard at its exact preset dimensions (PNG, SVG, PDF).

### 9. Motion Export & Animation
- **Video & GIF Export**: Render animations directly to high-quality palettegen **Animated GIF**, **WebM**, and video formats.

### 10. Intelligent Diagnostics & Offline Documentation
- **Visual Error Indicators**: Failing nodes are outlined with a vivid 3px red glow and an alert badge `(!)`.
- **Smart Warning Bar Tooltips**: Hovering over error warnings displays suggested remedies for argument mismatches, null upstream data, zero division, or memory bounds.
- **Click to Focus**: Single-click the warning bar to center and select the error node; double-click for the full stack trace.
- **Offline Documentation Encyclopedia**: Built-in offline reference manual with rich HTML tooltips in canvas, node palette, and quick-add search (`Tab` / `Shift+F`).

---

## Building and Running

### Prerequisites
- **Java JDK**: JDK 17 or higher (tested on JDK 17, 21, and 25).
- **Apache Ant**: 1.10+.
- **Maven**: Used for dependency resolution.

### Quick Start
```shell
# Clone the repository
git clone https://github.com/alezzacreative/nodebox.git
cd nodebox

# Build and launch NodeBox
ant run
```

### Running Tests
```shell
# Run all unit tests
ant test

# Run UI end-to-end tests (requires graphical display)
NODEBOX_E2E=1 ant test-e2e

# Run drag responsiveness performance benchmark
ant test-perf
```

### Packaging Distribution Apps
```shell
# macOS bundle (.app in dist/mac)
ant dist-mac

# Windows package (.exe / .msi in dist/windows)
ant dist-win
```

---

## Platform Build Details

### macOS
Install dependencies via Homebrew:
```shell
brew install ant maven
ant run
```

### Windows
1. Install [Git for Windows](https://git-scm.com/).
2. Install [Adoptium Temurin JDK 17+](https://adoptium.net/).
3. Install [Apache Ant](https://ant.apache.org/).
4. Launch Command Prompt or PowerShell:
```shell
ant run
```

### Linux (Ubuntu / Debian / Fedora / Arch)
```shell
# Ubuntu / Debian:
sudo apt install git openjdk-17-jdk ant maven

# Fedora:
sudo dnf install git java-17-openjdk ant maven

# Build and run:
ant run
```

---

## License

NodeBox is licensed under the [GNU General Public License v3 (GPLv3)](LICENSE.txt).
Libraries, example files, and bundled resources are distributed under their respective licenses.
