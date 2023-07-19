package com.example.msproject.view_model

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.msproject.MainActivity
import com.example.msproject.R
import com.example.msproject.view_model.http.HttpRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
//import kotlinx.android.synthetic.main.activity_main.*

import java.lang.Exception

class MainViewModel(private val activity: MainActivity) {

    val view = Location(activity)
    private var isApiRunning = false
    private val http = HttpRequest()
    var isSearchActivityLaunched = true
    var isSearchBarEmpty = true
    private lateinit var googleMap: GoogleMap
    val apiHandler = Handler()
    var lastSearchedLocationLat: Double? = null
    var lastSearchedLocationLng: Double? = null
    var parkingLatitude: Double = 0.0
    var parkingLongitude: Double = 0.0
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(activity)

    val apiRunnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.N)
        override fun run() {
            if (!isApiRunning) {
                activity.showProgressDialog("Loading nearest parking lot...")
                isApiRunning = true
            }
            if (lastSearchedLocationLat != null && lastSearchedLocationLng != null) {
                val currentLocation = Pair(
                    lastSearchedLocationLat ?: 0.0,
                    lastSearchedLocationLng ?: 0.0
                )

                http.callParkingLotsApi { apiResponse ->

                    if (apiResponse != null) {
                        activity.hideProgressDialog()
                        view.findNearestParkingLot(apiResponse, currentLocation, googleMap)
                    } else {
                        // Handle error calling API
                        println("Error calling API.")
                    }
                }

            } else if (isSearchBarEmpty) {
                lastSearchedLocationLat = null
                lastSearchedLocationLng = null
                getCurrentLocationAndSendToAPI()

            }
            apiHandler.postDelayed(this, 30000)
        }
    }

    fun onClick(v: View?) {
        when (v!!.id) {
            R.id.search_bar -> {
                if (isSearchActivityLaunched) {
                    try {
                        isSearchActivityLaunched = false // Disable search bar clickability
                        val fields = listOf(
                            Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,
                            Place.Field.ADDRESS
                        )

                        // Start the autocomplete intent with a unique request code.
                        val intent =
                            Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                                .build(activity)
                        activity.startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)


                    } catch (e: Exception) {
                        e.printStackTrace()
                        isSearchActivityLaunched = true // Re-enable search bar clickability in case of exception
                    }
                }
            }
        }
    }

    fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun getCurrentLocationAndSendToAPI() {
        // Get current location using fused location client
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_LOCATION
            )
        } else {
            fusedLocationClient.lastLocation
                .addOnSuccessListener(activity) { location ->
                    // Location retrieved successfully, send to API
                    location?.let {
                        val currentLocation = Pair(it.latitude, it.longitude)
                        http.callParkingLotsApi { apiResponse ->
                            if (apiResponse != null) {
                                activity.hideProgressDialog()
                                view.findNearestParkingLot(apiResponse, currentLocation, googleMap)
                            } else {
                                // Handle error calling API
                                println("Error calling API.")
                            }
                        }
                    }
                }
                .addOnFailureListener(activity) { e ->
                    // Failed to retrieve location
                    Log.e("Location Error", e.message ?: "Unknown Error")
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndSendToAPI()
                apiHandler.post(apiRunnable)
                // Permission was granted. Do the location-related task you need to do.
                // ...
            } else {
                // Permission denied.
                Toast.makeText(
                    activity,
                    "Location permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, googleMap: GoogleMap) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
                isSearchActivityLaunched = true // Re-enable search bar clickability after autocomplete intent is closed
                val place: Place = Autocomplete.getPlaceFromIntent(data!!)
                lastSearchedLocationLat = place.latLng?.latitude
                lastSearchedLocationLng = place.latLng?.longitude
                val currentLocation = Pair(
                    lastSearchedLocationLat ?: 0.0,
                    lastSearchedLocationLng ?: 0.0
                )
                http.callParkingLotsApi { apiResponse ->
                    if (apiResponse != null) {
                        view.findNearestParkingLot(apiResponse, currentLocation, googleMap)
                    } else {
                        // Handle error calling API
                        println("Error calling API.")
                    }
                }

                activity.binding.searchBar.setText(place.name)
                // Update the map in the activity (MainActivity) using a callback method
                activity.onMapReady(googleMap)
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.e("Cancelled", "Cancelled")
            isSearchActivityLaunched = true // Re-enable search bar clickability after autocomplete intent is closed
        }
    }

    fun onDestroy() {
        // Stop the API call loop when the activity is destroyed
        apiHandler.removeCallbacks(apiRunnable)
    }

    companion object {
        const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
        const val PERMISSIONS_REQUEST_LOCATION = 100
    }
}


