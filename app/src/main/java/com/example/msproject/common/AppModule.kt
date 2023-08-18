package com.example.msproject.com.example.msproject.common

import com.example.msproject.com.example.msproject.api.ApiService.ParkingLotsApiService
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
    fun provideApiService(): ParkingLotsApiService = ParkingLotsApiService()

}