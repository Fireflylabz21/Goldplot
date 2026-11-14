package com.terram.app.gis.tracking

import android.content.Context
import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.MapLibre
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapLibreMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

private const val SOURCE_ID = "terram-boundary-source"
private const val LINE_LAYER_ID = "terram-perimeter-line-layer"
private const val FILL_LAYER_ID = "terram-polygon-fill-layer"

/**
 * Compose wrapper for MapLibre Vector Map Engine delivering real-time
 * GPS boundary tracking and hardware-accelerated polygon updates.
 */
@Composable
fun VectorMapTrackingEngine(
    trackNodes: List<Point>,
    currentLocation: Point?,
    isTrackingActive: Boolean,
    modifier: Modifier = Modifier,
    mapStyleUrl: String = Style.MAPBOX_STREETS
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize MapLibre Singleton once
    RememberMapLibreInit(context)

    val mapView = remember { MapView(context) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var isStyleLoaded by remember { mutableStateOf(false) }

    // Map Lifecycle Handling
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Initialize Map & Layers
    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize()
    ) { view ->
        if (mapLibreMap == null) {
            view.getMapAsync { map ->
                mapLibreMap = map
                map.setStyle(Style.Builder().fromUri(mapStyleUrl)) { style ->
                    setupVectorLayers(style)
                    isStyleLoaded = true
                }
            }
        }
    }

    // Reactive Update: Update vector layers whenever coordinates change
    LaunchedEffect(trackNodes, currentLocation, isStyleLoaded) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect

        if (isStyleLoaded) {
            updateVectorSource(style, trackNodes)

            // Auto-center camera on current location
            currentLocation?.let { point ->
                if (isTrackingActive) {
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(point.latitude(), point.longitude()),
                            18.5
                        )
                    )
                }
            }
        }
    }
}

/**
 * Initializes vector GeoJsonSource and adds Line and Fill layers to MapLibre style.
 */
private fun setupVectorLayers(style: Style) {
    if (style.getSource(SOURCE_ID) == null) {
        val geoJsonSource = GeoJsonSource(SOURCE_ID, FeatureCollection.fromFeatures(emptyArray()))
        style.addSource(geoJsonSource)

        // Semi-transparent earth-tone polygon fill
        val fillLayer = FillLayer(FILL_LAYER_ID, SOURCE_ID).apply {
            setProperties(
                fillColor(Color.parseColor("#4A7C5C38")), // Terra Cotta opacity
                fillAntialias(true)
            )
        }

        // Crisp vector line string for walking path
        val lineLayer = LineLayer(LINE_LAYER_ID, SOURCE_ID).apply {
            setProperties(
                lineColor(Color.parseColor("#7C5C38")),
                lineWidth(4f),
                lineCap(LINE_CAP_ROUND),
                lineJoin(LINE_JOIN_ROUND)
            )
        }

        style.addLayer(fillLayer)
        style.addLayer(lineLayer)
    }
}

/**
 * Updates GeoJsonSource in memory with latest walking path nodes and enclosed polygon.
 */
private fun updateVectorSource(style: Style, nodes: List<Point>) {
    val source = style.getSourceAs<GeoJsonSource>(SOURCE_ID) ?: return
    val features = mutableListOf<Feature>()

    if (nodes.size >= 2) {
        // Line String for tracked perimeter path
        val lineString = LineString.fromLngLats(nodes)
        features.add(Feature.fromGeometry(lineString))
    }

    if (nodes.size >= 3) {
        // Closed Polygon for area calculation view
        val closedNodes = nodes.toMutableList()
        if (closedNodes.first() != closedNodes.last()) {
            closedNodes.add(closedNodes.first())
        }
        val polygon = Polygon.fromLngLats(listOf(closedNodes))
        features.add(Feature.fromGeometry(polygon))
    }

    source.setGeoJson(FeatureCollection.fromFeatures(features))
}

@Composable
private fun RememberMapLibreInit(context: Context) {
    remember {
        MapLibre.getInstance(context)
    }
}