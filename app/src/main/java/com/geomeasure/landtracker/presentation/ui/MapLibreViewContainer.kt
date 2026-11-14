package com.geomeasure.landtracker.presentation.ui

import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.geomeasure.landtracker.domain.model.LatLngNode
import org.maplibre.geojson.Point

@Composable
fun MapLibreViewContainer(
    nodes: List<LatLngNode>,
    isTrackingActive: Boolean,
    userLocation: Location?,
    onLocateMe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mapPoints = remember(nodes) {
        nodes.map { Point.fromLngLat(it.longitude, it.latitude) }
    }

    TerraMVectorMap(
        trackNodes = mapPoints,
        isTrackingActive = isTrackingActive,
        userLocation = userLocation,
        onLocateMe = onLocateMe,
        modifier = modifier
    )
}
