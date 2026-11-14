package com.geomeasure.landtracker.di;

import com.geomeasure.landtracker.data.export.GeoJsonSerializer;
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
public final class AppModule_ProvideGeoJsonSerializerFactory implements Factory<GeoJsonSerializer> {
  @Override
  public GeoJsonSerializer get() {
    return provideGeoJsonSerializer();
  }

  public static AppModule_ProvideGeoJsonSerializerFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static GeoJsonSerializer provideGeoJsonSerializer() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGeoJsonSerializer());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideGeoJsonSerializerFactory INSTANCE = new AppModule_ProvideGeoJsonSerializerFactory();
  }
}
