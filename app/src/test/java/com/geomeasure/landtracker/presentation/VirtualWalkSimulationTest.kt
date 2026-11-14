package com.geomeasure.landtracker.presentation

import android.location.Location
import com.geomeasure.landtracker.data.export.GeoJsonSerializer
import com.geomeasure.landtracker.domain.filter.LatitudeLongitudeKalmanFilter
import com.geomeasure.landtracker.domain.filter.StaticPointAverager
import com.geomeasure.landtracker.domain.gis.GeodesicAreaCalculator
import com.geomeasure.landtracker.domain.gis.PolygonSnapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VirtualWalkSimulationTest {

    private lateinit var viewModel: TrackingViewModel
    private val calculator = GeodesicAreaCalculator()
    private val snapper = PolygonSnapper(calculator)
    private val serializer = GeoJsonSerializer()

    @Before
    fun setUp() {
        viewModel = TrackingViewModel(
            calculator,
            snapper,
            LatitudeLongitudeKalmanFilter(),
            StaticPointAverager(),
            serializer
        )
    }

    @Test
    fun simulateUserWalkAndMeasurement() {
        // Start tracking session
        viewModel.startTracking()
        assertTrue(viewModel.uiState.value.isTracking)

        // Simulate walking along a rectangular perimeter (4 corners)
        val walkPath = listOf(
            Triple(12.9716, 77.5946, 2.0f), // Corner 1
            Triple(12.9725, 77.5946, 1.5f), // Corner 2 (North)
            Triple(12.9725, 77.5955, 1.8f), // Corner 3 (East)
            Triple(12.9716, 77.5955, 2.0f)  // Corner 4 (South)
        )

        val baseTime = System.currentTimeMillis()
        walkPath.forEachIndexed { index, (lat, lng, acc) ->
            val loc = Location("gps").apply {
                latitude = lat
                longitude = lng
                accuracy = acc
                time = baseTime + (index * 60_000L)
            }
            viewModel.processLocationUpdate(loc)
        }

        val state = viewModel.uiState.value
        
        // Verify nodes recorded
        assertEquals(4, state.nodes.size)

        // Verify geodesic calculations
        assertTrue("Area should be greater than 0", state.metrics.areaSquareMeters > 0.0)
        assertTrue("Perimeter should be greater than 0", state.metrics.perimeterMeters > 0.0)
        assertTrue("Acres should be computed", state.metrics.acres > 0.0)

        // Close polygon loop
        viewModel.closePolygonLoop()
        val closedState = viewModel.uiState.value
        assertTrue(closedState.isClosed)
        assertTrue(closedState.metrics.isClosed)

        // Test GeoJSON export
        val geoJson = viewModel.exportGeoJson()
        assertTrue(geoJson.contains("Polygon"))
        assertTrue(geoJson.contains("area_sq_meters"))
    }
}
