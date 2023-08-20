package com.example.msproject.api.apiService

import com.example.msproject.api.model.ParkingLotsResponse
import com.example.msproject.com.example.msproject.model.ParkingLotInfo

interface ParkingService  {
    suspend fun getParkingLots(): ParkingLotsResponse?

    suspend fun getParkingLotInfo(name: String): ParkingLotInfo?

    suspend fun reserveParkingSpot(parkingLotName: String, deviceId: String, timeToReach: Long)

    suspend fun getETA(
        currentLocation: Pair<Double, Double>,
        destinationLocation: Pair<Double, Double>
    ): Double?
}