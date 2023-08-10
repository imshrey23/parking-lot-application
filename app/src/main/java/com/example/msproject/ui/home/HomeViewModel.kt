package com.example.msproject.ui.home

import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.msproject.api.LoadingState
import com.example.msproject.api.ServiceHttpRequest
import com.example.msproject.common.CommonUtils
import com.example.msproject.model.ParkingLot
import com.example.msproject.model.ParkingLotsResponse
import com.google.android.gms.maps.GoogleMap
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val httpRequest: ServiceHttpRequest)  : ViewModel() {

    private val parkingLotUsersCount: MutableMap<String, MutableSet<String>> = mutableMapOf()
    var nearestParkingLotLiveData: MutableLiveData<ParkingLot>? = MutableLiveData<ParkingLot>()
    var parkingLotWeightsLiveData: MutableLiveData<MutableList<Pair<ParkingLot, Double>>>? =
        MutableLiveData<MutableList<Pair<ParkingLot, Double>>>()
    var durationInSecLiveData: MutableLiveData<Double>? = MutableLiveData<Double>()
    var loadingStateLiveData: MutableLiveData<LoadingState> = MutableLiveData<LoadingState>()

    fun getParkingLotsApi(
        currentLocation: Pair<Double, Double>,
        googleMap: GoogleMap,
        showProgressLoader: Boolean
    ) {
        if (showProgressLoader){
            loadingStateLiveData.postValue(LoadingState.LOADING)
        }
        httpRequest.callParkingLotsApi { apiResponse ->
            if (apiResponse != null) {
                if(showProgressLoader){
                    loadingStateLiveData.postValue(LoadingState.SUCCESS)
                }
                findNearestParkingLot(apiResponse, currentLocation, googleMap)
            } else {
                loadingStateLiveData.postValue(LoadingState.FAILURE)
            }
        }
    }

    fun sendDataToApi(parkingLotName: String, deviceId: String, timeToReach: Long) {
        httpRequest.sendDataToApi(parkingLotName, deviceId, timeToReach)
    }

    fun startPeriodicDeletion() {
        deleteHandler.removeCallbacks(deleteRunnable)
        deleteHandler.post(deleteRunnable)
    }

    private val deleteHandler = Handler(Looper.getMainLooper())
    private val deleteRunnable = object : Runnable {
        override fun run() {
            Thread {
                httpRequest.deleteOldEntriesFromApi()
            }.start()

            deleteHandler.postDelayed(this, DELETE_INTERVAL_MS.toLong())
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun findNearestParkingLot(
        apiResponse: ParkingLotsResponse,
        currentLocation: Pair<Double, Double>,
        googleMap: GoogleMap
    ) {


        fetchNumberOfUsersForParkingLots(apiResponse)

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
            httpRequest.callDistanceMatrixApi(currentLocation, destinationLocation)
        return CommonUtils.getDrivingDurationFromDistanceMatrixApiResponse(distanceMatrixResp)
    }

    private fun fetchNumberOfUsersForParkingLots(parkingLotsResponse: ParkingLotsResponse) {
        parkingLotUsersCount.clear()

        for (parkingLot in parkingLotsResponse.parkingLots) {
            val parkingLotName = parkingLot.parking_lot_name

            httpRequest.callParkingLotApi(parkingLotName) { response ->
                if (response != null) {
                    val numberOfUsers = response?.numberOfUsers?: 0
                    parkingLotUsersCount[parkingLotName] =
                        (parkingLotUsersCount.getOrDefault(
                            parkingLotName,
                            mutableSetOf()
                        ) + numberOfUsers) as MutableSet<String>
                }
            }
        }
    }

    companion object {
        private const val DELETE_INTERVAL_MS = 3000
    }
}