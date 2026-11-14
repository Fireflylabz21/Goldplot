package com.geomeasure.landtracker.presentation;

import com.geomeasure.landtracker.data.export.GeoJsonSerializer;
import com.geomeasure.landtracker.domain.filter.LatitudeLongitudeKalmanFilter;
import com.geomeasure.landtracker.domain.filter.StaticPointAverager;
import com.geomeasure.landtracker.domain.gis.GeodesicAreaCalculator;
import com.geomeasure.landtracker.domain.gis.PolygonSnapper;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class TrackingViewModel_Factory implements Factory<TrackingViewModel> {
  private final Provider<GeodesicAreaCalculator> calculatorProvider;

  private final Provider<PolygonSnapper> snapperProvider;

  private final Provider<LatitudeLongitudeKalmanFilter> latitudeLongitudeKalmanFilterProvider;

  private final Provider<StaticPointAverager> pointAveragerProvider;

  private final Provider<GeoJsonSerializer> serializerProvider;

  public TrackingViewModel_Factory(Provider<GeodesicAreaCalculator> calculatorProvider,
      Provider<PolygonSnapper> snapperProvider,
      Provider<LatitudeLongitudeKalmanFilter> latitudeLongitudeKalmanFilterProvider,
      Provider<StaticPointAverager> pointAveragerProvider,
      Provider<GeoJsonSerializer> serializerProvider) {
    this.calculatorProvider = calculatorProvider;
    this.snapperProvider = snapperProvider;
    this.latitudeLongitudeKalmanFilterProvider = latitudeLongitudeKalmanFilterProvider;
    this.pointAveragerProvider = pointAveragerProvider;
    this.serializerProvider = serializerProvider;
  }

  @Override
  public TrackingViewModel get() {
    return newInstance(calculatorProvider.get(), snapperProvider.get(), latitudeLongitudeKalmanFilterProvider.get(), pointAveragerProvider.get(), serializerProvider.get());
  }

  public static TrackingViewModel_Factory create(
      Provider<GeodesicAreaCalculator> calculatorProvider, Provider<PolygonSnapper> snapperProvider,
      Provider<LatitudeLongitudeKalmanFilter> latitudeLongitudeKalmanFilterProvider,
      Provider<StaticPointAverager> pointAveragerProvider,
      Provider<GeoJsonSerializer> serializerProvider) {
    return new TrackingViewModel_Factory(calculatorProvider, snapperProvider, latitudeLongitudeKalmanFilterProvider, pointAveragerProvider, serializerProvider);
  }

  public static TrackingViewModel newInstance(GeodesicAreaCalculator calculator,
      PolygonSnapper snapper, LatitudeLongitudeKalmanFilter latitudeLongitudeKalmanFilter,
      StaticPointAverager pointAverager, GeoJsonSerializer serializer) {
    return new TrackingViewModel(calculator, snapper, latitudeLongitudeKalmanFilter, pointAverager, serializer);
  }
}
