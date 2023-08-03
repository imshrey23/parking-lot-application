package com.example.msproject

import androidx.lifecycle.MutableLiveData
import com.example.msproject.api.LoadingState
import com.example.msproject.model.ParkingLot
import com.example.msproject.ui.home.HomeViewModel
import com.google.android.gms.maps.GoogleMap
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.any


import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class HomeViewModelTest {

    // The ViewModel to be tested
    private lateinit var homeViewModel: HomeViewModel

    // Mock the dependencies
    @Mock
    private lateinit var mockGoogleMap: GoogleMap

    @Mock
    private lateinit var mockLoadingStateLiveData: MutableLiveData<LoadingState>

    @Mock
    private lateinit var mockParkingLotWeightsLiveData: MutableLiveData<MutableList<Pair<ParkingLot, Double>>>

    @Mock
    private lateinit var mockDurationInSecLiveData: MutableLiveData<Double>

    @Before
    fun setup() {
        // Initialize the ViewModel with mocked dependencies
        homeViewModel = HomeViewModel().apply {
            loadingStateLiveData = mockLoadingStateLiveData
            parkingLotWeightsLiveData = mockParkingLotWeightsLiveData
            durationInSecLiveData = mockDurationInSecLiveData
        }
    }

    @Test
    fun testGetParkingLotsApi_Success() {
        // Arrange
        val currentLocation = Pair(10.0, 20.0)
        `when`(mockParkingLotWeightsLiveData.postValue(any())).thenAnswer {
            // Capture the posted value to the LiveData and verify it later
            val argument: MutableList<Pair<ParkingLot, Double>> =
                it.arguments[0] as MutableList<Pair<ParkingLot, Double>>
            assert(argument.isNotEmpty()) // Add more assertions based on your test case

            // Return any value here if needed
            null
        }
        `when`(mockDurationInSecLiveData.postValue(any())).thenAnswer {
            // Capture the posted value to the LiveData and verify it later
            val argument: Double = it.arguments[0] as Double
            assert(argument > 0) // Add more assertions based on your test case

            // Return any value here if needed
            null
        }

        // Act
        homeViewModel.getParkingLotsApi(currentLocation, mockGoogleMap, true)

        // Assert
        // Add any additional assertions you need to verify the behavior after the API call.
    }

    // Add more test cases as needed.

}