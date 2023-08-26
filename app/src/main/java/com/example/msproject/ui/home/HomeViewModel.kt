package com.example.msproject.ui.home

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.msproject.api.apiService.LoadingState
import com.example.msproject.api.apiService.ParkingService
import com.example.msproject.api.model.parking_lots
import com.example.msproject.api.model.ParkingLotsResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel  @Inject constructor(private val parkingService: ParkingService) :
    ViewModel() {

    var nearestParkinglotsLiveData: MutableLiveData<parking_lots>? = MutableLiveData<parking_lots>()
    var parkinglotsWeightsLiveData: MutableLiveData<MutableList<Pair<parking_lots, Double>>>? = MutableLiveData<MutableList<Pair<parking_lots, Double>>>()
    var durationInSecLiveData: MutableLiveData<Double>? = MutableLiveData<Double>()
    var loadingStateLiveData: MutableLiveData<LoadingState> = MutableLiveData<LoadingState>()
    val parkingLotUsersCount: MutableMap<String, MutableSet<String>> = mutableMapOf()

    var destinationLocation: Pair<Double, Double>? = Pair(0.0 , 0.0)
    
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
        Log.i("current location" , "$currentLocation")
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

    fun getNumberOfUsersForParkingLots(parkingLotsResponse: ParkingLotsResponse) {
        parkingLotUsersCount.clear()

        for (parkingLot in parkingLotsResponse.parking_lots) {
            val parkingLotName = parkingLot.parking_lot_name
            viewModelScope.launch(exceptionHandler) {
                withContext(Dispatchers.IO) {
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
    }

    fun reserveParkingSpot(parkingLotName: String, deviceId: String, currentLocation: Pair<Double, Double>) {
        viewModelScope.launch(exceptionHandler) {
            withContext(Dispatchers.IO) {
                var durationInMilliSec =
                    destinationLocation?.let { parkingService.getETA(currentLocation, it) }
                if (durationInMilliSec != null) {
                    durationInMilliSec *= 1000
                }
                var timeToReach =
                    durationInMilliSec?.let { it -> System.currentTimeMillis() + it }?.toLong()!!
                parkingService.reserveParkingSpot(parkingLotName, deviceId, timeToReach)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    suspend fun getNearestParkingLot(
        apiResponse: ParkingLotsResponse,
        currentLocation: Pair<Double, Double>
    ) {
        try {
            Log.i("current location - getNearest", "$currentLocation")
            val parkingLotWeights = mutableListOf<Pair<parking_lots, Double>>()

            for (parkingLot in apiResponse.parking_lots) {
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
            parkinglotsWeightsLiveData?.postValue(parkingLotWeights)
            if (parkingLotWeights.isNotEmpty()) {
                val sortedParkingLots = parkingLotWeights.sortedByDescending { it.second }
                val nearestParkingLot = sortedParkingLots.first().first
                nearestParkinglotsLiveData?.postValue(nearestParkingLot)
                destinationLocation =
                    Pair(nearestParkingLot.latitude, nearestParkingLot.longitude)
                val durationInSec = parkingService.getETA(currentLocation, destinationLocation!!)

                durationInSecLiveData?.postValue(durationInSec)
            }
        } catch (e: Exception) {
            Log.e("getNearestParkingLot", "Error processing nearest parking lot", e)
        }
    }
}