package com.geomeasure.landtracker.domain.filter

import com.geomeasure.landtracker.domain.model.LatLngNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StaticPointAveragerTest {

    @Test
    fun givesMoreWeightToTheMoreAccurateFix() {
        val result = StaticPointAverager().average(
            listOf(
                LatLngNode(
                    latitude = 0.0,
                    longitude = 0.0,
                    accuracy = 1f,
                    timestamp = 1L
                ),
                LatLngNode(
                    latitude = 0.0,
                    longitude = 0.001,
                    accuracy = 3f,
                    timestamp = 2L
                )
            )
        )

        assertNotNull(result)
        assertEquals(0.0001, result!!.longitude, 1e-8)
        assertEquals(1.2f, result.accuracy, 1e-6f)
        assertEquals(2L, result.timestamp)
    }

    @Test
    fun handlesLongitudesAcrossTheDateLine() {
        val result = StaticPointAverager().average(
            listOf(
                LatLngNode(latitude = 0.0, longitude = 179.999, accuracy = 2f),
                LatLngNode(latitude = 0.0, longitude = -179.999, accuracy = 2f)
            )
        )

        assertNotNull(result)
        assertEquals(180.0, kotlin.math.abs(result!!.longitude), 1e-6)
    }
}
