package com.geomeasure.landtracker.domain.gis

import com.geomeasure.landtracker.domain.model.LatLngNode
import com.geomeasure.landtracker.domain.model.MeasurementResult
import net.sf.geographiclib.Geodesic
import net.sf.geographiclib.PolygonArea
import kotlin.math.abs

/**
 * Computes high-precision ellipsoidal area and perimeter over WGS84
 * using Charles Karney's algorithm (GeographicLib).
 */
class GeodesicAreaCalculator {

    fun computeMetrics(nodes: List<LatLngNode>, forceClosed: Boolean = false): MeasurementResult {
        if (nodes.size < 3) {
            val perimeter = computePerimeter(nodes)
            return MeasurementResult(
                areaSquareMeters = 0.0,
                acres = 0.0,
                hectares = 0.0,
                squareFeet = 0.0,
                perimeterMeters = perimeter,
                isClosed = false
            )
        }

        val polygonArea = PolygonArea(Geodesic.WGS84, false)

        for (node in nodes) {
            polygonArea.AddPoint(node.latitude, node.longitude)
        }

        // Compute polygon metrics: compute(reverse, sign)
        val result = polygonArea.Compute(false, true)
        val areaSqMeters = abs(result.area)
        val perimeterM = result.perimeter

        val acres = areaSqMeters * 0.00024710538
        val hectares = areaSqMeters / 10000.0
        val sqFeet = areaSqMeters * 10.76391

        return MeasurementResult(
            areaSquareMeters = areaSqMeters,
            acres = acres,
            hectares = hectares,
            squareFeet = sqFeet,
            perimeterMeters = perimeterM,
            isClosed = forceClosed || nodes.size >= 3
        )
    }

    private fun computePerimeter(nodes: List<LatLngNode>): Double {
        if (nodes.size < 2) return 0.0
        var totalDist = 0.0
        for (i in 0 until nodes.size - 1) {
            val g = Geodesic.WGS84.Inverse(
                nodes[i].latitude, nodes[i].longitude,
                nodes[i + 1].latitude, nodes[i + 1].longitude
            )
            totalDist += g.s12
        }
        return totalDist
    }

    fun calculateDistanceBetween(a: LatLngNode, b: LatLngNode): Double {
        return Geodesic.WGS84.Inverse(a.latitude, a.longitude, b.latitude, b.longitude).s12
    }
}
