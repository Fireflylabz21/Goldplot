package com.geomeasure.landtracker.domain.filter

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Kalman smoothing directly in latitude/longitude degrees, with meter-based
 * accuracy converted independently for each coordinate axis.
 */
class LatitudeLongitudeKalmanFilter(
    private val processNoiseMetersPerSecond: Double = 1.0
) {
    private var timestampMs = 0L
    private var latitude = 0.0
    private var longitude = 0.0
    private var latitudeVariance = -1.0
    private var longitudeVariance = -1.0

    fun reset() {
        timestampMs = 0L
        latitude = 0.0
        longitude = 0.0
        latitudeVariance = -1.0
        longitudeVariance = -1.0
    }

    fun filter(
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        timeStampMs: Long
    ): Pair<Double, Double> {
        val accuracyMeters = max(accuracy.toDouble(), MINIMUM_ACCURACY_METERS)
        if (latitudeVariance < 0.0) {
            this.latitude = latitude
            this.longitude = longitude
            latitudeVariance = metersToLatitudeDegrees(accuracyMeters).let { it * it }
            longitudeVariance = metersToLongitudeDegrees(
                accuracyMeters,
                latitude
            ).let { it * it }
            timestampMs = timeStampMs
            return this.latitude to this.longitude
        }

        if (timeStampMs <= timestampMs) {
            return this.latitude to this.longitude
        }

        val deltaSeconds = (timeStampMs - timestampMs) / 1000.0
        timestampMs = timeStampMs

        val processNoiseMeters = processNoiseMetersPerSecond * sqrt(deltaSeconds)
        val latitudeProcessNoise = metersToLatitudeDegrees(processNoiseMeters)
        val longitudeProcessNoise = metersToLongitudeDegrees(
            processNoiseMeters,
            latitude
        )
        latitudeVariance += latitudeProcessNoise * latitudeProcessNoise
        longitudeVariance += longitudeProcessNoise * longitudeProcessNoise

        val latitudeMeasurementNoise = metersToLatitudeDegrees(accuracyMeters)
        val longitudeMeasurementNoise = metersToLongitudeDegrees(accuracyMeters, latitude)
        val latitudeMeasurementVariance = latitudeMeasurementNoise * latitudeMeasurementNoise
        val longitudeMeasurementVariance = longitudeMeasurementNoise * longitudeMeasurementNoise
        val latitudeGain = latitudeVariance /
            (latitudeVariance + latitudeMeasurementVariance)
        val longitudeGain = longitudeVariance /
            (longitudeVariance + longitudeMeasurementVariance)

        this.latitude += latitudeGain * (latitude - this.latitude)
        val longitudeDelta = wrapLongitudeDelta(longitude - this.longitude)
        this.longitude = wrapLongitude(this.longitude + longitudeGain * longitudeDelta)

        latitudeVariance = (1.0 - latitudeGain) * latitudeVariance
        longitudeVariance = (1.0 - longitudeGain) * longitudeVariance
        return this.latitude to this.longitude
    }

    private fun metersToLatitudeDegrees(meters: Double): Double =
        Math.toDegrees(meters / EARTH_RADIUS_METERS)

    private fun metersToLongitudeDegrees(meters: Double, atLatitude: Double): Double {
        val metersPerDegree = EARTH_RADIUS_METERS *
            max(abs(cos(Math.toRadians(atLatitude))), MINIMUM_POLE_SCALE) *
            PI / 180.0
        return meters / metersPerDegree
    }

    private fun wrapLongitudeDelta(delta: Double): Double =
        ((delta + 540.0) % 360.0) - 180.0

    private fun wrapLongitude(value: Double): Double =
        ((value + 540.0) % 360.0) - 180.0

    private companion object {
        const val EARTH_RADIUS_METERS = 6_371_008.8
        const val MINIMUM_ACCURACY_METERS = 1.0
        const val MINIMUM_POLE_SCALE = 1e-8
    }
}
