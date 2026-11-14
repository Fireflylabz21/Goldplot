# Implementation Plan - GPS Path Tracking Fix & Feature Enhancements

This plan outlines the steps to resolve GPS path tracking during walking surveys and implement the PDF Dossier Report Generator and ARCore AR Field Boundary Visualizer.

## Proposed Changes

### 1. GPS Path Tracking Fix
#### [MODIFY] [TrackingViewModel.kt](file:///C:/Users/Admin/Desktop/TerraM/app/src/main/java/com/geomeasure/landtracker/presentation/TrackingViewModel.kt)
- Review and adjust location acceptance criteria (speed, distance delta, accuracy threshold, or fallback behavior) so that walking path coordinates are correctly appended and tracked when recording is active.

### 2. Dossier PDF Report Generator
#### [NEW] [PdfDossierExporter.kt](file:///C:/Users/Admin/Desktop/TerraM/app/src/main/java/com/geomeasure/landtracker/data/export/PdfDossierExporter.kt)
- Generate branded PDF dossiers using native `PdfDocument` containing parcel map snapshots, GPS boundary coordinate tables, perimeter/area metrics, unit matrix, and property cost summary.

### 3. AR (Augmented Reality) Field Boundary Visualizer
#### [NEW] [ArBoundaryVisualizerActivity.kt](file:///C:/Users/Admin/Desktop/TerraM/app/src/main/java/com/geomeasure/landtracker/presentation/ar/ArBoundaryVisualizerActivity.kt)
- ARCore camera rendering view to overlay calculated plot boundary lines and corner markers on camera view.
