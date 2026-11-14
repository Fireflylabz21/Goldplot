package com.geomeasure.landtracker.domain.model

import java.util.UUID

/**
 * Immutable spatial coordinate node representing a polygon vertex.
 */
data class LatLngNode(
    val id: String = UUID.randomUUID().toString(),
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class MeasurementResult(
    val areaSquareMeters: Double = 0.0,
    val acres: Double = 0.0,
    val hectares: Double = 0.0,
    val squareFeet: Double = 0.0,
    val perimeterMeters: Double = 0.0,
    val isClosed: Boolean = false
)
