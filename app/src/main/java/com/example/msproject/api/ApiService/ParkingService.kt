package com.example.msproject.com.example.msproject.api.ApiService

import com.example.msproject.com.example.msproject.model.ParkingLotInfo
import com.example.msproject.api.model.ParkingLotsResponse

interface ParkingService  {
    suspend fun getParkingLots(): ParkingLotsResponse?

    suspend fun getParkingLotInfo(name: String): ParkingLotInfo?

    suspend fun reserveParkingSpot(parkingLotName: String, deviceId: String, timeToReach: Long)

    suspend fun getDistanceMatrix(
        currentLocation: Pair<Double, Double>,
        destinationLocation: Pair<Double, Double>
    ): Double?
}