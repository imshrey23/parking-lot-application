package com.example.msproject.ui.home

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.msproject.api.LoadingState
import com.example.msproject.api.ServiceHttpRequest
import com.example.msproject.common.CommonUtils
import com.example.msproject.model.ParkingLot
import com.example.msproject.model.ParkingLotsResponse
import com.google.android.gms.maps.GoogleMap
import com.google.gson.Gson
import java.util.*

class HomeViewModel : ViewModel() {

    private val parkingLotUsersCount: MutableMap<String, MutableSet<String>> = mutableMapOf()
    private val httpRequest = ServiceHttpRequest()
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
        httpRequest.callParkingLotsApi(loadingStateLiveData, showProgressLoader) { apiResponse ->
            if (apiResponse != null) {
                Log.d("API Response ---------> ", apiResponse)
                findNearestParkingLot(apiResponse, currentLocation, googleMap)
            } else {
                // Handle error calling API
                println("Error calling API.")
            }
        }
    }

    fun sendDataToApi(parkingLotName: String, deviceId: String, timeToReach: Long) {
        httpRequest.sendDataToApi(parkingLotName, deviceId, timeToReach)
    }

    // Method to start periodic deletion process
    fun startPeriodicDeletion() {
        // Remove any existing callbacks, in case this method is called multiple times
        deleteHandler.removeCallbacks(deleteRunnable)
        // Schedule the first deletion immediately, and then it will repeat every DELETE_INTERVAL_MS
        deleteHandler.post(deleteRunnable)
    }

    // Handler and Runnable for periodic deletion
    private val deleteHandler = Handler(Looper.getMainLooper())
    private val deleteRunnable = object : Runnable {
        override fun run() {
            // Call the method to delete old entries
            Thread {
                httpRequest.deleteOldEntriesFromApi()
            }.start()
            // Schedule the next deletion after DELETE_INTERVAL_MS
            deleteHandler.postDelayed(this, DELETE_INTERVAL_MS.toLong())
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun findNearestParkingLot(
        apiResponse: String,
        currentLocation: Pair<Double, Double>,
        googleMap: GoogleMap
    ) {
        val gson = Gson()

        val parkingLotsResponse = gson.fromJson(apiResponse, ParkingLotsResponse::class.java)

        fetchNumberOfUsersForParkingLots(parkingLotsResponse)

        // Create a list to store parking lots along with their weights
        val parkingLotWeights = mutableListOf<Pair<ParkingLot, Double>>()

        // Calculate the weight for each parking lot based on the number of empty spots and distance
        for (parkingLot in parkingLotsResponse.parkingLots) {
            val availableSpots = parkingLot.number_of_empty_parking_slots
            val numberOfUsersForLot = parkingLotUsersCount[parkingLot.parking_lot_name]?.size ?: 0
            if (availableSpots > 0) {
                val destinationLocation =
                    Pair(parkingLot.latitude.toDouble(), parkingLot.longitude.toDouble())
                val duration = getDurationInSecs(currentLocation, destinationLocation)

                if (duration != null) {
                    // Calculate the weight as the ratio of available spots to duration
                    val weight = availableSpots.toDouble() / (duration)
                    parkingLotWeights.add(Pair(parkingLot, weight))
                }
            }

//            if(numberOfUsersForLot < availableSpots){
//            activity.runOnUiThread {
//                val builder = AlertDialog.Builder(activity)
//                builder.setTitle("Important Message")
//                builder.setMessage("Please check when you are 15 mins away from the destination in order to get reliable data.")
//                builder.setPositiveButton("OK") { dialog, _ ->
//                    dialog.dismiss()
//                }
//
//                val dialog: AlertDialog = builder.create()
//                dialog.show()
//            }
//        }
        }
        parkingLotWeightsLiveData?.postValue(parkingLotWeights)
        if (parkingLotWeights.isNotEmpty()) {
            // Sort the parking lots based on their weights in descending order
            val sortedParkingLots = parkingLotWeights.sortedByDescending { it.second }

            // Get the first parking lot with the highest weight
            val nearestParkingLot = sortedParkingLots.first().first
            nearestParkingLotLiveData?.postValue(nearestParkingLot)

            val destinationLocation =
                Pair(nearestParkingLot.latitude.toDouble(), nearestParkingLot.longitude.toDouble())

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
                    val numberOfUsers = response.toIntOrNull() ?: 0
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