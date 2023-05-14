package com.example.msproject

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.google.gson.Gson
import com.google.gson.internal.bind.TypeAdapters.URL
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.search_bar
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.net.URL
import java.util.*
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.cloud.translate.Translation
import android.provider.Settings



class MainActivity : AppCompatActivity() , View.OnClickListener, OnMapReadyCallback {
    private var isSearchActivityLaunched = true // Flag to indicate whether search activity is already launched
    private lateinit var fusedLocationClient: FusedLocationProviderClient //location permission
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var googleMap: GoogleMap
    private var parkingTimeLimit: String? = null
    private var parkingCharges: String? = null
    private var parkingImageUrl: String? = null
    private var isSearchBarEmpty = true // Flag to indicate whether the search bar is empty or not

    private val apiHandler = Handler()
    private var lastSearchedLocationLat: Double? = null
    private var lastSearchedLocationLng: Double? = null


    private val apiRunnable = object : Runnable {
        override fun run() {
            if (lastSearchedLocationLat != null && lastSearchedLocationLng != null) {

                val currentLocation = Pair(
                    lastSearchedLocationLat ?: 0.0,
                    lastSearchedLocationLng ?: 0.0
                )

                callParkingLotsApi { apiResponse ->
                    if (apiResponse != null) {
                        findNearestParkingLot(apiResponse, currentLocation)
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
            apiHandler.postDelayed(this, 300)
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
            intent.putExtra("parking_time_limit", parkingTimeLimit)
            intent.putExtra("parking_charges", parkingCharges)
            intent.putExtra("parkingImageUrl" , parkingImageUrl)
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
            val latLng = place.latLng
            lastSearchedLocationLat = place.latLng?.latitude
            lastSearchedLocationLng = place.latLng?.longitude
            val currentLocation = Pair(
                lastSearchedLocationLat ?: 0.0,
                lastSearchedLocationLng ?: 0.0
            )
            callParkingLotsApi { apiResponse ->
                if (apiResponse != null) {
                    findNearestParkingLot(apiResponse, currentLocation)
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



    private var parkingLatitude: Double = 0.0
    private var parkingLongitude: Double = 0.0

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
                        callParkingLotsApi { apiResponse ->
                            if (apiResponse != null) {
                                findNearestParkingLot(apiResponse, currentLocation)
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

        // Check if location services are enabled
// Check if location services are enabled
//        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
//        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            // Show popup to turn on location services
//            val builder = AlertDialog.Builder(this)
//            builder.setTitle("Location Services Disabled")
//            builder.setMessage("To find parking lots near your current location, turn on location services.")
//            builder.setPositiveButton("Settings") { _, _ ->
//                // Open settings to turn on location services
//                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
//                startActivity(intent)
//            }
//            builder.setNegativeButton("Cancel", null)
//            val dialog = builder.create()
//            dialog.show()
//        }

    }









    fun translate(text: String, targetLanguage: String): String {
        // Set up the translation service
        val translate = TranslateOptions.newBuilder()
            .setApiKey("AIzaSyDu-QiPUjWvgz9k4WH56Qj0w03Qs2eud9I")
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

    private fun updateParkingLocationOnMap(googleMap: GoogleMap ,latitude: Double, longitude: Double) {
        val latLng = LatLng(latitude, longitude)
        runOnUiThread {
            googleMap.clear()
            googleMap.addMarker(MarkerOptions().position(latLng))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
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


    private fun callParkingLotsApi(callback: (String?) -> Unit) {
        val url = "https://658ytxfrod.execute-api.us-east-1.amazonaws.com/dev/parking_lots"
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("API Error", e.message ?: "Unknown Error")
                callback(null)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                val responseString = response.body?.string()
                callback(responseString)
            }
        })
    }


    fun findNearestParkingLot(apiResponse: String, currentLocation: Pair<Double, Double>) {
        val gson = Gson()
        val parkingLotsResponse = gson.fromJson(apiResponse, ParkingLotsResponse::class.java)

        // First, filter out parking lots with less than 5 spots available
        val availableParkingLots = parkingLotsResponse.parkingLots.filter { it.number_of_empty_parking_slots > 5 }

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
            parkingLatitude = (nearestParkingLot.latitude).toDouble()
            parkingLongitude = (nearestParkingLot.longitude).toDouble()
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

            val spotsString = getString(spotsStringResourceId)
            val formattedSpotsAvailable = String.format(
                Locale.getDefault(),
                "%d",
                spotsAvailable
            )
            val rightText = "$formattedSpotsAvailable $spotsString"


            // Update the bottom sheet view
            val bottomSheetLayout = findViewById<LinearLayout>(R.id.bottomSheetLayout)
            val leftTextView = bottomSheetLayout.findViewById<TextView>(R.id.leftTextView)
            val rightTextView = bottomSheetLayout.findViewById<TextView>(R.id.rightTextView)

            parkingTimeLimit = nearestParkingLot.parking_lot_time_limit
            parkingCharges = nearestParkingLot.parking_charges
            parkingImageUrl = nearestParkingLot.image_url

            runOnUiThread {
                leftTextView.text = translatedLocationName
                rightTextView.text = rightText

            }


        } else {
            // No parking lot found
            println("No parking lots found.")
        }
    }



    // Models
    data class ParkingLotsResponse(val parkingLots: List<ParkingLot>)
    data class ParkingLot(
        val latitude: String,
        val longitude: String,
        val parking_lot_name: String,
        val number_of_empty_parking_slots: Int,
        val total_number_of_parking_lots: Int,
        val timestamp: String,
        val image_url: String,
        val parking_lot_time_limit: String,
        val parking_charges: String
    )

    // Functions
    fun callDistanceMatrixApi(origin: Pair<Double, Double>, destination: Pair<Double, Double>): String {
        val url = "https://maps.googleapis.com/maps/api/distancematrix/json?" +
                "origins=${origin.first},${origin.second}&" +
                "destinations=${destination.first},${destination.second}&" +
                "mode=driving&" +
                "key=AIzaSyDu-QiPUjWvgz9k4WH56Qj0w03Qs2eud9I"
        return URL(url).readText()
    }

    fun getDrivingDurationFromDistanceMatrixApiResponse(response: String): Double? {
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

    data class DistanceMatrixResponse(val rows: List<Row>)
    data class Row(val elements: List<Element>)
    data class Element(val duration: Duration?)
    data class Duration(val value: Long, val text: String)

    companion object{
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
        private const val PERMISSIONS_REQUEST_LOCATION = 100
    }
}