package com.geomeasure.landtracker.domain.gis

import com.geomeasure.landtracker.domain.model.LatLngNode

/**
 * Monitors user proximity to the start node to trigger auto-snapping & loop closure.
 */
class PolygonSnapper(
    private val calculator: GeodesicAreaCalculator,
    private val snapThresholdMeters: Double = 4.0
) {
    fun shouldSnapToOrigin(currentNode: LatLngNode, nodes: List<LatLngNode>): Boolean {
        if (nodes.size < 3) return false
        val origin = nodes.first()
        val dist = calculator.calculateDistanceBetween(currentNode, origin)
        return dist <= snapThresholdMeters
    }
}
