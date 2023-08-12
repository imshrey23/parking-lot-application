package com.example.msproject.ui.home

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.msproject.api.LoadingState
import com.example.msproject.api.ApiService
import com.example.msproject.common.CommonUtils
import com.example.msproject.model.ParkingLot
import com.example.msproject.model.ParkingLotsResponse
import com.google.android.gms.maps.GoogleMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val apiService: ApiService)  : ViewModel() {


    var nearestParkingLotLiveData: MutableLiveData<ParkingLot>? = MutableLiveData<ParkingLot>()
    var parkingLotWeightsLiveData: MutableLiveData<MutableList<Pair<ParkingLot, Double>>>? =
        MutableLiveData<MutableList<Pair<ParkingLot, Double>>>()
    var durationInSecLiveData: MutableLiveData<Double>? = MutableLiveData<Double>()
    var loadingStateLiveData: MutableLiveData<LoadingState> = MutableLiveData<LoadingState>()

    private val parkingLotUsersCount: MutableMap<String, MutableSet<String>> = mutableMapOf()
    private val scope = MainScope()
    private var job: Job? = null


    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        loadingStateLiveData.postValue(LoadingState.FAILURE)
    }

    fun getParkingLotsApi(
        currentLocation: Pair<Double, Double>,
        googleMap: GoogleMap,
        showProgressLoader: Boolean
    ) {
        if (showProgressLoader){
            loadingStateLiveData.postValue(LoadingState.LOADING)
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                try {
                    apiService.getParkingLots { apiResponse ->
                        if (apiResponse != null) {
                            if (showProgressLoader) {
                                loadingStateLiveData.postValue(LoadingState.SUCCESS)
                            }
                            fetchNumberOfUsersForParkingLots(apiResponse)
                            findNearestParkingLot(apiResponse, currentLocation, googleMap)
                        } else {
                            if (showProgressLoader) {
                                loadingStateLiveData.postValue(LoadingState.FAILURE)
                            }
                        }
                    }
                } catch (networkError: IOException ){
                    if (showProgressLoader) {
                        loadingStateLiveData.postValue(LoadingState.FAILURE)
                    }
                }
            }
        }
    }

    private fun fetchNumberOfUsersForParkingLots(parkingLotsResponse: ParkingLotsResponse) {
        parkingLotUsersCount.clear()

        for (parkingLot in parkingLotsResponse.parkingLots) {
            val parkingLotName = parkingLot.parking_lot_name
            viewModelScope.launch(exceptionHandler) {
                try{
                    apiService.getParkingLotInfo(parkingLotName) { response ->
                        if (response != null) {
                            val numberOfUsers = response?.numberOfUsers?: 0
                            parkingLotUsersCount[parkingLotName] =
                                (parkingLotUsersCount.getOrDefault(
                                    parkingLotName,
                                    mutableSetOf()
                                ) + numberOfUsers) as MutableSet<String>
                        }
                    }
                }catch (networkError: IOException){
                    Log.e("fetchNumberOfUsersForParkingLots" , "Request to get number of users for parking lots failed")
                }
            }
        }
    }

    fun sendDataToApi(parkingLotName: String, deviceId: String, timeToReach: Long) {
        viewModelScope.launch(exceptionHandler) {
            withContext(Dispatchers.IO){
                apiService.sendDataToDB(parkingLotName, deviceId, timeToReach)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun findNearestParkingLot(
        apiResponse: ParkingLotsResponse,
        currentLocation: Pair<Double, Double>,
        googleMap: GoogleMap
    ) {
        val parkingLotWeights = mutableListOf<Pair<ParkingLot, Double>>()

        for (parkingLot in apiResponse.parkingLots) {
            val availableSpots = parkingLot.number_of_empty_parking_slots
            val numberOfUsersForLot = parkingLotUsersCount[parkingLot.parking_lot_name]?.size ?: 0
            if (availableSpots > 0) {
                val destinationLocation =
                    Pair(parkingLot.latitude, parkingLot.longitude)
                val duration = getDurationInSecs(currentLocation, destinationLocation)

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
            val destinationLocation =
                Pair(nearestParkingLot.latitude, nearestParkingLot.longitude)
            val durationInSec = getDurationInSecs(currentLocation, destinationLocation)

            durationInSecLiveData?.postValue(durationInSec)
        }
    }


    private fun getDurationInSecs(
        currentLocation: Pair<Double, Double>,
        destinationLocation: Pair<Double, Double>
    ): Double? {

        val distanceMatrixResp =
            apiService.callDistanceMatrixApi(currentLocation, destinationLocation)
        return CommonUtils.getDrivingDurationFromDistanceMatrixApiResponse(distanceMatrixResp)
    }

    fun startPeriodicDeletion() {
        job = scope.launch {
            withContext(Dispatchers.IO) {
                while (true) {
                    apiService.deleteExpiredDocuments()
                    delay(DELETE_INTERVAL_MS.toLong())
                }
            }
        }
    }

    fun removeJobUpdates(){
        job?.cancel()
        job = null
    }


    companion object {
        private const val DELETE_INTERVAL_MS = 3000
    }
}