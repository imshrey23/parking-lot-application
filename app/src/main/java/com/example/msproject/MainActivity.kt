package com.example.msproject

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.msproject.processor.Location
import com.example.msproject.processor.http.HttpRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.search_bar

import java.lang.Exception

import java.util.*



class MainActivity : AppCompatActivity() , View.OnClickListener, OnMapReadyCallback {

    private val view = Location(this)
    private val http = HttpRequest()
    private var isSearchActivityLaunched = true // Flag to indicate whether search activity is already launched
    private lateinit var fusedLocationClient: FusedLocationProviderClient //location permission
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    lateinit var googleMap: GoogleMap
    private var isSearchBarEmpty = true // Flag to indicate whether the search bar is empty or not

    private val apiHandler = Handler()
    private var lastSearchedLocationLat: Double? = null
    private var lastSearchedLocationLng: Double? = null
    var parkingLatitude: Double = 0.0
    var parkingLongitude: Double = 0.0


    private val apiRunnable = object : Runnable {
        override fun run() {
            if (lastSearchedLocationLat != null && lastSearchedLocationLng != null) {

                val currentLocation = Pair(
                    lastSearchedLocationLat ?: 0.0,
                    lastSearchedLocationLng ?: 0.0
                )

                http.callParkingLotsApi { apiResponse ->
                    if (apiResponse != null) {
                        view.findNearestParkingLot(apiResponse, currentLocation)
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
            apiHandler.postDelayed(this, 3000)
        }
    }


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize fused location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check location permission and request if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_LOCATION
            )
            getCurrentLocationAndSendToAPI()
            apiHandler.post(apiRunnable)
        } else {
            // Permission already granted, get current location and send to API
            getCurrentLocationAndSendToAPI()
            apiHandler.post(apiRunnable)
        }

        val latitude = 29.636137668369987 // destination latitude
        val longitude = -123.279363220437  // destination longitude
        val label = "Destination Label" // destination label for display
        val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude&label=$label")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        val mapsPackage = "com.google.android.apps.maps"

        //map fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)


        //more info button
        fabMoreInfo.setOnClickListener{
            fabMoreInfo.bringToFront()
            val intent = Intent(this, MoreInfoActivity::class.java)
            intent.putExtra("parking_charges", view.parkingCharges)
            intent.putExtra("parkingImageUrl" , view.parkingImageUrl)
            startActivity(intent)
        }

        //navigate
        fabNavigation.setOnClickListener{
            fabNavigation.bringToFront()
            val isMapsInstalled = try {
                packageManager.getPackageInfo(mapsPackage, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
            if (isMapsInstalled) {
                // Set the package for the intent if Maps is installed
                val gmmIntentUri = Uri.parse("google.navigation:q=$parkingLatitude,$parkingLongitude&label=$label")
                mapIntent.setData(gmmIntentUri)
                mapIntent.setPackage(mapsPackage)
                startActivity(mapIntent)
            } else {
                // Handle the case where Maps is not installed
                Toast.makeText(this, "Google Maps is not installed on this device", Toast.LENGTH_SHORT).show()
            }
        }

        //search bar text
        if (!Places.isInitialized()){
            //add activity you are in currently
            Places.initialize(this@MainActivity,resources.getString(R.string.google_maps_api_key))
        }
        search_bar.setOnClickListener(this)
        search_bar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isSearchBarEmpty = s?.isEmpty() ?: true
                if (isSearchBarEmpty) {
                    // Set the search icon when the search bar is empty
                    search_bar.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.search_bar_corner, 0)

                    // Set the behavior for the search icon
                    search_bar.setOnClickListener(this@MainActivity)
                    lastSearchedLocationLat = null
                    lastSearchedLocationLng = null

                } else {
                    // Set the cross icon when the search bar is not empty
                    search_bar.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_cross, 0)

                    // Set the behavior for the cross icon
                    search_bar.setOnClickListener {
                        // Clear the search bar text
                        search_bar.text = null
                        lastSearchedLocationLat = null
                        lastSearchedLocationLng = null
                    }

                    // Open the search screen
                    if (!isSearchActivityLaunched) {
                        val intent = Autocomplete.IntentBuilder(
                            AutocompleteActivityMode.FULLSCREEN,
                            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
                        ).build(this@MainActivity)
                        startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)
                        isSearchActivityLaunched = true
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Do nothing
            }
        })


        til_location.setEndIconOnClickListener {
            search_bar.setText("")
            search_bar.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.search_bar_corner, 0)
            isSearchBarEmpty = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the API call loop when the activity is destroyed
        apiHandler.removeCallbacks(apiRunnable)
    }

    override fun onClick(v: View?) {
        when(v!!.id){
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
                                .build(this@MainActivity)
                        startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)


                    } catch (e: Exception) {
                        e.printStackTrace()
                        isSearchActivityLaunched = true // Re-enable search bar clickability in case of exception
                    }
                }
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
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
                    view.findNearestParkingLot(apiResponse, currentLocation)
                } else {
                    // Handle error calling API
                    println("Error calling API.")
                }
            }

            search_bar.setText(place.name)
            runOnUiThread {
                onMapReady(googleMap, parkingLatitude, parkingLongitude)
            }

        }
      }
        else if (resultCode == Activity.RESULT_CANCELED) {
            Log.e("Cancelled", "Cancelled")
            isSearchActivityLaunched = true // Re-enable search bar clickability after autocomplete intent is closed
        }
    }

    override fun onMapReady(googleMap: GoogleMap){
        this.googleMap = googleMap
    }
    fun onMapReady(googleMap: GoogleMap , latitude: Double,longitude: Double) {
        val parking_marker = LatLng(latitude, longitude)
        googleMap.addMarker(MarkerOptions().position(parking_marker).title("Marker in New York"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(parking_marker, 18f))
    }


    fun getCurrentLocationAndSendToAPI() {
        // Get current location using fused location client
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_LOCATION
            )
        } else {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    // Location retrieved successfully, send to API
                    location?.let {
                        val currentLocation = Pair(it.latitude, it.longitude)
                        http.callParkingLotsApi { apiResponse ->
                            if (apiResponse != null) {
                                view.findNearestParkingLot(apiResponse, currentLocation)
                            } else {
                                // Handle error calling API
                                println("Error calling API.")
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Failed to retrieve location
                    Log.e("Location Error", e.message ?: "Unknown Error")
                }
        }


    }

    // This method is called when the user responds to the permission request.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndSendToAPI()
                apiHandler.post(apiRunnable)
                // Permission was granted. Do the location-related task you need to do.
                // ...
            } else {
                // Permission denied.
                Toast.makeText(
                    this,
                    "Location permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object{
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
        const val PERMISSIONS_REQUEST_LOCATION = 100
    }
}