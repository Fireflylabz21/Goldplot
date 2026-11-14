# Real-Time GPS Tracking on MapLibre Vector Maps

This document provides the technical specification and architecture for implementing real-time GPS tracking and dynamic polygon rendering using **MapLibre Native SDK** in Android applications built with Kotlin and Jetpack Compose.

---

## 1. System Overview

Traditional mapping libraries render GPS points by placing native UI marker views over raster image tiles. This approach creates memory bottlenecks, lag during continuous coordinate updates, and pixelation during zooming.

**MapLibre Vector Mapping** solves this by maintaining a client-side vector database (`GeoJsonSource`) rendered directly via OpenGL/Vulkan shaders. GPS locations update raw coordinate arrays inside memory, and the GPU updates vector line strings and polygons in real-time.

```
┌──────────────────────────┐
│  FusedLocationProvider   │
└────────────┬─────────────┘
             │ (GPS Lat/Lng Stream)
             ▼
┌──────────────────────────┐
│   LocationFilterEngine   │ (Filters Jitter & Low Accuracy)
└────────────┬─────────────┘
             │ (Filtered Coordinate Nodes)
             ▼
┌──────────────────────────┐
│     GeoJsonSource        │ (MapLibre Reactive Vector Data)
└────────────┬─────────────┘
             │
      ┌──────┴──────────────────────────┐
      ▼                                 ▼
┌──────────────┐                 ┌──────────────┐
│  LineLayer   │ (Perimeter)     │  FillLayer   │ (Enclosed Area)
└──────────────┘                 └──────────────┘
```

---

## 2. Key Technical Advantages

| Feature | Raster Tile Markers | MapLibre Vector Data Layers |
| :--- | :--- | :--- |
| **Performance** | High CPU overhead per marker | GPU-accelerated drawing pipeline |
| **Data Updating** | Destroys/recreates view objects | Modifies single dynamic `GeoJsonSource` |
| **Visual Quality** | Pixels blur when zooming | Vectors remain infinitely sharp |
| **Map Rotation** | Text & icons invert or blur | Text automatically stays upright |
| **Polygon Filling** | Slow canvas overlays | Hardware-accelerated GPU polygon fills |

---

## 3. Layer Architecture

1. **`GeoJsonSource` (Data Provider):**
   * Holds the active list of tracked points as a `FeatureCollection`.
   * Contains two primary geometries:
     * `LineString`: The continuous walking path.
     * `Polygon`: Auto-created when closing or previewing the boundary.

2. **`LineLayer` (Path Renderer):**
   * Connects incoming GPS nodes with antialiased lines.
   * Supports dynamic styling (e.g., earth-tone color `#7C5C38`, line width `4dp`, dash arrays).

3. **`FillLayer` (Area Shading):**
   * Renders real-time semi-transparent color fills over the enclosed walking zone.

4. **`SymbolLayer` / `CircleLayer` (Live GPS Pin):**
   * Renders the current user position and active accuracy radius.

---

## 4. Integration Steps

1. Add **MapLibre Native SDK** dependency to `build.gradle.kts`:
   ```kotlin
   implementation("org.maplibre.gl:android-sdk:10.3.0")
   ```
2. Initialize map style (`Style.MAPBOX_STREETS` or custom offline MBTiles style URL).
3. Inject empty `GeoJsonSource` during map style load.
4. Add `LineLayer` and `FillLayer` linked to the source ID.
5. Continuously update `GeoJsonSource.setGeoJson()` when new filtered GPS coordinates arrive.