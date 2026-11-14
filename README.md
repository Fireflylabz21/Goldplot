# GeoMeasure - High-Accuracy Land Measurement Android App

Native Kotlin 2.0 application built with Jetpack Compose, GeographicLib, and Clean Architecture.

## Features Included:
- **Foreground GNSS Service:** Continuous updates with persistent notification.
- **Kalman Filtering:** Rejection of low-accuracy GPS spikes & micro-drift smoothing.
- **WGS84 Ellipsoidal Area:** High-precision Karney's formula via GeographicLib.
- **Dual View:** Vector tile map layer & offline pixel grid Canvas diagram.
- **Export Engine:** GeoJSON serialization and shape metrics.

## How to Run:
1. Open Android Studio (Ladybug / Koala or newer).
2. Choose **Open an Existing Project** and select this directory.
3. Sync Gradle and run on a device or emulator. TerraM requests location access when opened to center the vector map on the device; fine location and notification access are requested when tracking starts.

Camera and accelerometer permissions are not requested because the app does not use those device features. GPS accuracy is determined by the device and signal conditions; map zoom does not improve the location fix.