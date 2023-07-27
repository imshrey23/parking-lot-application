package com.example.msproject.view_model

import android.app.AlertDialog
import android.os.Build
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.example.msproject.MainActivity
import com.example.msproject.R
import com.example.msproject.model.ParkingLot
import com.example.msproject.model.ParkingLotsResponse
import com.example.msproject.model.distance.DistanceMatrixResponse
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.cloud.translate.Translation
import com.google.gson.Gson
import java.net.URL
import android.provider.Settings
import com.example.msproject.view_model.http.HttpRequest
import com.google.api.AnnotationsProto.http


import java.util.*

class Location(private val activity: MainActivity) {

    var parkingCharges: String? = null
    var parkingImageUrl: String? = null
    var timestamp: String? = null
    var locationName: String? = null
    var timeToReach: Long? = null

    private val parkingLotUsersCount: MutableMap<String, MutableSet<String>> = mutableMapOf()
    private val httpRequest = HttpRequest()

    @RequiresApi(Build.VERSION_CODES.N)
    fun findNearestParkingLot(apiResponse: String, currentLocation: Pair<Double, Double>, googleMap: GoogleMap) {
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
                val destinationLocation = Pair(parkingLot.latitude.toDouble(), parkingLot.longitude.toDouble())
                val duration = getDrivingDurationFromDistanceMatrixApiResponse(callDistanceMatrixApi(currentLocation, destinationLocation))

                if (duration != null) {
                    // Calculate the weight as the ratio of available spots to duration
                    val weight = availableSpots.toDouble() / (duration )
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
        if (parkingLotWeights.isNotEmpty()) {
            // Sort the parking lots based on their weights in descending order
            val sortedParkingLots = parkingLotWeights.sortedByDescending { it.second }

            // Get the first parking lot with the highest weight
            val nearestParkingLot = sortedParkingLots.first().first

            // Update the UI with the information about the nearest parking lot
            updateParkingLocationOnMap(googleMap, nearestParkingLot.latitude.toDouble(), nearestParkingLot.longitude.toDouble())
            locationName = nearestParkingLot.parking_lot_name
            val destinationLocation = Pair(nearestParkingLot.latitude.toDouble(), nearestParkingLot.longitude.toDouble())
            val durationInSec = getDrivingDurationFromDistanceMatrixApiResponse(callDistanceMatrixApi(currentLocation, destinationLocation))
            val durationInMilliSec = durationInSec?.let { it * 1000 }
            print(System.currentTimeMillis())
            timeToReach = durationInMilliSec?.let { System.currentTimeMillis() + it }?.toLong()


            // Convert milliseconds to minutes before the comparison
            if ((durationInSec ?: 0.0) > 600.0) {
                activity.runOnUiThread {
                    val builder = AlertDialog.Builder(activity)
                    builder.setTitle("Important Message")
                    builder.setMessage("Please check when you are 15 mins away from the destination in order to get reliable data.")
                    builder.setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }

                    val dialog: AlertDialog = builder.create()
                    dialog.show()
                }
            }



            val spotsAvailable = nearestParkingLot.number_of_empty_parking_slots

            val locale = Locale.getDefault()
            val translatedLocationName = when (locale.language) {
                "mr" -> translate(locationName!!, "Mr") // Replace with your translation function
                "es" -> translate(locationName!!, "Es") // Replace with your translation function
                else -> locationName // Default to original name if no translation available
            }

            // Translate "spot" or "spots" based on device language
            val spotsStringResourceId = if (spotsAvailable == 1) {
                R.string.spot
            } else {
                R.string.spots
            }

            val spotsString = activity.getString(spotsStringResourceId)
            val formattedSpotsAvailable = String.format(
                Locale.getDefault(),
                "%d",
                spotsAvailable
            )
            val rightText = "$formattedSpotsAvailable $spotsString"

            parkingCharges = nearestParkingLot.parking_charges
            parkingImageUrl = nearestParkingLot.image_url
            timestamp = nearestParkingLot.timestamp
            // Update the bottom sheet view
            val bottomSheetLayout = activity.findViewById<LinearLayout>(R.id.bottomSheetLayout)
            val leftTextView = bottomSheetLayout.findViewById<TextView>(R.id.leftTextView)
            val rightTextView = bottomSheetLayout.findViewById<TextView>(R.id.rightTextView)

            activity.runOnUiThread {
                leftTextView.text = translatedLocationName
                rightTextView.text = rightText
            }

        } else {
            // No parking lot found with available spots
            val message = "Parking lot will be full by the time you reach there"
            activity.showPopupMessage(message)
        }



    }

    private fun fetchNumberOfUsersForParkingLots(parkingLotsResponse: ParkingLotsResponse) {
        parkingLotUsersCount.clear()

        for (parkingLot in parkingLotsResponse.parkingLots) {
            val parkingLotName = parkingLot.parking_lot_name

            httpRequest.callParkingLotApi(parkingLotName) { response ->
                if (response != null) {
                    val numberOfUsers = response.toIntOrNull() ?: 0
                    parkingLotUsersCount[parkingLotName] =
                        (parkingLotUsersCount.getOrDefault(parkingLotName, mutableSetOf()) + numberOfUsers) as MutableSet<String>
                }
            }
        }
    }


    // Used only by findNearestParkingLot
    private fun callDistanceMatrixApi(origin: Pair<Double, Double>, destination: Pair<Double, Double>): String {
        val url = "https://maps.googleapis.com/maps/api/distancematrix/json?" +
                "origins=${origin.first},${origin.second}&" +
                "destinations=${destination.first},${destination.second}&" +
                "mode=driving&" +
                "key=AIzaSyCvSl5ugDPB8g_NPPEtK2NwMqB6D0zzF0Y"
        return URL(url).readText()
    }

    // Used by findNearestParkingLot
    private fun getDrivingDurationFromDistanceMatrixApiResponse(response: String): Double? {
        val gson = Gson()
        val distanceMatrixResponse = gson.fromJson(response, DistanceMatrixResponse::class.java)
        return if (distanceMatrixResponse.rows.isNotEmpty() &&
            distanceMatrixResponse.rows[0].elements.isNotEmpty() &&
            distanceMatrixResponse.rows[0].elements[0].duration != null) {
            distanceMatrixResponse.rows[0].elements[0].duration?.value?.toDouble()
        } else {
            null
        }
    }

    // Get the count of people watching a specific parking lot
//    @RequiresApi(Build.VERSION_CODES.N)
//    fun getParkingLotUsersCount(parkingLotId: String): Int {
//        val count = parkingLotUsersCount[parkingLotId]
//        return count ?: 0
//    }

    private fun translate(text: String, targetLanguage: String): String {
        // Set up the translation service
        val translate = TranslateOptions.newBuilder()
            .setApiKey("AIzaSyCvSl5ugDPB8g_NPPEtK2NwMqB6D0zzF0Y")
            .build()
            .service

        // Perform the translation
        val translation: Translation = translate.translate(
            text,
            Translate.TranslateOption.targetLanguage(targetLanguage)
        )

        // Return the translated text
        return translation.translatedText
    }

    private fun updateParkingLocationOnMap(googleMap: GoogleMap, latitude: Double, longitude: Double) {
        val latLng = LatLng(latitude, longitude)
        activity.runOnUiThread {
            googleMap.clear()
            googleMap.addMarker(MarkerOptions().position(latLng))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,18f))
        }
    }

    // Function to retrieve a unique identifier for the device
    private fun getDeviceId(): String {
        return Settings.Secure.getString(activity.contentResolver, Settings.Secure.ANDROID_ID)
    }



}


