package com.geomeasure.landtracker.domain.gis

import com.geomeasure.landtracker.domain.model.LatLngNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeodesicAreaCalculatorTest {

    private lateinit var calculator: GeodesicAreaCalculator

    @Before
    fun setUp() {
        calculator = GeodesicAreaCalculator()
    }

    @Test
    fun testQuadrilateralAreaCalculation() {
        // ~100m x ~100m plot near equator (~10,000 m2)
        val nodes = listOf(
            LatLngNode(latitude = 0.0, longitude = 0.0),
            LatLngNode(latitude = 0.00090, longitude = 0.0),
            LatLngNode(latitude = 0.00090, longitude = 0.00090),
            LatLngNode(latitude = 0.0, longitude = 0.00090)
        )

        val result = calculator.computeMetrics(nodes, forceClosed = true)

        assertTrue(result.areaSquareMeters > 9500.0)
        assertTrue(result.areaSquareMeters < 10500.0)
        assertTrue(result.acres > 2.0)
    }

    @Test
    fun testDegeneratePolygonReturnsZeroArea() {
        val lineNodes = listOf(
            LatLngNode(latitude = 12.9716, longitude = 77.5946),
            LatLngNode(latitude = 12.9720, longitude = 77.5950)
        )

        val result = calculator.computeMetrics(lineNodes)
        assertEquals(0.0, result.areaSquareMeters, 0.001)
        assertTrue(result.perimeterMeters > 0.0)
    }
}
