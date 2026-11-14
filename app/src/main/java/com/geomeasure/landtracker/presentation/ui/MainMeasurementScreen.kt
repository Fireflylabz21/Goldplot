package com.geomeasure.landtracker.presentation.ui

import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.geomeasure.landtracker.R
import com.geomeasure.landtracker.presentation.TrackingUiState

private enum class MainTab { MAP, CANVAS, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMeasurementScreen(
    state: TrackingUiState,
    userLocation: Location?,
    onLocateMe: () -> Unit,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    onAverageVertex: () -> Unit,
    onCancelAverageVertex: () -> Unit,
    onCloseLoop: () -> Unit,
    onReset: () -> Unit,
    onShare: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.MAP) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = selectedTab == MainTab.MAP,
                    onClick = { selectedTab = MainTab.MAP },
                    icon = { Icon(Icons.Filled.Place, contentDescription = null) },
                    label = { Text("Vector map") }
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.CANVAS,
                    onClick = { selectedTab = MainTab.CANVAS },
                    icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    label = { Text("2D canvas") }
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.SETTINGS,
                    onClick = { selectedTab = MainTab.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(R.drawable.terra_icon_art),
                                contentDescription = "TerraM logo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(11.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column {
                            Text(
                                text = "TerraM",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "GPS LAND SURVEY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showAbout = true }) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "About TerraM"
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share survey as GeoJSON"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            when (selectedTab) {
                MainTab.MAP, MainTab.CANVAS -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        if (selectedTab == MainTab.MAP) {
                            MapLibreViewContainer(
                                nodes = state.nodes,
                                isTrackingActive = state.isTracking,
                                userLocation = userLocation,
                                onLocateMe = onLocateMe
                            )
                        } else {
                            DiagramCanvasView(nodes = state.nodes, isClosed = state.isClosed)
                        }
                    }

                    MeasurementDashboardOverlay(
                        state = state,
                        onStartTracking = onStartTracking,
                        onStopTracking = onStopTracking,
                        onAverageVertex = onAverageVertex,
                        onCancelAverageVertex = onCancelAverageVertex,
                        onCloseLoop = onCloseLoop,
                        onReset = onReset
                    )
                }
                MainTab.SETTINGS -> SettingsScreen(
                    onAbout = { showAbout = true }
                )
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About TerraM") },
            text = {
                Text(
                    "TerraM is a GPS land-survey tool for recording boundary points, " +
                        "calculating area and perimeter, and sharing survey data as GeoJSON."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("Done")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsScreen(
    onAbout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        SettingsItem(
            title = "Measurement units",
            detail = "Metric · square meters, acres, and meters"
        )
        SettingsItem(
            title = "Vector map",
            detail = "MapLibre demo vector tiles · internet connection required"
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text("GPS smoothing", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "GPS positions are filtered as latitude/longitude coordinates using each fix's reported accuracy. Survey distance and area use geodesic calculations on those coordinates.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            TextButton(
                onClick = onAbout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text("About TerraM", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SettingsItem(title: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
