package com.example.msproject.com.example.msproject.api.ApiService

import com.example.msproject.com.example.msproject.model.ParkingLotInfo
import com.example.msproject.api.model.ParkingLotsResponse
import javax.inject.Inject

class ApiInterfaceImpl @Inject constructor(private val parkingLotsApiService: ParkingLotsApiService) :
    ApiInterface {

    override suspend fun getParkingLots(): ParkingLotsResponse? =
        parkingLotsApiService.getParkingLots()

    override suspend fun getParkingLotInfo(name: String): ParkingLotInfo? =
        parkingLotsApiService.getParkingLotInfo(name)

//    override suspend fun deleteExpiredDocuments() = parkingLotsApiService.deleteExpiredDocuments()

    override suspend fun reserveParkingSpot(
        parkingLotName: String,
        deviceId: String,
        timeToReach: Long
    ) =
        parkingLotsApiService.reserveParkingSpot(parkingLotName, deviceId, timeToReach)

    override fun getDistanceMatrix(
        currentLocation: Pair<Double, Double>,
        destinationLocation: Pair<Double, Double>
    ): Double? = parkingLotsApiService.getDistanceMatrix(currentLocation, destinationLocation)
}