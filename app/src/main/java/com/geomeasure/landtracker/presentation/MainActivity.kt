package com.geomeasure.landtracker.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import com.geomeasure.landtracker.R
import com.geomeasure.landtracker.data.location.BoundaryTrackingService
import com.geomeasure.landtracker.presentation.ui.MainMeasurementScreen
import com.geomeasure.landtracker.presentation.ui.TerraMTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: TrackingViewModel by viewModels()
    private var userLocation by mutableStateOf<Location?>(null)
    private var pendingTrackingStart = false

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasAnyLocationPermission()) {
            updateUserLocation()
        }
    }

    private val trackingPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (pendingTrackingStart) {
            pendingTrackingStart = false
            if (hasFineLocationPermission()) {
                beginTracking()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

        setContent {
            TerraMTheme {
                var showSplash by rememberSaveable { mutableStateOf(savedInstanceState == null) }

                LaunchedEffect(Unit) {
                    if (showSplash) {
                        delay(1_100)
                        showSplash = false
                    }
                }

                if (showSplash) {
                    Image(
                        painter = painterResource(R.drawable.splash_screen),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val uiState by viewModel.uiState.collectAsState()

                    MainMeasurementScreen(
                        state = uiState,
                        userLocation = userLocation,
                        onLocateMe = {
                            if (hasAnyLocationPermission()) {
                                updateUserLocation()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        onStartTracking = {
                            requestTrackingPermissions()
                        },
                        onStopTracking = {
                            viewModel.stopTracking()
                        },
                        onAverageVertex = {
                            viewModel.startVertexAveraging()
                        },
                        onCancelAverageVertex = {
                            viewModel.cancelVertexAveraging()
                        },
                        onCloseLoop = {
                            viewModel.closePolygonLoop()
                        },
                        onReset = {
                            viewModel.reset()
                        },
                        onShare = {
                            val geoJson = viewModel.exportGeoJson()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/geo+json"
                                putExtra(Intent.EXTRA_SUBJECT, "TerraM land survey")
                                putExtra(Intent.EXTRA_TEXT, geoJson)
                            }
                            startActivity(
                                Intent.createChooser(shareIntent, "Share TerraM survey")
                            )
                        }
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val notGranted = locationPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) {
            updateUserLocation()
        } else {
            locationPermissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun requestTrackingPermissions() {
        pendingTrackingStart = true
        val permissions = mutableListOf<String>()
        if (!hasFineLocationPermission()) {
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }

        if (permissions.isEmpty()) {
            pendingTrackingStart = false
            beginTracking()
        } else {
            trackingPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun hasFineLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun hasAnyLocationPermission(): Boolean =
        hasFineLocationPermission() ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    private fun updateUserLocation() {
        if (!hasAnyLocationPermission()) return

        val locationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            locationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener(this) { location ->
                if (location != null) {
                    userLocation = location
                } else {
                    locationClient.lastLocation.addOnSuccessListener(this) {
                        userLocation = it
                    }.addOnFailureListener(this) {
                        Log.w(TAG, "Unable to retrieve the last known location", it)
                    }
                }
            }.addOnFailureListener(this) {
                Log.w(TAG, "Unable to retrieve the current location", it)
            }
        } catch (exception: SecurityException) {
            Log.w(TAG, "Location permission changed while retrieving the current location", exception)
        }
    }

    private fun beginTracking() {
        startTrackingService()
        viewModel.startTracking()
    }

    private fun startTrackingService() {
        val intent = Intent(this, BoundaryTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
