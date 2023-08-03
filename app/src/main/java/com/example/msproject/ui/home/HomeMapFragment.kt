package com.example.msproject.ui.home

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.msproject.R
import com.example.msproject.api.LoadingState
import com.example.msproject.common.CommonUtils
import com.example.msproject.databinding.HomeMapFragmentBinding
import com.example.msproject.ui.moreinfo.MoreInfoFragment
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
import java.util.*


class HomeMapFragment : Fragment(R.layout.home_map_fragment), OnMapReadyCallback {


    lateinit var binding: HomeMapFragmentBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap
    private lateinit var homeViewModel: HomeViewModel
    lateinit var progressDialog: ProgressDialog
    private var lastSearchedLocationLat: Double? = null
    private var lastSearchedLocationLng: Double? = null
    private var isSearchActivityLaunched: Boolean = true
    private val apiHandler = Handler()
    private var mapFragment: SupportMapFragment? = null;
    private var timeToReach: Long = 0
    private var parkingLotName: String = ""
    private var isSearchBarEmpty: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = HomeMapFragmentBinding.inflate(inflater, container, false)
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        homeViewModel.loadingStateLiveData.observe(requireActivity(), Observer {
            when (it) {
                LoadingState.LOADING -> {
                    showProgressDialog("Loading nearest parking lot...")
                }
                LoadingState.SUCCESS -> {
                    hideProgressDialog()
                }
                LoadingState.FAILURE -> {
                    hideProgressDialog()
                }
            }
        })

        homeViewModel.startPeriodicDeletion()

        if (!Places.isInitialized()) {
            Places.initialize(
                requireActivity().applicationContext,
                "AIzaSyCvSl5ugDPB8g_NPPEtK2NwMqB6D0zzF0Y"
            )
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_LOCATION
            )
        } else {
            getCurrentLocationAndSendToAPI(true)
            Log.w("========================", "onCreate")
            apiHandler.post(apiRunnable)
        }
        mapFragment =
            childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        binding.fabMoreInfo.setOnClickListener {
            binding.fabMoreInfo.bringToFront()

            val moreInfoFragment = MoreInfoFragment()
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.add(R.id.fragment_container_view, moreInfoFragment)
                .addToBackStack("more_info_tag")
            transaction.commit()
        }

        binding.fabNavigation.setOnClickListener {
            binding.fabNavigation.bringToFront()
            //navigate to external Map for Direction
            navigateToMap()
            // Call the function to send data to the API
            if (parkingLotName != null) {
                if (timeToReach != null) {
                    homeViewModel.sendDataToApi(
                        parkingLotName,
                        CommonUtils.getDeviceId(requireActivity().contentResolver),
                        timeToReach
                    )
                }
            }
        }

        binding.searchBar.setOnClickListener {
            if (isSearchActivityLaunched) {
                try {
                    isSearchActivityLaunched = false // Disable search bar clickability
                    val fields = listOf(
                        Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,
                        Place.Field.ADDRESS
                    )

                    // p the autocomplete intent with a unique request code.
                    val intent =
                        Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                            .build(requireActivity())
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)
                } catch (e: Exception) {
                    e.printStackTrace()
                    isSearchActivityLaunched =
                        true // Re-enable search bar clickability in case of exception
                }
            }
        }

        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isSearchBarEmpty = s?.isEmpty() ?: true
                if (isSearchBarEmpty) {
                    binding.searchBar.setCompoundDrawablesWithIntrinsicBounds(
                        0, 0,
                        R.drawable.search_bar_corner, 0
                    )
                    lastSearchedLocationLat = null
                    lastSearchedLocationLng = null

                    getCurrentLocationAndSendToAPI(false)

                } else {
                    binding.searchBar.setCompoundDrawablesWithIntrinsicBounds(
                        0, 0,
                        R.drawable.ic_cross, 0
                    )
                    binding.searchBar.setOnClickListener {
                        binding.searchBar.text = null
                        lastSearchedLocationLat = null
                        lastSearchedLocationLng = null

                        getCurrentLocationAndSendToAPI(true)
                    }

                    if (!isSearchActivityLaunched) {
                        val intent = Autocomplete.IntentBuilder(
                            AutocompleteActivityMode.FULLSCREEN,
                            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
                        ).build(requireActivity())
                        startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)
                        isSearchActivityLaunched = true
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        binding.tilLocation.setEndIconOnClickListener {
            binding.searchBar.setText("")
            binding.searchBar.setCompoundDrawablesWithIntrinsicBounds(
                0, 0,
                R.drawable.search_bar_corner, 0
            )
            isSearchBarEmpty = true

            getCurrentLocationAndSendToAPI(true)
        }

        homeViewModel.parkingLotWeightsLiveData?.observe(requireActivity(), Observer {
            if (it.isNotEmpty()) {
                updateUIElements()
            } else {
                // No parking lot found with available spots
                showPopupMessage(getString(R.string.parking_full))
            }
        })
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun getCurrentLocationAndSendToAPI(showLoader: Boolean) {
        // Get current location using fused location client
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If location permission is not given
            AlertDialog.Builder(requireActivity())
                .setTitle("Location Permission Needed")
                .setMessage("This application needs the location permission to find the nearest parking lot.")
                .setPositiveButton("Allow") { dialog, which ->
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        PERMISSIONS_REQUEST_LOCATION
                    )
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    dialog.dismiss()
                }
                .create()
                .show()
        } else {
            fusedLocationClient.lastLocation
                .addOnSuccessListener(requireActivity()) { location ->
                    // Location retrieved successfully, send to API
                    location?.let {
                        val currentLocation = Pair(it.latitude, it.longitude)
                        homeViewModel.getParkingLotsApi(currentLocation, googleMap, showLoader)
                    }
                }
                .addOnFailureListener(requireActivity()) { e ->
                    // Failed to retrieve location
                    Log.e("Location Error", e.message ?: "Unknown Error")
                }
        }
    }


    private val apiRunnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.N)
        override fun run() {
            Log.d("=======run==========", "" + isSearchBarEmpty)
            if (lastSearchedLocationLat != null && lastSearchedLocationLng != null) {
                val currentLocation = Pair(
                    lastSearchedLocationLat ?: 0.0,
                    lastSearchedLocationLng ?: 0.0
                )

                homeViewModel.getParkingLotsApi(currentLocation, googleMap, false)

            } else if (isSearchBarEmpty) {
                isSearchBarEmpty = false
                lastSearchedLocationLat = null
                lastSearchedLocationLng = null
                getCurrentLocationAndSendToAPI(false)

            }
            apiHandler.postDelayed(this, 30000)
        }
    }


    private fun navigateToMap() {
        val latitude = 29.636137668369987 // destination latitude
        val longitude = -123.279363220437  // destination longitude
        val label = "Destination Label" // destination label for display
        val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude&label=$label")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        val mapsPackage = "com.google.android.apps.maps"

        if (CommonUtils.isMapsInstalled(requireActivity().packageManager) == true) {
            val gmmIntentUri =
                Uri.parse("google.navigation:q=${lastSearchedLocationLat},${lastSearchedLocationLng}&label=$label")
            mapIntent.data = gmmIntentUri
            mapIntent.setPackage(mapsPackage)
            startActivity(mapIntent)
        } else {
            Toast.makeText(
                requireActivity(),
                getString(R.string.map_not_installed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateParkingLocationOnMap(
        googleMap: GoogleMap,
        latitude: Double,
        longitude: Double
    ) {
        val latLng = LatLng(latitude, longitude)
        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(latLng))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
    }

    private fun updateUIElements() {

        homeViewModel.nearestParkingLotLiveData?.observe(requireActivity(), Observer {
            Log.d("----------->", it.toString());

            parkingLotName = it.parking_lot_name
            val spotsAvailable = it.number_of_empty_parking_slots
            val locale: Locale = Locale.getDefault()
            val translatedLocationName = when (locale.language) {
                "mr" -> CommonUtils.translate(
                    parkingLotName!!,
                    "Mr"
                ) // Replace with your translation function
                "es" -> CommonUtils.translate(
                    parkingLotName!!,
                    "Es"
                ) // Replace with your translation function
                else -> parkingLotName // Default to original name if no translation available
            }
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
            binding.leftTextView.text = translatedLocationName
            binding.rightTextView.text = "$formattedSpotsAvailable $spotsString"

            // Update the UI with the information about the nearest parking lot
            updateParkingLocationOnMap(googleMap, it.latitude.toDouble(), it.longitude.toDouble())
        })

        homeViewModel.durationInSecLiveData?.observe(requireActivity(), Observer {
            val durationInMilliSec = it?.let { it * 1000 }
            print(System.currentTimeMillis())
            timeToReach =
                durationInMilliSec?.let { it -> System.currentTimeMillis() + it }?.toLong()!!

            // Convert milliseconds to minutes before the comparison
            if ((it ?: 0.0) > 600.0) {
                val builder = AlertDialog.Builder(requireActivity())
                builder.setTitle("Important Message")
                builder.setMessage("Please check when you are 15 mins away from the destination in order to get reliable data.")
                builder.setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                val dialog: AlertDialog = builder.create()
                if (!dialog.isShowing) {
                    dialog.show()
                }
            }
        })
    }

    private fun showProgressDialog(message: String) {
        progressDialog = ProgressDialog(requireActivity())
        progressDialog.setMessage(message)
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun hideProgressDialog() {
        if (::progressDialog.isInitialized && progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mapFragment?.getMapAsync { googleMap ->
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
                    isSearchActivityLaunched =
                        true // Re-enable search bar clickability after autocomplete intent is closed
                    val place: Place = Autocomplete.getPlaceFromIntent(data!!)
                    lastSearchedLocationLat = place.latLng?.latitude
                    lastSearchedLocationLng = place.latLng?.longitude
                    val currentLocation = Pair(
                        lastSearchedLocationLat ?: 0.0,
                        lastSearchedLocationLng ?: 0.0
                    )
                    homeViewModel.getParkingLotsApi(currentLocation, googleMap, true)

                    binding.searchBar.setText(place.name)
                    // Update the map in the activity (MainActivity) using a callback method
                    onMapReady(googleMap)
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.e("Cancelled", "Cancelled")
                isSearchActivityLaunched =
                    true // Re-enable search bar clickability after autocomplete intent is closed
            }
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        // Enable the "My Location" button and handle its click event
        googleMap.isMyLocationEnabled = true
        googleMap.setOnMyLocationButtonClickListener {
            // Check location permission before accessing the current location
            if (ContextCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Get the last known location and move the camera to it
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val latLng = LatLng(it.latitude, it.longitude)
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    }
                }
            } else {
                // Request location permission
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
            true
        }
    }


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                getCurrentLocationAndSendToAPI(true)
                Log.w("========================", "onRequestPermissionsResult")
                apiHandler.post(apiRunnable)
            } else {
                // permission denied, show a Toast message
                Toast.makeText(
                    requireActivity(),
                    "Permission denied to access location",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }

    }

    private fun showPopupMessage(message: String) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        apiHandler.removeCallbacks(apiRunnable)
    }


    companion object {
        const val PERMISSIONS_REQUEST_LOCATION = 100
        const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
        const val LOCATION_PERMISSION_REQUEST_CODE = 1

        // Milliseconds interval for periodic deletion (3000ms = 3 seconds)
        private const val DELETE_INTERVAL_MS = 3000
    }

}