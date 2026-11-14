package com.geomeasure.landtracker.domain.filter

import com.geomeasure.landtracker.domain.model.LatLngNode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

class StaticPointAverager {

    fun average(samples: List<LatLngNode>): LatLngNode? {
        if (samples.isEmpty()) return null

        val origin = samples.first()
        val originLatitudeRadians = Math.toRadians(origin.latitude)
        val eastScale = EARTH_RADIUS_METERS * cos(originLatitudeRadians)
        var totalWeight = 0.0
        var weightedEast = 0.0
        var weightedNorth = 0.0
        var weightedAccuracy = 0.0

        samples.forEach { sample ->
            val accuracy = sample.accuracy.takeIf { it > 0f }?.toDouble()
                ?: MINIMUM_ACCURACY_METERS
            val weight = 1.0 / (accuracy * accuracy)
            val longitudeDelta = wrapRadians(
                Math.toRadians(sample.longitude - origin.longitude)
            )
            weightedEast += longitudeDelta * eastScale * weight
            weightedNorth +=
                (Math.toRadians(sample.latitude) - originLatitudeRadians) *
                    EARTH_RADIUS_METERS * weight
            weightedAccuracy += accuracy * weight
            totalWeight += weight
        }

        val latitude = Math.toDegrees(
            originLatitudeRadians + weightedNorth / totalWeight / EARTH_RADIUS_METERS
        )
        val longitude = if (abs(eastScale) < MINIMUM_EAST_SCALE_METERS) {
            origin.longitude
        } else {
            wrapLongitude(
                origin.longitude + Math.toDegrees(weightedEast / totalWeight / eastScale)
            )
        }

        return LatLngNode(
            latitude = latitude,
            longitude = longitude,
            altitude = samples.map { it.altitude }.average(),
            accuracy = (weightedAccuracy / totalWeight).toFloat(),
            timestamp = samples.last().timestamp
        )
    }

    private fun wrapRadians(value: Double): Double = ((value + 3.0 * PI) % (2.0 * PI)) - PI

    private fun wrapLongitude(value: Double): Double =
        ((value + 540.0) % 360.0) - 180.0

    private companion object {
        const val EARTH_RADIUS_METERS = 6_371_008.8
        const val MINIMUM_ACCURACY_METERS = 1.0
        const val MINIMUM_EAST_SCALE_METERS = 1.0
    }
}
