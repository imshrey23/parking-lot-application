package com.example.msproject.view_model

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



import java.util.*

class Location(private val activity: MainActivity) {

    var parkingCharges: String? = null
    var parkingImageUrl: String? = null
    var timestamp: String? = null

    private val parkingLotUsersCount: MutableMap<String, MutableSet<String>> = mutableMapOf()

    @RequiresApi(Build.VERSION_CODES.N)
    fun findNearestParkingLot(apiResponse: String, currentLocation: Pair<Double, Double>, googleMap: GoogleMap) {
        val gson = Gson()
        val parkingLotsResponse = gson.fromJson(apiResponse, ParkingLotsResponse::class.java)
        // Get the total number of empty parking spots available
        val emptyParkingSpots = parkingLotsResponse.parkingLots.sumBy { it.number_of_empty_parking_slots }

        // First, filter out parking lots with less than 5 spots available
        val availableParkingLots = parkingLotsResponse.parkingLots.filter { it.number_of_empty_parking_slots > 1 }

        var minDistance = Double.MAX_VALUE
        var nearestParkingLot: ParkingLot? = null

        if (availableParkingLots.isNotEmpty()) {
            // If there are available parking lots, find the nearest one
            for (parkingLot in availableParkingLots) {
                val distanceMatrixApiResponse = callDistanceMatrixApi(currentLocation, Pair(parkingLot.latitude.toDouble(), parkingLot.longitude.toDouble()))
                val duration = getDrivingDurationFromDistanceMatrixApiResponse(distanceMatrixApiResponse)

                if (duration != null && duration < minDistance) {
                    minDistance = duration
                    nearestParkingLot = parkingLot
                }
            }
        } else {
            // If there are no available parking lots, find the nearest one with less than 5 spots available
            for (parkingLot in parkingLotsResponse.parkingLots) {
                val distanceMatrixApiResponse = callDistanceMatrixApi(currentLocation, Pair(parkingLot.latitude.toDouble(), parkingLot.longitude.toDouble()))
                val duration = getDrivingDurationFromDistanceMatrixApiResponse(distanceMatrixApiResponse)

                if (duration != null && duration < minDistance && parkingLot.number_of_empty_parking_slots > 0) {
                    minDistance = duration
                    nearestParkingLot = parkingLot
                }
            }
        }

        if (nearestParkingLot != null) {
            val nearestParkingLotJson = gson.toJson(nearestParkingLot)
            val nearestParkingLot = Gson().fromJson(
                nearestParkingLotJson,
                ParkingLot::class.java
            )
            // Print information about the nearest parking lot
            updateParkingLocationOnMap(googleMap, (nearestParkingLot.latitude).toDouble(), nearestParkingLot.longitude.toDouble())
            val locationName = (nearestParkingLot.parking_lot_name)
            val spotsAvailable = (nearestParkingLot.number_of_empty_parking_slots)

            val locale = Locale.getDefault()
            val translatedLocationName = when (locale.language) {
                "mr" -> translate(locationName , "Mr") // Replace with your translation function
                "es" -> translate(locationName , "Es") // Replace with your translation function
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
            // No parking lot found
            println("No parking lots found.")
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


