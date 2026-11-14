package com.geomeasure.landtracker.di

import com.geomeasure.landtracker.data.export.GeoJsonSerializer
import com.geomeasure.landtracker.domain.filter.LatitudeLongitudeKalmanFilter
import com.geomeasure.landtracker.domain.filter.StaticPointAverager
import com.geomeasure.landtracker.domain.gis.GeodesicAreaCalculator
import com.geomeasure.landtracker.domain.gis.PolygonSnapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGeodesicCalculator(): GeodesicAreaCalculator = GeodesicAreaCalculator()

    @Provides
    @Singleton
    fun providePolygonSnapper(calculator: GeodesicAreaCalculator): PolygonSnapper =
        PolygonSnapper(calculator)

    @Provides
    @Singleton
    fun provideLatitudeLongitudeKalmanFilter(): LatitudeLongitudeKalmanFilter =
        LatitudeLongitudeKalmanFilter()

    @Provides
    @Singleton
    fun provideStaticPointAverager(): StaticPointAverager = StaticPointAverager()

    @Provides
    @Singleton
    fun provideGeoJsonSerializer(): GeoJsonSerializer = GeoJsonSerializer()
}
