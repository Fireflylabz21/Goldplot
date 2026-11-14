package com.geomeasure.landtracker.presentation

import android.location.Location
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geomeasure.landtracker.data.export.GeoJsonSerializer
import com.geomeasure.landtracker.data.location.BoundaryTrackingService
import com.geomeasure.landtracker.domain.filter.LatitudeLongitudeKalmanFilter
import com.geomeasure.landtracker.domain.filter.StaticPointAverager
import com.geomeasure.landtracker.domain.gis.GeodesicAreaCalculator
import com.geomeasure.landtracker.domain.gis.PolygonSnapper
import com.geomeasure.landtracker.domain.model.LatLngNode
import com.geomeasure.landtracker.domain.model.MeasurementResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackingUiState(
    val isTracking: Boolean = false,
    val nodes: List<LatLngNode> = emptyList(),
    val metrics: MeasurementResult = MeasurementResult(),
    val currentAccuracy: Float = 0f,
    val accuracyThresholdMeters: Float = MAX_HORIZONTAL_ACCURACY_METERS,
    val hasGoodFix: Boolean = false,
    val gpsStatus: String = "Waiting for GPS fix",
    val isAveragingVertex: Boolean = false,
    val averagingSampleCount: Int = 0,
    val averagingDurationSeconds: Long = 0,
    val averagingStatus: String = "",
    val isNearOriginForSnap: Boolean = false,
    val isClosed: Boolean = false
) {
    companion object {
        const val MAX_HORIZONTAL_ACCURACY_METERS = 3.0f
    }
}

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val calculator: GeodesicAreaCalculator,
    private val snapper: PolygonSnapper,
    private val latitudeLongitudeKalmanFilter: LatitudeLongitudeKalmanFilter,
    private val pointAverager: StaticPointAverager,
    private val serializer: GeoJsonSerializer
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()
    private var lastAcceptedTimestampMs = 0L
    private var lastAcceptedRawLocation: Location? = null
    private val vertexSamples = mutableListOf<LatLngNode>()

    init {
        observeRawLocations()
    }

    private fun observeRawLocations() {
        viewModelScope.launch {
            BoundaryTrackingService.rawLocationFlow.collect { loc ->
                processLocationUpdate(loc)
            }
        }
    }

    fun processLocationUpdate(location: Location) {
        if (!_uiState.value.isTracking || _uiState.value.isClosed) return

        if (
            !location.hasAccuracy() ||
            !location.accuracy.isFinite() ||
            location.accuracy <= 0f ||
            location.accuracy > TrackingUiState.MAX_HORIZONTAL_ACCURACY_METERS
        ) {
            _uiState.update {
                it.copy(
                    currentAccuracy = location.accuracy,
                    hasGoodFix = false,
                    gpsStatus = "Waiting for GPS accuracy ≤ ${TrackingUiState.MAX_HORIZONTAL_ACCURACY_METERS.toInt()} m"
                )
            }
            return
        }

        val filterTimestampMs = location.elapsedRealtimeNanos
            .takeIf { it > 0L }
            ?.div(NANOS_PER_MILLISECOND)
            ?: location.time
        val fixAgeMs = if (location.elapsedRealtimeNanos > 0L) {
            (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) /
                NANOS_PER_MILLISECOND
        } else {
            0L
        }
        val coordinatesValid =
            location.latitude.isFinite() &&
                location.longitude.isFinite() &&
                location.latitude in -90.0..90.0 &&
                location.longitude in -180.0..180.0
        val speedValid = !location.hasSpeed() ||
            (location.speed.isFinite() &&
                location.speed <= MAX_SURVEY_SPEED_METERS_PER_SECOND)
        val implausibleJump = coordinatesValid && isImplausibleJump(location, filterTimestampMs)
        if (
            filterTimestampMs <= lastAcceptedTimestampMs ||
            fixAgeMs > MAX_FIX_AGE_MS ||
            fixAgeMs < -MAX_FUTURE_FIX_AGE_MS ||
            !coordinatesValid ||
            !speedValid ||
            implausibleJump
        ) {
            _uiState.update {
                it.copy(
                    currentAccuracy = location.accuracy,
                    hasGoodFix = false,
                    gpsStatus = when {
                        fixAgeMs > MAX_FIX_AGE_MS -> "Ignoring stale GPS fix"
                        fixAgeMs < -MAX_FUTURE_FIX_AGE_MS -> "Ignoring invalid GPS timestamp"
                        filterTimestampMs <= lastAcceptedTimestampMs ->
                            "Ignoring out-of-order GPS fix"
                        !coordinatesValid -> "Ignoring invalid GPS coordinates"
                        !speedValid -> "GPS speed exceeds survey limit"
                        else -> "Ignoring GPS jump · waiting for stable fix"
                    }
                )
            }
            return
        }

        val (filteredLat, filteredLng) = latitudeLongitudeKalmanFilter.filter(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            timeStampMs = filterTimestampMs
        )

        val newNode = LatLngNode(
            latitude = filteredLat,
            longitude = filteredLng,
            altitude = location.altitude,
            accuracy = location.accuracy,
            timestamp = location.time
        )

        lastAcceptedTimestampMs = filterTimestampMs
        lastAcceptedRawLocation = Location(location)
        if (_uiState.value.isAveragingVertex) {
            collectAveragedVertex(location)
            return
        }

        val updatedList = _uiState.value.nodes.toMutableList()

        if (updatedList.isNotEmpty()) {
            val dist = calculator.calculateDistanceBetween(updatedList.last(), newNode)
            if (dist < 0.2) {
                return
            }
        }

        val canSnap = snapper.shouldSnapToOrigin(newNode, updatedList)

        updatedList.add(newNode)
        val metrics = calculator.computeMetrics(updatedList)

        _uiState.update {
            it.copy(
                nodes = updatedList,
                metrics = metrics,
                currentAccuracy = location.accuracy,
                hasGoodFix = true,
                gpsStatus = "Good GPS fix",
                isNearOriginForSnap = canSnap
            )
        }
    }

    fun startTracking() {
        latitudeLongitudeKalmanFilter.reset()
        lastAcceptedTimestampMs = 0L
        lastAcceptedRawLocation = null
        vertexSamples.clear()
        _uiState.update { it.copy(isTracking = true, isClosed = false) }
    }

    fun stopTracking() {
        cancelVertexAveraging()
        _uiState.update { it.copy(isTracking = false) }
    }

    fun startVertexAveraging() {
        if (!_uiState.value.isTracking || _uiState.value.isClosed) return
        vertexSamples.clear()
        _uiState.update {
            it.copy(
                isAveragingVertex = true,
                averagingSampleCount = 0,
                averagingDurationSeconds = 0,
                averagingStatus = "Stand still for 60 seconds"
            )
        }
    }

    fun cancelVertexAveraging() {
        vertexSamples.clear()
        _uiState.update {
            it.copy(
                isAveragingVertex = false,
                averagingSampleCount = 0,
                averagingDurationSeconds = 0,
                averagingStatus = ""
            )
        }
    }

    fun closePolygonLoop() {
        if (_uiState.value.nodes.size >= 3) {
            cancelVertexAveraging()
            val finalizedMetrics = calculator.computeMetrics(_uiState.value.nodes, forceClosed = true)
            _uiState.update {
                it.copy(
                    isClosed = true,
                    isTracking = false,
                    metrics = finalizedMetrics
                )
            }
        }
    }

    fun reset() {
        latitudeLongitudeKalmanFilter.reset()
        lastAcceptedTimestampMs = 0L
        lastAcceptedRawLocation = null
        vertexSamples.clear()
        _uiState.value = TrackingUiState()
    }

    fun exportGeoJson(): String {
        return serializer.serialize(_uiState.value.nodes, _uiState.value.metrics)
    }

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000L
        const val MAX_FIX_AGE_MS = 10_000L
        const val MAX_FUTURE_FIX_AGE_MS = 1_000L
        const val MAX_SURVEY_SPEED_METERS_PER_SECOND = 12f
        const val MAX_STATIONARY_SPEED_METERS_PER_SECOND = 0.75f
        const val MAX_STATIONARY_CLUSTER_RADIUS_METERS = 5.0
        const val REQUIRED_VERTEX_SAMPLES = 60
        const val REQUIRED_VERTEX_DURATION_MS = 60_000L
    }

    private fun isImplausibleJump(location: Location, timestampMs: Long): Boolean {
        val previous = lastAcceptedRawLocation ?: return false
        val elapsedSeconds = (timestampMs - lastAcceptedTimestampMs) / 1_000.0
        if (elapsedSeconds <= 0.0) return true

        val distance = calculator.calculateDistanceBetween(
            LatLngNode(
                latitude = previous.latitude,
                longitude = previous.longitude,
                accuracy = previous.accuracy
            ),
            LatLngNode(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy
            )
        )
        val uncertaintyAllowance =
            previous.accuracy.toDouble() + location.accuracy.toDouble()
        val plausibleDistance =
            MAX_SURVEY_SPEED_METERS_PER_SECOND * elapsedSeconds + uncertaintyAllowance
        return distance > plausibleDistance
    }

    private fun collectAveragedVertex(location: Location) {
        if (
            location.hasSpeed() &&
            (!location.speed.isFinite() ||
                location.speed > MAX_STATIONARY_SPEED_METERS_PER_SECOND)
        ) {
            vertexSamples.clear()
            _uiState.update {
                it.copy(
                    currentAccuracy = location.accuracy,
                    hasGoodFix = true,
                    averagingSampleCount = 0,
                    averagingDurationSeconds = 0,
                    averagingStatus = "Moving detected · hold still to restart"
                )
            }
            return
        }

        val sample = LatLngNode(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            accuracy = location.accuracy,
            timestamp = location.time
        )
        val firstSample = vertexSamples.firstOrNull()
        if (
            firstSample != null &&
            calculator.calculateDistanceBetween(firstSample, sample) >
            MAX_STATIONARY_CLUSTER_RADIUS_METERS
        ) {
            vertexSamples.clear()
        }
        vertexSamples += sample

        val durationMs = vertexSamples.last().timestamp - vertexSamples.first().timestamp
        _uiState.update {
            it.copy(
                currentAccuracy = location.accuracy,
                hasGoodFix = true,
                averagingSampleCount = vertexSamples.size,
                averagingDurationSeconds = durationMs / 1_000L,
                averagingStatus = "Hold still · ${vertexSamples.size}/60 good fixes"
            )
        }

        if (
            vertexSamples.size >= REQUIRED_VERTEX_SAMPLES &&
            durationMs >= REQUIRED_VERTEX_DURATION_MS
        ) {
            val averagedPoint = pointAverager.average(vertexSamples) ?: return
            val updatedNodes = _uiState.value.nodes + averagedPoint
            latitudeLongitudeKalmanFilter.reset()
            latitudeLongitudeKalmanFilter.filter(
                averagedPoint.latitude,
                averagedPoint.longitude,
                averagedPoint.accuracy,
                lastAcceptedTimestampMs
            )
            vertexSamples.clear()
            _uiState.update {
                it.copy(
                    nodes = updatedNodes,
                    metrics = calculator.computeMetrics(updatedNodes),
                    isAveragingVertex = false,
                    averagingSampleCount = 0,
                    averagingDurationSeconds = 0,
                    averagingStatus = "",
                    gpsStatus = "Good GPS fix · vertex averaged",
                    isNearOriginForSnap = snapper.shouldSnapToOrigin(
                        averagedPoint,
                        updatedNodes.dropLast(1)
                    )
                )
            }
        }
    }
}
