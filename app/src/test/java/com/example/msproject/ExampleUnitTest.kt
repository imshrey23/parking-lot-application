package com.example.msproject

import android.content.ContentResolver
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.example.msproject.api.LoadingState
import com.example.msproject.common.CommonUtils
import com.example.msproject.model.ParkingLot
import com.example.msproject.ui.home.HomeViewModel
import com.google.android.gms.maps.GoogleMap
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.capture
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import okhttp3.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.capture
import org.powermock.api.mockito.PowerMockito
import java.io.IOException
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@RunWith(MockitoJUnitRunner::class)
class CommonUtilsTest {

    @Test
    fun testIsMapsInstalled() {
        val packageManager = mock(PackageManager::class.java)
        val packageName = "com.google.android.apps.maps"

        // When Google Maps is installed
        `when`(packageManager.getPackageInfo(packageName, 0)).thenReturn(mock(PackageInfo::class.java))
        Assert.assertTrue(CommonUtils.isMapsInstalled(packageManager) == true)

        // When Google Maps is not installed
        `when`(packageManager.getPackageInfo(packageName, 0)).thenThrow(PackageManager.NameNotFoundException())
        Assert.assertTrue(CommonUtils.isMapsInstalled(packageManager) == false) // Corrected to assertTrue
    }

    @Test
    fun testGetDeviceId() {
        val contentResolver = mock(ContentResolver::class.java)
        val androidId = "test_android_id"

        // Use mockStatic to create a mock for the Settings.Secure class
        mockStatic(Settings.Secure::class.java).use { mock ->
            // When getString is called on the ContentResolver with the specific key, return the mock value
            `when`(Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)).thenReturn(androidId)

            // Now call the method under test
            assertEquals(androidId, CommonUtils.getDeviceId(contentResolver))

            // Optionally, you can verify that the method was called with the expected parameters
            mock.verify { Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) }
        }
    }




    @Test
    fun testGetDrivingDurationFromDistanceMatrixApiResponse() {
        // Prepare a valid JSON response
        val response = """
            {
                "rows": [
                    {
                        "elements": [
                            {
                                "duration": { "value": 300 }
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        CommonUtils.getDrivingDurationFromDistanceMatrixApiResponse(response)
            ?.let { assertEquals(300.0, it, 0.0) }

        // Prepare an invalid JSON response
        val invalidResponse = """ { "rows": [] } """
        Assert.assertNull(
            CommonUtils.getDrivingDurationFromDistanceMatrixApiResponse(
                invalidResponse
            )
        )
    }
}
//@RunWith(MockitoJUnitRunner::class)
//class HomeViewModelTest {
//
//    // The ViewModel to be tested
//    private lateinit var homeViewModel: HomeViewModel
//
//    // Mock the dependencies
//    @Mock
//    private lateinit var mockGoogleMap: GoogleMap
//
//    @Mock
//    private lateinit var mockLoadingStateLiveData: MutableLiveData<LoadingState>
//
//    @Mock
//    private lateinit var mockParkingLotWeightsLiveData: MutableLiveData<MutableList<Pair<ParkingLot, Double>>>
//
//    @Mock
//    private lateinit var mockDurationInSecLiveData: MutableLiveData<Double>
//
//    @get:Rule
//    val rule= InstantTaskExecutorRule()
//
//    @Before
//    fun setup() {
//        // Initialize the ViewModel with mocked dependencies
//        homeViewModel = HomeViewModel().apply {
//            loadingStateLiveData = mockLoadingStateLiveData
//            parkingLotWeightsLiveData = mockParkingLotWeightsLiveData
//            durationInSecLiveData = mockDurationInSecLiveData
//        }
//    }
//
//    @Test
//    fun testGetParkingLotsApi_Success() {
//        // Arrange
//        val currentLocation = Pair(10.0, 20.0)
//
//        // Act
//        homeViewModel.getParkingLotsApi(currentLocation, mockGoogleMap, true)
//
//        // Assert
//        // Add any additional assertions you need to verify the behavior after the API call.
//    }
//
//    // Add more test cases as needed.
//
//}
