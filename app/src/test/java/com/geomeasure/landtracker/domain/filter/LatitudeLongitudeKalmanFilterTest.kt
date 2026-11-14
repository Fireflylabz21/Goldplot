package com.geomeasure.landtracker.domain.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos

class LatitudeLongitudeKalmanFilterTest {

    @Test
    fun filtersLatitudeAndLongitudeUsingTheirGroundScales() {
        val filter = LatitudeLongitudeKalmanFilter()
        val latitude = 60.0
        val longitude = 10.0
        val metersPerDegreeLatitude = 6_371_008.8 * PI / 180.0
        val metersPerDegreeLongitude =
            metersPerDegreeLatitude * cos(Math.toRadians(latitude))

        filter.filter(latitude, longitude, accuracy = 1f, timeStampMs = 1_000L)
        val result = filter.filter(
            latitude = latitude + 10.0 / metersPerDegreeLatitude,
            longitude = longitude + 10.0 / metersPerDegreeLongitude,
            accuracy = 1f,
            timeStampMs = 2_000L
        )

        val northMeters = (result.first - latitude) * metersPerDegreeLatitude
        val eastMeters = (result.second - longitude) * metersPerDegreeLongitude
        assertTrue(northMeters in 0.0..10.0)
        assertTrue(eastMeters in 0.0..10.0)
        assertEquals(northMeters, eastMeters, 0.01)
    }

    @Test
    fun handlesLongitudeAcrossTheDateLine() {
        val filter = LatitudeLongitudeKalmanFilter()
        filter.filter(0.0, 179.999, accuracy = 2f, timeStampMs = 1_000L)
        val result = filter.filter(0.0, -179.999, accuracy = 2f, timeStampMs = 2_000L)

        assertTrue(kotlin.math.abs(kotlin.math.abs(result.second) - 180.0) < 0.002)
    }

    @Test
    fun givesLowerWeightToLessAccurateFixes() {
        val latitude = 12.0
        val metersPerDegreeLatitude = 6_371_008.8 * PI / 180.0
        val preciseFilter = LatitudeLongitudeKalmanFilter()
        val uncertainFilter = LatitudeLongitudeKalmanFilter()
        preciseFilter.filter(latitude, 77.0, accuracy = 1f, timeStampMs = 1_000L)
        uncertainFilter.filter(latitude, 77.0, accuracy = 1f, timeStampMs = 1_000L)

        val precise = preciseFilter.filter(
            latitude + 10.0 / metersPerDegreeLatitude,
            77.0,
            accuracy = 1f,
            timeStampMs = 2_000L
        )
        val uncertain = uncertainFilter.filter(
            latitude + 10.0 / metersPerDegreeLatitude,
            77.0,
            accuracy = 3f,
            timeStampMs = 2_000L
        )

        assertTrue(precise.first > uncertain.first)
        assertTrue(uncertain.first > latitude)
    }

    @Test
    fun ignoresOutOfOrderFixes() {
        val filter = LatitudeLongitudeKalmanFilter()
        val first = filter.filter(12.0, 77.0, accuracy = 2f, timeStampMs = 2_000L)
        val outOfOrder = filter.filter(12.01, 77.01, accuracy = 1f, timeStampMs = 1_000L)

        assertEquals(first.first, outOfOrder.first, 0.0)
        assertEquals(first.second, outOfOrder.second, 0.0)
    }
}
