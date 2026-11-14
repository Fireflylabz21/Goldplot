package com.geomeasure.landtracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.geomeasure.landtracker.data.location.BoundaryTrackingService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GeoMeasureApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            BoundaryTrackingService.CHANNEL_ID,
            "Land Tracking Service Channel",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Used for active GPS boundary measurement foreground service"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }
}
