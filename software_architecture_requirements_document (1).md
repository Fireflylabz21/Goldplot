# Software Requirements Specification (SRS) & Architecture Blueprint
## High-Accuracy Land Measurement Android Application (Kotlin)

---

## 1. Executive Summary & System Context

The objective is to build a native Android application using **Kotlin** and **Jetpack Compose** that enables users to measure land areas by walking around the perimeter of a property. The application records GNSS/GPS track points, applies real-time filtering algorithms to prune spatial jitter, renders the boundaries dynamically on an interactive vector map, and computes precise geodesic area and perimeter measurements using WGS84 ellipsoidal geometry.

---

## 2. Technology Stack & Architectural Choice

### 2.1 Technology Stack

* **Language:** Kotlin 2.x
* **UI Framework:** Jetpack Compose (Modern Declarative UI with Canvas rendering)
* **Architecture Pattern:** MVVM (Model-View-ViewModel) + Clean Architecture (Data, Domain, Presentation Layers)
* **Asynchrony & Data Streams:** Kotlin Coroutines & `StateFlow` / `SharedFlow`
* **Dependency Injection:** Hilt / Dagger
* **Location API:** Google Play Services - `FusedLocationProviderClient` + Android Location Framework (`LocationManager` fallback / Raw GNSS measurements)
* **Map SDK:** MapLibre Native Android SDK (for open-source offline capability and vector tile styling) or Mapbox Android SDK
* **Spatial & Geometry Computation:**
  * **GeographicLib-Java / Kotlin:** For geodesic area calculation over the WGS84 ellipsoid.
  * **JTS Topology Suite (Java Topology Suite) / Turf-JVM:** For spatial operations (polygon self-intersection checks, buffering, vertex simplification).
* **Local Storage / Offline:** Room Database (Metadata & Polyline GeoJSON) + Tile Caching Engine
* **Export Libraries:** Jackson / Gson (GeoJSON Serialization), Apache POI / iText (PDF/Excel Report Generation)

---

## 3. High-Level Architecture (Clean Architecture)

```
┌────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                            │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Jetpack Compose UI (MapView, DiagramView Canvas, Dashboard)       │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                  │                                     │
│                        Observes via StateFlow                          │
│                                  ▼                                     │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ ViewModels (TrackingViewModel, ParcelDetailViewModel)           │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                           Executes Use Cases
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                             DOMAIN LAYER                               │
│  ┌─────────────────────────┐  ┌─────────────────────────────────────┐  │
│  │ Spatial Filtering Engine│  │ Geodesic Geometry Calculator        │  │
│  │ (Kalman / HDOP Filter)  │  │ (Area, Perimeter, Bearing)          │  │
│  └─────────────────────────┘  └─────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Use Cases: ProcessLocationPoint, ClosePolygon, ExportParcel      │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                       Calls Repository Interfaces
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                              DATA LAYER                                │
│  ┌───────────────────────────┐       ┌──────────────────────────────┐  │
│  │ Location Data Source      │       │ Room Database                │  │
│  │ (FusedLocationProvider)   │       │ (Saved Parcels, Vertices)    │  │
│  └───────────────────────────┘       └──────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Software Requirements Specification

### 4.1 Functional Requirements (FR)

| Req ID | Module | Requirement Description |
| :--- | :--- | :--- |
| **FR-01** | Location Service | Continuous tracking of user coordinates ($\text{Lat}, \text{Lng}, \text{Altitude}$) via foreground service with persistent status bar notification. |
| **FR-02** | Signal Quality Engine | Real-time monitoring of GNSS metrics: Horizontal Dilution of Precision (HDOP), Horizontal Accuracy Radius ($\pm \text{meters}$), and Satellite Count. |
| **FR-03** | Filtering & Pruning | Reject raw location points where estimated horizontal accuracy exceeds a user-defined threshold (e.g., $>3.0\text{m}$) or speed threshold anomalies indicate GPS jump spikes. |
| **FR-04** | Dual View Rendering | Provide seamless toggle between **Map View** (interactive vector base map with markers connected via geodesic paths) and **Diagram View** (offline 2D canvas displaying dynamically scaled boundary nodes on a pixel grid layout with proportional buffering). |
| **FR-05** | Polygon Closure | Auto-detection when the user walks within a configurable snap distance (e.g., 3m–5m) of the starting vertex node, triggering a snapping prompt. |
| **FR-06** | Polygon Manual Edit | Ability to manually drag, insert, or delete individual vertex pins on both Map and Diagram views. |
| **FR-07** | Real-Time Metrics | Real-time calculation and display of total walked distance, current enclosed area (m², Hectares, Acres, Sq Ft), and closed loop status. |
| **FR-08** | Offline Capability | Native diagram rendering requires zero internet connection. Vector base tiles can also be cached for remote field usage. |
| **FR-09** | Export Functionality | Export finalized land plots into standardized GIS formats: GeoJSON, KML, ESRI Shapefile, and structured PDF reports containing satellite boundary snapshots. |

### 4.2 Non-Functional Requirements (NFR)

* **NFR-01 (Battery Optimization):** Foreground Location Service must use adaptive interval sampling based on accelerometer movement (e.g., pause updates when stationary).
* **NFR-02 (Precision & Accuracy):** Spatial area calculations must utilize WGS84 geodesic algorithms (Karney’s method via GeographicLib) rather than Euclidean approximation to eliminate projection distortion.
* **NFR-03 (Lifecycle & State Resilience):** App must handle activity recreation and screen orientation flips gracefully without leaking map surface textures, losing in-memory polyline state, or triggering surface rendering crashes.
* **NFR-04 (Performance):** Dynamic rendering of polylines up to 10,000 vertices must maintain a minimum frame rate of 60 FPS on mid-range Android hardware.

---

## 5. Domain Logic & Spatial Algorithms

### 5.1 Distance & Drift Filtering Strategy

```
                               ┌─────────────────────────┐
                               │  Incoming Raw Location  │
                               └────────────┬────────────┘
                                            │
                                            ▼
                               /─────────────────────────\
                              < Accuracy <= Threshold?    >
                               \─────────────────────────/
                                      │           │
                                   NO │           │ YES
                                      ▼           ▼
                               ┌──────────┐  /─────────────────────────\
                               │ Reject   │ < Dist to Last Node >= Min? >
                               └──────────┘  \─────────────────────────/
                                                    │           │
                                                 NO │           │ YES
                                                    ▼           ▼
                                             ┌──────────┐  ┌──────────────────┐
                                             │ Ignore   │  │ Apply Extended   │
                                             │ Drift    │  │ Kalman Filtering │
                                             └──────────┘  └────────┬─────────┘
                                                                    │
                                                                    ▼
                                                           [ Store Node Vertex ]
```

### 5.2 WGS84 Geodesic Area Calculation Engine

To achieve millimeter-precision area calculations over long perimeters on the Earth spheroid, the application uses **Karney's Algorithm** (`GeographicLib` implementation).

For spherical fallback approximation:

$$A = R^2 \sum_{i=1}^{n} (\lambda_{i+1} - \lambda_i) \left(2 + \sin(\phi_i) + \sin(\phi_{i+1})\right)$$

*Where:*
* $\phi$ = Latitude in radians
* $\lambda$ = Longitude in radians
* $R = 6,378,137\text{ m}$ (Earth equatorial radius)

---

## 6. Implementation Code Blueprint (Kotlin + Jetpack Compose)

### 6.1 Location Tracking Service (`Foreground Tracking`)

```kotlin
package com.example.landmeasurement.data.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class BoundaryTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val _locationFlow = MutableSharedFlow<Location>(replay = 1)
    val locationFlow: SharedFlow<Location> = _locationFlow

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    // Emit location stream to subscribers
                    CoroutineScope(Dispatchers.IO).launch {
                        _locationFlow.emit(location)
                    }
                }
            }
        }
    }

    fun startTracking() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000L
        ).apply {
            setMinUpdateIntervalMillis(500L)
            setMinUpdateDistanceMeters(1.0f) // Threshold to reduce stationary jitter
        }.build()

        startForegroundServiceNotification()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (unlikely: SecurityException) {
            // Permission lost
        }
    }

    private fun startForegroundServiceNotification() {
        val channelId = "land_tracking_channel"
        val notificationManager = getSystemService(NotificationManager::class.java)
        
        val channel = NotificationChannel(
            channelId, "Land Measurement Tracking", NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Land Measurement in Progress")
            .setContentText("Logging GNSS boundary coordinates...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

---

### 6.2 Geodesic Area Calculation Module (`GeographicLib`)

```kotlin
package com.example.landmeasurement.domain.gis

import net.sf.geographiclib.Geodesic
import net.sf.geographiclib.PolygonArea
import kotlin.math.abs

data class LatLngNode(val latitude: Double, val longitude: Double)

data class MeasurementResult(
    val areaSquareMeters: Double,
    val acres: Double,
    val hectares: Double,
    val perimeterMeters: Double
)

class GeodesicAreaCalculator {

    /**
     * Calculates precise area and perimeter over the WGS84 ellipsoid.
     */
    fun computeLandMetrics(nodes: List<LatLngNode>): MeasurementResult {
        if (nodes.size < 3) {
            return MeasurementResult(0.0, 0.0, 0.0, 0.0)
        }

        val polygonArea = PolygonArea(Geodesic.WGS84, false)

        for (node in nodes) {
            polygonArea.AddPoint(node.latitude, node.longitude)
        }

        // Compute result: compute(reverse, sign)
        val result = polygonArea.Compute(false, true)
        val areaSqMeters = abs(result.area)
        val perimeterM = result.perimeter

        val acres = areaSqMeters * 0.000247105
        val hectares = areaSqMeters / 10000.0

        return MeasurementResult(
            areaSquareMeters = areaSqMeters,
            acres = acres,
            hectares = hectares,
            perimeterMeters = perimeterM
        )
    }
}
```

---

### 6.3 Dual View Engine: Map & Canvas Diagram (`Jetpack Compose`)

```kotlin
package com.example.landmeasurement.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.landmeasurement.domain.gis.LatLngNode
import com.example.landmeasurement.domain.gis.MeasurementResult

enum class ViewMode { MAP, DIAGRAM }

@Composable
fun MainMeasurementContainer(
    nodes: List<LatLngNode>,
    metrics: MeasurementResult,
    gpsAccuracy: Float,
    isTracking: Boolean,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    onClear: () -> Unit
) {
    var selectedViewMode by rememberSaveable { mutableStateOf(ViewMode.MAP) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top View Selector Segmented Control
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            SegmentedButton(
                selected = selectedViewMode == ViewMode.MAP,
                onClick = { selectedViewMode = ViewMode.MAP },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Map View")
            }
            SegmentedButton(
                selected = selectedViewMode == ViewMode.DIAGRAM,
                onClick = { selectedViewMode = ViewMode.DIAGRAM },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Diagram View (Offline)")
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedViewMode) {
                ViewMode.MAP -> MapViewContainer(nodes = nodes)
                ViewMode.DIAGRAM -> DiagramCanvasView(nodes = nodes)
            }
        }

        // Persistent Dashboard Overlay
        MeasurementDashboardOverlay(
            metrics = metrics,
            gpsAccuracy = gpsAccuracy,
            isTracking = isTracking,
            onStartTracking = onStartTracking,
            onStopTracking = onStopTracking,
            onClear = onClear
        )
    }
}

/**
 * Offline Canvas Diagram View with Proportional Scaling & Pixel Grid Background
 */
@Composable
fun DiagramCanvasView(
    nodes: List<LatLngNode>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val padding = 60f

        // Draw Pixel Grid Layout
        val gridSize = 40f
        var x = 0f
        while (x < canvasWidth) {
            drawLine(Color(0xFFE0E0E0), Offset(x, 0f), Offset(x, canvasHeight), strokeWidth = 1f)
            x += gridSize
        }
        var y = 0f
        while (y < canvasHeight) {
            drawLine(Color(0xFFE0E0E0), Offset(0f, y), Offset(canvasWidth, y), strokeWidth = 1f)
            y += gridSize
        }

        if (nodes.size < 2) return@Canvas

        // Normalize and Scale Lat/Lng Bounding Box to Canvas Coordinates
        val minLat = nodes.minOf { it.latitude }
        val maxLat = nodes.maxOf { it.latitude }
        val minLng = nodes.minOf { it.longitude }
        val maxLng = nodes.maxOf { it.longitude }

        val latSpan = (maxLat - minLat).takeIf { it > 0 } ?: 0.00001
        val lngSpan = (maxLng - minLng).takeIf { it > 0 } ?: 0.00001

        val drawWidth = canvasWidth - (2 * padding)
        val drawHeight = canvasHeight - (2 * padding)

        val scaledPoints = nodes.map { node ->
            val px = padding + ((node.longitude - minLng) / lngSpan * drawWidth).toFloat()
            // Invert latitude for screen Y coordinates
            val py = padding + ((maxLat - node.latitude) / latSpan * drawHeight).toFloat()
            Offset(px, py)
        }

        // Render Path & Vector Polygon
        val path = Path().apply {
            moveTo(scaledPoints.first().x, scaledPoints.first().y)
            for (i in 1 until scaledPoints.size) {
                lineTo(scaledPoints[i].x, scaledPoints[i].y)
            }
        }

        drawPath(path = path, color = Color(0xFF1E88E5), style = Stroke(width = 6f))

        // Draw Vertices
        scaledPoints.forEach { point ->
            drawCircle(color = Color(0xFFD81B60), radius = 8f, center = point)
        }
    }
}

@Composable
fun MapViewContainer(nodes: List<LatLngNode>) {
    // MapView implementation utilizing MapLibre/Mapbox Android SDK with view lifecycle safety
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Map View (MapLibre Vector Tiles)")
    }
}

@Composable
fun MeasurementDashboardOverlay(
    metrics: MeasurementResult,
    gpsAccuracy: Float,
    isTracking: Boolean,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "GPS Accuracy: ±${"%.2f".format(gpsAccuracy)} m",
                style = MaterialTheme.typography.labelMedium,
                color = if (gpsAccuracy <= 3.0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn("Area (Sq. M)", "%.2f".format(metrics.areaSquareMeters))
                MetricColumn("Acres", "%.4f".format(metrics.acres))
                MetricColumn("Perimeter (m)", "%.2f".format(metrics.perimeterMeters))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (!isTracking) {
                    Button(onClick = onStartTracking) {
                        Text("Start Measurement")
                    }
                } else {
                    Button(
                        onClick = onStopTracking,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Finish & Compute")
                    }
                }
                OutlinedButton(onClick = onClear) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
fun MetricColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}
```

---

## 7. Development Phases & Implementation Timeline

```
┌─────────────────────────────────────────────────────────────────────────┐
│ Phase 1: Core GNSS Pipeline & Filtering (Weeks 1–2)                      │
│ - FusedLocationProviderClient Setup                                     │
│ - Kalman Filter & Accuracy Thresholding Engine                          │
├─────────────────────────────────────────────────────────────────────────┤
│ Phase 2: Dual View Rendering Engine & Canvas (Weeks 3–4)                │
│ - MapLibre Android Vector Map Engine Configuration                      │
│ - Jetpack Compose Canvas Diagram View with Dynamic Grid & Buffering    │
│ - Orientation Lifecycle & State Preservation System                     │
├─────────────────────────────────────────────────────────────────────────┤
│ Phase 3: Spatial Engine & Geodesic Calculations (Weeks 5–6)             │
│ - GeographicLib WGS84 Geodesic Polygon Area Algorithm                   │
│ - Polygon Snapping & Vertex Node Dragging/Editing                       │
├─────────────────────────────────────────────────────────────────────────┤
│ Phase 4: Room Database & Export Modules (Weeks 7–8)                      │
│ - Local Storage Architecture                                            │
│ - GeoJSON, KML, and PDF Report Generators                               │
└─────────────────────────────────────────────────────────────────────────┘
```