package com.geomeasure.landtracker.di;

import com.geomeasure.landtracker.domain.filter.KalmanLocationFilter;
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
public final class AppModule_ProvideKalmanFilterFactory implements Factory<KalmanLocationFilter> {
  @Override
  public KalmanLocationFilter get() {
    return provideKalmanFilter();
  }

  public static AppModule_ProvideKalmanFilterFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static KalmanLocationFilter provideKalmanFilter() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideKalmanFilter());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideKalmanFilterFactory INSTANCE = new AppModule_ProvideKalmanFilterFactory();
  }
}
