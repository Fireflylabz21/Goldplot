package com.geomeasure.landtracker.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.geomeasure.landtracker.presentation.TrackingUiState

@Composable
fun MeasurementDashboardOverlay(
    state: TrackingUiState,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    onAverageVertex: () -> Unit,
    onCancelAverageVertex: () -> Unit,
    onCloseLoop: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when {
                            state.isClosed -> "BOUNDARY COMPLETE"
                            state.isTracking -> "SURVEY IN PROGRESS"
                            state.nodes.isNotEmpty() -> "SURVEY PAUSED"
                            else -> "READY TO MEASURE"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = when {
                            state.hasGoodFix -> "±%.2f m".format(state.currentAccuracy)
                            state.currentAccuracy > 0f ->
                                "Weak fix · ±%.2f m".format(state.currentAccuracy)
                            else -> state.gpsStatus
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (state.hasGoodFix)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    if (!state.hasGoodFix) {
                        Text(
                            text = state.gpsStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "POINTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = state.nodes.size.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricItem(
                    label = "AREA",
                    value = "%.2f m²".format(state.metrics.areaSquareMeters),
                    modifier = Modifier.weight(1.3f),
                    emphasized = true
                )
                MetricItem(
                    label = "ACRES",
                    value = "%.4f".format(state.metrics.acres),
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "PERIMETER",
                    value = "%.1f m".format(state.metrics.perimeterMeters),
                    modifier = Modifier.weight(1f)
                )
            }

            if (state.isTracking && !state.isClosed) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = if (state.isAveragingVertex) {
                        onCancelAverageVertex
                    } else {
                        onAverageVertex
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (state.isAveragingVertex) {
                            "Cancel point averaging"
                        } else {
                            "Average next vertex · 60 sec"
                        }
                    )
                }
                Text(
                    text = if (state.isAveragingVertex) {
                        "${state.averagingStatus} · ${state.averagingDurationSeconds}/60 sec"
                    } else {
                        "Pause on the corner; only fixes within 3 m accuracy are averaged."
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!state.isTracking) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onStartTracking,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Start survey")
                    }
                } else {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onStopTracking,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Pause")
                    }

                    if (state.nodes.size >= 3 && !state.isClosed) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = onCloseLoop,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("Close boundary")
                        }
                    }
                }

                OutlinedButton(
                    onClick = onReset,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = if (emphasized) MaterialTheme.typography.titleLarge
            else MaterialTheme.typography.titleSmall,
            color = if (emphasized) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}
