package com.geomeasure.landtracker.di;

import com.geomeasure.landtracker.domain.filter.LatitudeLongitudeKalmanFilter;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvideLatitudeLongitudeKalmanFilterFactory implements Factory<LatitudeLongitudeKalmanFilter> {
  @Override
  public LatitudeLongitudeKalmanFilter get() {
    return provideLatitudeLongitudeKalmanFilter();
  }

  public static AppModule_ProvideLatitudeLongitudeKalmanFilterFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static LatitudeLongitudeKalmanFilter provideLatitudeLongitudeKalmanFilter() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideLatitudeLongitudeKalmanFilter());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideLatitudeLongitudeKalmanFilterFactory INSTANCE = new AppModule_ProvideLatitudeLongitudeKalmanFilterFactory();
  }
}
