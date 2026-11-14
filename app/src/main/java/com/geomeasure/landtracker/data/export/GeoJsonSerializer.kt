package com.geomeasure.landtracker.data.export

import com.geomeasure.landtracker.domain.model.LatLngNode
import com.geomeasure.landtracker.domain.model.MeasurementResult
import org.json.JSONArray
import org.json.JSONObject

class GeoJsonSerializer {

    fun serialize(nodes: List<LatLngNode>, metrics: MeasurementResult, title: String = "Measured Parcel"): String {
        val feature = JSONObject()
        feature.put("type", "Feature")

        val properties = JSONObject().apply {
            put("title", title)
            put("area_sq_meters", metrics.areaSquareMeters)
            put("area_acres", metrics.acres)
            put("area_hectares", metrics.hectares)
            put("perimeter_meters", metrics.perimeterMeters)
            put("created_at", System.currentTimeMillis())
        }
        feature.put("properties", properties)

        val geometry = JSONObject()
        val coordinates = JSONArray()
        val ring = JSONArray()

        nodes.forEach { node ->
            val point = JSONArray()
            point.put(node.longitude)
            point.put(node.latitude)
            point.put(node.altitude)
            ring.put(point)
        }

        // Close polygon if at least 3 points exist
        if (nodes.size >= 3) {
            val first = nodes.first()
            val closePt = JSONArray()
            closePt.put(first.longitude)
            closePt.put(first.latitude)
            closePt.put(first.altitude)
            ring.put(closePt)
            geometry.put("type", "Polygon")
            coordinates.put(ring)
        } else {
            geometry.put("type", "LineString")
            geometry.put("coordinates", ring)
        }

        if (geometry.getString("type") == "Polygon") {
            geometry.put("coordinates", coordinates)
        }

        feature.put("geometry", geometry)
        return feature.toString(2)
    }
}
