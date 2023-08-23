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
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.msproject.BuildConfig
import com.example.msproject.R
import com.example.msproject.api.apiService.LoadingState
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
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class HomeMapFragment : Fragment(R.layout.home_map_fragment), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap
    private lateinit var homeViewModel: HomeViewModel
    lateinit var binding: HomeMapFragmentBinding
    lateinit var progressDialog: ProgressDialog
    private var lastSearchedLocationLat: Double? = null
    private var lastSearchedLocationLng: Double? = null
    private var isSearchActivityLaunched: Boolean = true
    private val apiHandler = Handler()
    private var mapFragment: SupportMapFragment? = null
    private var timeToReach: Long = 0
    private var parkingLotName: String = ""
    private var isSearchBarEmpty: Boolean = true
    private var showAlert: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = HomeMapFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        requestForLocationPermission()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        homeViewModel.loadingStateLiveData.observe(requireActivity(),  {
            when (it) {
                LoadingState.LOADING -> {
                    showProgressDialog(R.string.progress_bar_text.toString())
                }
                LoadingState.SUCCESS -> {
                    hideProgressDialog()
                }
                LoadingState.FAILURE -> {
                    hideProgressDialog()
                }
            }
        })

        if (!Places.isInitialized()) {
            Places.initialize(
                requireActivity().applicationContext, BuildConfig.PLACES_API_KEY
            )
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

//        if (checkSelfPermission(
//                requireActivity(),
//                Manifest.permission.ACCESS_FINE_LOCATION
//            )
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            requestPermissions(
//                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                PERMISSIONS_REQUEST_LOCATION
//            )
//        } else {
//            getCurrentLocationAndParkingLots(true)
//            apiHandler.post(apiRunnable)
//        }
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
            openGoogleMaps()
            if (ActivityCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener(requireActivity()) { location ->
                        location?.let {
                            val currentLocation = Pair(it.latitude, it.longitude)
                            homeViewModel.reserveParkingSpot(
                                parkingLotName,
                                CommonUtils.getDeviceId(requireActivity().contentResolver),
                                currentLocation
                            )
                        }
                    }
            }
        }

        binding.tilLocation.setEndIconOnClickListener {
            binding.searchBar.setText("")
            binding.searchBar.text = null
            binding.searchBar.isSelected = false
            binding.searchBar.isFocusable = false
            isSearchBarEmpty = true
            showAlert = true
            lastSearchedLocationLat = null
            lastSearchedLocationLng = null
            getCurrentLocationAndParkingLots(true)
        }

        binding.searchBar.setOnClickListener {
            if (isSearchActivityLaunched) {
                try {
                    isSearchActivityLaunched = false // Disable search bar clickability
                    val fields = listOf(
                        Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,
                        Place.Field.ADDRESS
                    )

                    val intent =
                        Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                            .build(requireActivity())
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)
                } catch (e: Exception) {
                    e.printStackTrace()
                    isSearchActivityLaunched =
                        true // Re-enable search bar clickability
                }
            }
        }

        homeViewModel.parkingLotWeightsLiveData?.observe(requireActivity(), {
            if (it.isNotEmpty()) {
                updateUIElements()
            } else {
                showPopupMessage(getString(R.string.parking_full))
            }
        })
    }

    private fun requestForLocationPermission(){
        when {
            checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocationAndParkingLots(true)
                apiHandler.post(apiRunnable)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) ->{
                AlertDialog.Builder(requireActivity())
                    .setTitle("Location Permission Needed")
                    .setMessage("This application needs the location permission to find the most suitable parking lot.")
                    .setPositiveButton("Settings") { dialog, which ->
                        dialog.dismiss()
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", requireActivity().packageName, null)
                        })

                    }
                    .setNegativeButton("Cancel") { dialog, which ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
            else -> {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    PERMISSIONS_REQUEST_LOCATION
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getCurrentLocationAndParkingLots(true)
                    apiHandler.post(apiRunnable)
                } else {
                    Toast.makeText(
                        requireActivity(),
                        "Permission denied to access location",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun getCurrentLocationAndParkingLots(showLoader: Boolean) {
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener(requireActivity()) { location ->
                    location?.let {
                        val currentLocation = Pair(it.latitude, it.longitude)
                        homeViewModel.getParkingLots(currentLocation, showLoader)
                    }
                }
                .addOnFailureListener(requireActivity()) { e ->
                    Log.e("Location Error", e.message ?: "Unknown Error")
                }
            return
        }
    }


    private val apiRunnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.N)
        override fun run() {
            if (lastSearchedLocationLat != null && lastSearchedLocationLng != null) {
                val currentLocation = Pair(
                    lastSearchedLocationLat ?: 0.0,
                    lastSearchedLocationLng ?: 0.0
                )
                homeViewModel.getParkingLots(currentLocation, false)

            } else if (isSearchBarEmpty) {
//                isSearchBarEmpty = false
                lastSearchedLocationLat = null
                lastSearchedLocationLng = null
                getCurrentLocationAndParkingLots(false)
            }
            apiHandler.postDelayed(this, 30000)
        }
    }


    private fun openGoogleMaps() {
        val label = "Destination Label"
        val gmmIntentUri =
            Uri.parse("google.navigation:q=$lastSearchedLocationLat,$lastSearchedLocationLng&label=$label")
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
                Toast.LENGTH_LONG
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

        homeViewModel.nearestParkingLotLiveData?.observe(requireActivity(), {
            parkingLotName = it.parking_lot_name
            val spotsAvailable = it.number_of_empty_parking_slots
            val locale: Locale = Locale.getDefault()
            val translatedLocationName = when (locale.language) {
                "mr" -> CommonUtils.translate(
                    parkingLotName,
                    "Mr"
                )
                "es" -> CommonUtils.translate(
                    parkingLotName,
                    "Es"
                )
                else -> parkingLotName
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

            updateParkingLocationOnMap(googleMap, it.latitude, it.longitude)
        })

        homeViewModel.durationInSecLiveData?.observe(requireActivity(), {
            val durationInMilliSec = it?.let { it * 1000 }
            timeToReach =
                durationInMilliSec?.let { it -> System.currentTimeMillis() + it }?.toLong()!!



            if (it > MIN_RADIUS_CHECK && showAlert) {
                val builder = AlertDialog.Builder(requireActivity())
                builder.setTitle(R.string.min_radius_alert_title)
                builder.setMessage(R.string.min_radius_alert_text)
                builder.setPositiveButton(R.string.min_radius_alert_ok) { dialog, _ ->
                    dialog.dismiss()
                }
                val dialog: AlertDialog = builder.create()
                if (!dialog.isShowing) {
                    dialog.show()
                }
                showAlert = false
            }
        })
    }

    private fun showProgressDialog(message: String) {
        binding.progressBarLayout.visibility = View.VISIBLE
    }

    private fun hideProgressDialog() {
        binding.progressBarLayout.visibility = View.GONE
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mapFragment?.getMapAsync { googleMap ->
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
                    isSearchActivityLaunched =
                        true
                    binding.tilLocation.setEndIconDrawable(R.drawable.ic_cross)
                    val place: Place = Autocomplete.getPlaceFromIntent(data!!)
                    lastSearchedLocationLat = place.latLng?.latitude
                    lastSearchedLocationLng = place.latLng?.longitude
                    val currentLocation = Pair(
                        lastSearchedLocationLat ?: 0.0,
                        lastSearchedLocationLng ?: 0.0
                    )
                    homeViewModel.getParkingLots(currentLocation,true)
                    isSearchBarEmpty = false
                    binding.searchBar.isFocusableInTouchMode = true
                    binding.searchBar.requestFocus()
                    binding.searchBar.setText(place.name)
                    showAlert = true
                    onMapReady(googleMap)
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.e("Cancelled", "Cancelled")
                isSearchActivityLaunched =
                    true
            }
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        googleMap.setOnMyLocationButtonClickListener {

                if (checkSelfPermission(
                        requireActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        requireActivity(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    googleMap.isMyLocationEnabled = true
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            val latLng = LatLng(it.latitude, it.longitude)
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                        }
                    }

                }

            true
        }
    }

    private fun showPopupMessage(message: String) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.min_radius_alert_ok) { dialog, _ ->
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
        const val MIN_RADIUS_CHECK = 9000.0

    }

}