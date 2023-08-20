package com.example.msproject.ui.home

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.msproject.api.apiService.LoadingState
import com.example.msproject.api.apiService.ParkingService
import com.example.msproject.api.model.ParkingLot
import com.example.msproject.api.model.ParkingLotsResponse
import com.google.android.gms.maps.GoogleMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import java.io.IOException
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class HomeViewModel  @Inject constructor(private val parkingService: ParkingService) :
    ViewModel() {

    var nearestParkingLotLiveData: MutableLiveData<ParkingLot>? = MutableLiveData<ParkingLot>()
    var parkingLotWeightsLiveData: MutableLiveData<MutableList<Pair<ParkingLot, Double>>>? = MutableLiveData<MutableList<Pair<ParkingLot, Double>>>()
    var durationInSecLiveData: MutableLiveData<Double>? = MutableLiveData<Double>()
    var loadingStateLiveData: MutableLiveData<LoadingState> = MutableLiveData<LoadingState>()
    private val parkingLotUsersCount: MutableMap<String, MutableSet<String>> = mutableMapOf()

    private var destinationLocation: Pair<Double, Double>? = Pair(0.0 , 0.0)
    
    private val exceptionHandler = CoroutineExceptionHandler { _, _ ->
        loadingStateLiveData.postValue(LoadingState.FAILURE)
    }

    fun getParkingLots(
        currentLocation: Pair<Double, Double>,
        showProgressLoader: Boolean
    ) {
        if (showProgressLoader) {
            loadingStateLiveData.postValue(LoadingState.LOADING)
        }
        viewModelScope.launch(exceptionHandler) {
            withContext(Dispatchers.IO) {
                try {
                    val parkingLotsResp = parkingService?.getParkingLots()
                    if (parkingLotsResp != null) {
                        if (showProgressLoader) {
                            loadingStateLiveData.postValue(LoadingState.SUCCESS)
                        }
                        getNumberOfUsersForParkingLots(parkingLotsResp)
                        getNearestParkingLot(parkingLotsResp, currentLocation)
                    } else {
                        if (showProgressLoader) {
                            loadingStateLiveData.postValue(LoadingState.FAILURE)
                        }
                    }
                } catch (exception: Exception) {
                    if (showProgressLoader) {
                        loadingStateLiveData.postValue(LoadingState.FAILURE)
                    }
                    Log.e("getParkingLots" , "$exception")
                }
            }
        }
    }

    private fun getNumberOfUsersForParkingLots(parkingLotsResponse: ParkingLotsResponse) {
        parkingLotUsersCount.clear()

        for (parkingLot in parkingLotsResponse.parkingLots) {
            val parkingLotName = parkingLot.parking_lot_name
            viewModelScope.launch(exceptionHandler) {
                try {
                    val parkingLotInfoResp = parkingService?.getParkingLotInfo(parkingLotName)
                    if (parkingLotInfoResp != null) {
                        val numberOfUsers = parkingLotInfoResp.numberOfUsers
                        parkingLotUsersCount[parkingLotName] =
                            (parkingLotUsersCount.getOrDefault(
                                parkingLotName,
                                mutableSetOf()
                            ) + numberOfUsers) as MutableSet<String>
                    }
                } catch (exception: Exception) {

                    Log.e("getNumberOfUsersForParkingLots", "$exception")
                }
            }
        }
    }

    fun reserveParkingSpot(parkingLotName: String, deviceId: String, timeToReach: Long) {
        viewModelScope.launch(exceptionHandler) {
            withContext(Dispatchers.IO) {
                try {
                    parkingService.reserveParkingSpot(parkingLotName, deviceId, timeToReach)
                }catch (exception : Exception){
                    Log.e("reserveParkingSpot" , "$exception")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    suspend fun getNearestParkingLot(
        apiResponse: ParkingLotsResponse,
        currentLocation: Pair<Double, Double>
    ) {
        val parkingLotWeights = mutableListOf<Pair<ParkingLot, Double>>()

        for (parkingLot in apiResponse.parkingLots) {
            val availableSpots = parkingLot.number_of_empty_parking_slots
            val numberOfUsersForLot = parkingLotUsersCount[parkingLot.parking_lot_name]?.size ?: 0

            if (numberOfUsersForLot >= availableSpots) continue

            if (availableSpots > 0) {
                val destinationLocation =
                    Pair(parkingLot.latitude, parkingLot.longitude)
                val duration = parkingService?.getETA(currentLocation, destinationLocation)

                if (duration != null) {
                    val weight = availableSpots.toDouble() / (duration)
                    parkingLotWeights.add(Pair(parkingLot, weight))
                }
            }
        }
        parkingLotWeightsLiveData?.postValue(parkingLotWeights)
        if (parkingLotWeights.isNotEmpty()) {
            val sortedParkingLots = parkingLotWeights.sortedByDescending { it.second }
            val nearestParkingLot = sortedParkingLots.first().first
            nearestParkingLotLiveData?.postValue(nearestParkingLot)
            destinationLocation =
                Pair(nearestParkingLot.latitude, nearestParkingLot.longitude)
            val durationInSec = parkingService?.getETA(currentLocation, destinationLocation!!)

            durationInSecLiveData?.postValue(durationInSec)
        }
    }

//    private suspend fun getDurationInSecs(
//        currentLocation: Pair<Double, Double>,
//        destinationLocation: Pair<Double, Double>
//    ): Double? {
//
//        return parkingService?.getETA(currentLocation, destinationLocation)
//    }
}