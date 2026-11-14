package com.geomeasure.landtracker.di;

import com.geomeasure.landtracker.domain.filter.StaticPointAverager;
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
public final class AppModule_ProvideStaticPointAveragerFactory implements Factory<StaticPointAverager> {
  @Override
  public StaticPointAverager get() {
    return provideStaticPointAverager();
  }

  public static AppModule_ProvideStaticPointAveragerFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static StaticPointAverager provideStaticPointAverager() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideStaticPointAverager());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideStaticPointAveragerFactory INSTANCE = new AppModule_ProvideStaticPointAveragerFactory();
  }
}
