package com.example.msproject.common

import com.example.msproject.api.apiService.ParkingService
import com.example.msproject.api.apiService.ParkingServiceImpl
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
    fun provideApiService(): ParkingService = ParkingServiceImpl()

}