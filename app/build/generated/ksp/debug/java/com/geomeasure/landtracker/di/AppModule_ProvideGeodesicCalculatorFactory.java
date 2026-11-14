package com.geomeasure.landtracker.di;

import com.geomeasure.landtracker.domain.gis.GeodesicAreaCalculator;
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
public final class AppModule_ProvideGeodesicCalculatorFactory implements Factory<GeodesicAreaCalculator> {
  @Override
  public GeodesicAreaCalculator get() {
    return provideGeodesicCalculator();
  }

  public static AppModule_ProvideGeodesicCalculatorFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static GeodesicAreaCalculator provideGeodesicCalculator() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGeodesicCalculator());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideGeodesicCalculatorFactory INSTANCE = new AppModule_ProvideGeodesicCalculatorFactory();
  }
}
