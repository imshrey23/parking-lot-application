package com.example.msproject

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.msproject.databinding.ActivityMainBinding
import com.example.msproject.view_model.MainViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
//import kotlinx.android.synthetic.main.activity_main.*
//import kotlinx.android.synthetic.main.activity_main.search_bar

import java.util.*



class MainActivity : AppCompatActivity(), View.OnClickListener, OnMapReadyCallback {

//    var binding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
    lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var currentLocationMarker: Marker? = null

    lateinit var progressDialog: ProgressDialog

    fun showProgressDialog(message: String) {
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage(message)
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    fun hideProgressDialog() {
        if (::progressDialog.isInitialized && progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }


    private val viewModel by lazy {
        com.example.msproject.view_model.MainViewModel(this)
    }


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyCvSl5ugDPB8g_NPPEtK2NwMqB6D0zzF0Y")
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_LOCATION
            )
            viewModel.getCurrentLocationAndSendToAPI()
            viewModel.apiHandler.post(viewModel.apiRunnable)
        } else {
            viewModel.getCurrentLocationAndSendToAPI()
            viewModel.apiHandler.post(viewModel.apiRunnable)
        }

        val latitude = 29.636137668369987 // destination latitude
        val longitude = -123.279363220437  // destination longitude
        val label = "Destination Label" // destination label for display
        val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude&label=$label")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        val mapsPackage = "com.google.android.apps.maps"

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.fabMoreInfo.setOnClickListener{
            binding.fabMoreInfo.bringToFront()
            val intent = Intent(this, MoreInfoActivity::class.java)
            intent.putExtra("parking_charges", viewModel.view.parkingCharges)
            intent.putExtra("parkingImageUrl" , viewModel.view.parkingImageUrl)
            intent.putExtra("timestamp" , viewModel.view.timestamp)
            startActivity(intent)
        }

        binding.fabNavigation.setOnClickListener{
            binding.fabNavigation.bringToFront()
            val isMapsInstalled = try {
                packageManager.getPackageInfo(mapsPackage, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
            if (isMapsInstalled) {
                val gmmIntentUri = Uri.parse("google.navigation:q=${viewModel.parkingLatitude},${viewModel.parkingLongitude}&label=$label")
                mapIntent.data = gmmIntentUri
                mapIntent.setPackage(mapsPackage)
                startActivity(mapIntent)
            } else {
                Toast.makeText(this, "Google Maps is not installed on this device", Toast.LENGTH_SHORT).show()
            }
        }

        binding.searchBar.setOnClickListener(this)
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.isSearchBarEmpty = s?.isEmpty() ?: true
                if (viewModel.isSearchBarEmpty) {
                    binding.searchBar.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                        R.drawable.search_bar_corner, 0)
                    binding.searchBar.setOnClickListener(this@MainActivity)
                    viewModel.lastSearchedLocationLat = null
                    viewModel.lastSearchedLocationLng = null

                } else {
                    binding.searchBar.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                        R.drawable.ic_cross, 0)
                    binding.searchBar.setOnClickListener {
                        binding.searchBar.text = null
                        viewModel.lastSearchedLocationLat = null
                        viewModel.lastSearchedLocationLng = null
                    }

                    if (!viewModel.isSearchActivityLaunched) {
                        val intent = Autocomplete.IntentBuilder(
                            AutocompleteActivityMode.FULLSCREEN,
                            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
                        ).build(this@MainActivity)
                        startActivityForResult(intent, MainViewModel.PLACE_AUTOCOMPLETE_REQUEST_CODE)
                        viewModel.isSearchActivityLaunched = true
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Do nothig
            }
        })

        binding.tilLocation.setEndIconOnClickListener {
            binding.searchBar.setText("")
            binding.searchBar.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.search_bar_corner, 0)
            viewModel.isSearchBarEmpty = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
    }

    override fun onClick(v: View?) {
        viewModel.onClick(v)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync { map ->
            viewModel.onActivityResult(requestCode, resultCode, data, map)
        }
    }



    override fun onMapReady(googleMap: GoogleMap) {
        viewModel.onMapReady(googleMap)

        // Enable the "My Location" button and handle its click event
        googleMap.isMyLocationEnabled = true
        googleMap.setOnMyLocationButtonClickListener {
            // Check location permission before accessing the current location
            if (ContextCompat.checkSelfPermission(
                    this,
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
                    this,
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
        viewModel.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        const val PERMISSIONS_REQUEST_LOCATION = 100
    }
}
