package com.geomeasure.landtracker.presentation.ui

import android.graphics.Color as AndroidColor
import android.location.Location
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.fillAntialias
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import kotlin.math.floor

private const val SOURCE_ID = "terram-boundary-source"
private const val LINE_LAYER_ID = "terram-perimeter-line-layer"
private const val FILL_LAYER_ID = "terram-polygon-fill-layer"
private const val CURRENT_POINT_LAYER_ID = "terram-current-point-layer"
private const val USER_LOCATION_SOURCE_ID = "terram-user-location-source"
private const val USER_LOCATION_LAYER_ID = "terram-user-location-layer"
private const val SATELLITE_TILE_URL =
    "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
private const val MAX_IMAGERY_ZOOM = 23.0
private const val SATELLITE_STYLE_JSON = """
    {
      "version": 8,
      "sources": {
        "terram-satellite-source": {
          "type": "raster",
          "tiles": ["$SATELLITE_TILE_URL"],
          "tileSize": 256,
          "maxzoom": 23,
          "attribution": "Imagery © Esri, Vantor, Earthstar Geographics, and the GIS User Community"
        }
      },
      "layers": [
        {
          "id": "terram-satellite-layer",
          "type": "raster",
          "source": "terram-satellite-source"
        }
      ]
    }
"""

@Composable
fun TerraMVectorMap(
    trackNodes: List<Point>,
    isTrackingActive: Boolean,
    userLocation: Location?,
    onLocateMe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    remember(context) { MapLibre.getInstance(context) }

    val mapView = remember(context) {
        MapView(context).also { it.onCreate(null) }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleLoaded by remember { mutableStateOf(false) }
    var initialCameraPositioned by remember { mutableStateOf(false) }
    var locatePending by remember { mutableStateOf(false) }
    var mapLoadError by remember { mutableStateOf(false) }
    var lastCenteredLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    DisposableEffect(mapView, lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val loadFailureListener = MapView.OnDidFailLoadingMapListener {
            mapLoadError = true
        }
        mapView.addOnDidFailLoadingMapListener(loadFailureListener)
        var started = false
        var resumed = false
        var destroyed = false

        fun start() {
            if (!started && !destroyed) {
                mapView.onStart()
                started = true
            }
        }

        fun resume() {
            start()
            if (!resumed && !destroyed) {
                mapView.onResume()
                resumed = true
            }
        }

        fun pause() {
            if (resumed && !destroyed) {
                mapView.onPause()
                resumed = false
            }
        }

        fun stop() {
            pause()
            if (started && !destroyed) {
                mapView.onStop()
                started = false
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_RESUME -> resume()
                Lifecycle.Event.ON_PAUSE -> pause()
                Lifecycle.Event.ON_STOP -> stop()
                Lifecycle.Event.ON_DESTROY -> {
                    stop()
                    if (!destroyed) {
                        mapView.onDestroy()
                        destroyed = true
                    }
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resume()

        onDispose {
            lifecycle.removeObserver(observer)
            mapView.removeOnDidFailLoadingMapListener(loadFailureListener)
            stop()
            if (!destroyed) {
                mapView.onDestroy()
                destroyed = true
            }
        }
    }

    LaunchedEffect(
        trackNodes,
        userLocation,
        styleLoaded,
        isTrackingActive,
        locatePending
    ) {
        val currentMap = map ?: return@LaunchedEffect
        val style = currentMap.style ?: return@LaunchedEffect
        if (!styleLoaded) return@LaunchedEffect

        updateVectorSource(style, trackNodes)
        updateUserLocationSource(style, userLocation)

        val locationCoordinates = userLocation?.let { it.latitude to it.longitude }
        if (
            userLocation != null &&
            (locationCoordinates != lastCenteredLocation || locatePending)
        ) {
            val locationPosition = LatLng(userLocation.latitude, userLocation.longitude)
            val cameraUpdate =
                CameraUpdateFactory.newLatLngZoom(locationPosition, MAX_IMAGERY_ZOOM)
            if (initialCameraPositioned || locatePending) {
                currentMap.animateCamera(cameraUpdate, 350)
            } else {
                currentMap.moveCamera(cameraUpdate)
            }
            initialCameraPositioned = true
            locatePending = false
            lastCenteredLocation = locationCoordinates
        }

        val lastPoint = trackNodes.lastOrNull()
        if (lastPoint != null && (isTrackingActive || !initialCameraPositioned)) {
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                LatLng(lastPoint.latitude(), lastPoint.longitude()),
                MAX_IMAGERY_ZOOM
            )
            if (initialCameraPositioned) {
                currentMap.animateCamera(cameraUpdate, 350)
            } else {
                currentMap.moveCamera(cameraUpdate)
            }
            initialCameraPositioned = true
        }
    }



    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        ) { view ->
            if (map == null) {
                view.getMapAsync { mapLibreMap ->
                    map = mapLibreMap
                    mapLibreMap.setStyle(
                        Style.Builder().fromJson(SATELLITE_STYLE_JSON)
                    ) { style ->
                        mapLibreMap.setMaxZoomPreference(MAX_IMAGERY_ZOOM)
                        setupVectorLayers(style)
                        mapLoadError = false
                        styleLoaded = true
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .shadow(6.dp, CircleShape),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            IconButton(
                onClick = {
                    val location = userLocation
                    if (location != null) {
                        map?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(location.latitude, location.longitude),
                                MAX_IMAGERY_ZOOM
                            ),
                            350
                        )
                    } else {
                        locatePending = true
                    }
                    onLocateMe()
                },
                modifier = Modifier.size(48.dp)
            ) {
                GpsCrosshairIcon()
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 8.dp),
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = "Imagery © Esri & contributors",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }

        if (mapLoadError) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Map imagery failed to load. Check your internet connection.",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}



private fun setupVectorLayers(style: Style) {
    if (style.getSource(USER_LOCATION_SOURCE_ID) == null) {
        style.addSource(
            GeoJsonSource(
                USER_LOCATION_SOURCE_ID,
                FeatureCollection.fromFeatures(emptyArray())
            )
        )
        style.addLayer(
            CircleLayer(USER_LOCATION_LAYER_ID, USER_LOCATION_SOURCE_ID).apply {
                setProperties(
                    circleColor(AndroidColor.parseColor("#4285F4")),
                    circleRadius(8f),
                    circleStrokeColor(AndroidColor.WHITE),
                    circleStrokeWidth(2f)
                )
            }
        )
    }

    if (style.getSource(SOURCE_ID) != null) return

    style.addSource(
        GeoJsonSource(SOURCE_ID, FeatureCollection.fromFeatures(emptyArray()))
    )
    style.addLayer(
        FillLayer(FILL_LAYER_ID, SOURCE_ID).apply {
            setProperties(
                fillColor(AndroidColor.parseColor("#667C5C38")),
                fillAntialias(true)
            )
        }
    )
    style.addLayer(
        LineLayer(LINE_LAYER_ID, SOURCE_ID).apply {
            setProperties(
                lineColor(AndroidColor.parseColor("#E8A15B")),
                lineWidth(4f),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND)
            )
        }
    )
    style.addLayer(
        CircleLayer(CURRENT_POINT_LAYER_ID, SOURCE_ID).apply {
            setProperties(
                circleColor(AndroidColor.parseColor("#E8A15B")),
                circleRadius(7f),
                circleStrokeColor(AndroidColor.WHITE),
                circleStrokeWidth(2f)
            )
        }
    )
}

private fun updateVectorSource(style: Style, nodes: List<Point>) {
    val source = style.getSourceAs<GeoJsonSource>(SOURCE_ID) ?: return
    val features = mutableListOf<Feature>()

    if (nodes.size >= 2) {
        features += Feature.fromGeometry(LineString.fromLngLats(nodes))
    }

    nodes.lastOrNull()?.let { currentPoint ->
        features += Feature.fromGeometry(currentPoint)
    }

    if (nodes.size >= 3) {
        val closedNodes = nodes.toMutableList()
        if (closedNodes.first() != closedNodes.last()) {
            closedNodes += closedNodes.first()
        }
        features += Feature.fromGeometry(Polygon.fromLngLats(listOf(closedNodes)))
    }

    source.setGeoJson(FeatureCollection.fromFeatures(features))
}

private fun updateUserLocationSource(style: Style, userLocation: Location?) {
    val source = style.getSourceAs<GeoJsonSource>(USER_LOCATION_SOURCE_ID) ?: return
    val features = userLocation?.let {
        arrayOf(Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)))
    } ?: emptyArray()
    source.setGeoJson(FeatureCollection.fromFeatures(features))
}

@Composable
private fun GpsCrosshairIcon() {
    val iconColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.size(24.dp)) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.27f
        val outerRadius = size.minDimension * 0.42f
        val gap = size.minDimension * 0.12f
        val strokeWidth = size.minDimension * 0.09f

        drawCircle(
            color = iconColor,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        drawCircle(
            color = iconColor,
            radius = strokeWidth / 2f,
            center = center
        )
        drawLine(
            iconColor,
            androidx.compose.ui.geometry.Offset(center.x, 0f),
            androidx.compose.ui.geometry.Offset(center.x, gap),
            strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            iconColor,
            androidx.compose.ui.geometry.Offset(center.x, size.height - gap),
            androidx.compose.ui.geometry.Offset(center.x, size.height),
            strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            iconColor,
            androidx.compose.ui.geometry.Offset(0f, center.y),
            androidx.compose.ui.geometry.Offset(gap, center.y),
            strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            iconColor,
            androidx.compose.ui.geometry.Offset(size.width - gap, center.y),
            androidx.compose.ui.geometry.Offset(size.width, center.y),
            strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
