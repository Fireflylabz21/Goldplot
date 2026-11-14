package com.geomeasure.landtracker.di;

import com.geomeasure.landtracker.domain.gis.GeodesicAreaCalculator;
import com.geomeasure.landtracker.domain.gis.PolygonSnapper;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class AppModule_ProvidePolygonSnapperFactory implements Factory<PolygonSnapper> {
  private final Provider<GeodesicAreaCalculator> calculatorProvider;

  public AppModule_ProvidePolygonSnapperFactory(
      Provider<GeodesicAreaCalculator> calculatorProvider) {
    this.calculatorProvider = calculatorProvider;
  }

  @Override
  public PolygonSnapper get() {
    return providePolygonSnapper(calculatorProvider.get());
  }

  public static AppModule_ProvidePolygonSnapperFactory create(
      Provider<GeodesicAreaCalculator> calculatorProvider) {
    return new AppModule_ProvidePolygonSnapperFactory(calculatorProvider);
  }

  public static PolygonSnapper providePolygonSnapper(GeodesicAreaCalculator calculator) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.providePolygonSnapper(calculator));
  }
}
