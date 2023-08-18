package com.example.msproject.com.example.msproject.ui.home

import com.example.msproject.com.example.msproject.api.ApiService.ParkingLotsApiService
import com.example.msproject.api.model.ParkingLotsResponse
import com.example.msproject.com.example.msproject.api.ApiService.ApiInterface
import com.example.msproject.com.example.msproject.api.ApiService.ApiInterfaceImpl
import com.example.msproject.com.example.msproject.model.ParkingLotInfo
import javax.inject.Inject

class HomeRepository @Inject constructor(parkingLotsApiService: ParkingLotsApiService) {

    private var apiInterface: ApiInterface? = null

    init {
        apiInterface = ApiInterfaceImpl(parkingLotsApiService)
    }

    suspend fun getParkingLots(): ParkingLotsResponse? {
        return apiInterface?.getParkingLots()
    }

    suspend fun getParkingLotInfo(name: String): ParkingLotInfo? {
        return apiInterface?.getParkingLotInfo(name)
    }

//    suspend fun deleteExpiredDocuments(): Unit? {
//        return apiInterface?.deleteExpiredDocuments()
//    }

    suspend fun reserveParkingSpot(
        parkingLotName: String,
        deviceId: String,
        timeToReach: Long
    ): Unit? {
        return apiInterface?.reserveParkingSpot(parkingLotName, deviceId, timeToReach)
    }

    fun getDistanceMatrix(
        currentLocation: Pair<Double, Double>,
        destinationLocation: Pair<Double, Double>
    ): Double? {
        return apiInterface?.getDistanceMatrix(currentLocation, destinationLocation)
    }
}