package com.example.msproject

import com.example.msproject.api.apiService.LoadingState
import com.example.msproject.api.apiService.ParkingService
import com.example.msproject.api.model.ParkingLot
import com.example.msproject.api.model.ParkingLotsResponse
import com.example.msproject.com.example.msproject.model.ParkingLotInfo
import com.example.msproject.ui.home.HomeViewModel
import com.google.android.gms.maps.GoogleMap
import io.mockk.*
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.*

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private var parkingService: ParkingService = mockk()
    private lateinit var viewModel: HomeViewModel
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var loadingState: MutableList<LoadingState>
    private val parkingLotUsersCount: MutableMap<String, MutableSet<String>> = mutableMapOf()

    @get:Rule
    val rule = MainDispatcherRule()

    @Before
    fun setUp() {
        viewModel = HomeViewModel(parkingService)
        loadingState = mutableListOf()
        viewModel.loadingStateLiveData.observeForever {
            loadingState.add(it)
        }
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }


    @Test
    fun `Given showProgressLoader is true when getParkingLots is called and fails, should return FAILURE loading state`() {

        val location = Pair(0.0, 0.0)
        val showProgressLoader = true

        coEvery {
            parkingService.getParkingLots()
        } returns null

        viewModel.getParkingLots(location, showProgressLoader)

        coVerify {
            parkingService.getParkingLots()
        }
        Assert.assertEquals(LoadingState.FAILURE, viewModel.loadingStateLiveData.value)
    }

    @Test
    fun `Given showProgressLoader is true when getParkingLots is called and succeeds, should return list of parking lots`() {

        val location = Pair(0.0, 0.0)
        val showProgressLoader = true
        val response = ParkingLotsResponse(
            listOf(
                ParkingLot(0.0, 0.0, "https://detectionlog.s3.amazonaws.com/original_images/2023-05-14T13:42:24.838578_uu8ca0j.jpg", 10, "$10", "Lot1", "1hr", "timestamp", 20)
            )
        )

        coEvery {
            parkingService.getParkingLots()
        } returns response

        viewModel.getParkingLots(location, showProgressLoader)

        Assert.assertEquals(LoadingState.LOADING, viewModel.loadingStateLiveData.value)
        coVerify {
            parkingService.getParkingLots()
        }

        Assert.assertEquals(LoadingState.SUCCESS, loadingState.last())

    }

    @Test
    fun `Given showProgressLoader is true when getParkingLotInfo is called and succeeds, should return list of parking lots info`() {
        val response = ParkingLotsResponse(
            listOf(
                ParkingLot(0.0, 0.0, "https://detectionlog.s3.amazonaws.com/original_images/2023-05-14T13:42:24.838578_uu8ca0j.jpg", 10, "$10", "Lot1", "1hr", "timestamp", 20)
            )
        )
        val parkingLotInfoResp = ParkingLotInfo("Lot1", 12)

        parkingLotUsersCount["Lot1"] = mutableSetOf("12")
        coEvery {
            parkingService.getParkingLotInfo("Lot1")
        } returns parkingLotInfoResp

        viewModel.getNumberOfUsersForParkingLots(response)

        coVerify {
            parkingService.getParkingLotInfo(response.parkingLots[0].parking_lot_name)
        }

        Assert.assertNotNull(parkingLotUsersCount)
        Assert.assertNotNull(parkingLotUsersCount["Lot1"])
        Assert.assertEquals(
            parkingLotUsersCount["Lot1"].toString(),
            viewModel.parkingLotUsersCount["Lot1"].toString()
        )

    }


    @Test
    fun `Given current location when getNearestParkingLot is called, should verify correct parking lot is found`()= runBlocking {
        val currentLocation = Pair(44.57317333333334, -123.282065)
        val mockApiResponse = ParkingLotsResponse(listOf(
            ParkingLot(44.568078130296776, -123.279363220437, "https://detectionlog.s3.amazonaws.com/original_images/2023-05-14T13:42:24.838578_uu8ca0j.jpg", 12, "2", "Johnson Hall", "2 Hr Parking [ 7.00 am to 5.00 pm]", "2023-04-29 18:51:40", 36),
            ParkingLot(44.56298278509426, -123.27235573138302, "https://detectionlog.s3.amazonaws.com/original_images/2023-05-30T20:14:51.395821_gotlchv.jpg", 3, "2", "Tebeau Hall", "2 Hr Parking [ 8.30 am to 5.30 am]", "2023-05-30 20:14:51", 41),
        ))

        coEvery { parkingService.getETA(Pair(44.57317333333334, -123.282065), Pair(44.568078130296776, -123.279363220437)) } returns 60.0
        coEvery { parkingService.getETA(Pair(44.57317333333334, -123.282065), Pair(44.56298278509426, -123.27235573138302)) } returns 50.0

        viewModel.getNearestParkingLot(mockApiResponse, currentLocation)
        // Then
        coVerify {
            parkingService.getETA(Pair(44.57317333333334, -123.282065), Pair(44.568078130296776, -123.279363220437))
            parkingService.getETA(Pair(44.57317333333334, -123.282065), Pair(44.56298278509426, -123.27235573138302))
        }

        val expectedWeight = 12 / 60.0
        val expectedWeight1 = 3 / 50.0
        val expectedList: MutableList<Pair<ParkingLot, Double>> = mutableListOf()
        expectedList.add(Pair(mockApiResponse.parkingLots[0], expectedWeight))
        expectedList.add(Pair(mockApiResponse.parkingLots[1], expectedWeight1))

        assertEquals(expectedList,
            viewModel.parkingLotWeightsLiveData?.value
        )
        assertEquals(mockApiResponse.parkingLots[0], viewModel.nearestParkingLotLiveData?.value)
        assertEquals(60.0, viewModel.durationInSecLiveData?.value)
        assertTrue(viewModel.parkingLotWeightsLiveData?.value?.size == 2)
    }

    @Test
    fun `Given an exception when getParkingLots is called, should set loadingStateLiveData to FAILURE`() = runBlocking {
        val location = Pair(0.0, 0.0)
        val showProgressLoader = true

        coEvery {
            parkingService.getParkingLots()
        } throws Exception("Error")

        viewModel.getParkingLots(location, showProgressLoader)

        coVerify {
            parkingService.getParkingLots()
        }
        Assert.assertEquals(LoadingState.FAILURE, viewModel.loadingStateLiveData.value)
    }


    @Test
    fun `Given an exception when getNumberOfUsersForParkingLots is called, should log exception and not crash`() = runBlocking {
        val response = ParkingLotsResponse(
            listOf(
                ParkingLot(0.0, 0.0, "", 10, "$10", "Lot1", "1hr", "timestamp", 20)
            )
        )

        coEvery {
            parkingService.getParkingLotInfo("Lot1")
        } throws Exception("Error")

        viewModel.getNumberOfUsersForParkingLots(response)

        coVerify {
            parkingService.getParkingLotInfo(response.parkingLots[0].parking_lot_name)
        }
        // Assert that there was no change to the parkingLotUsersCount
        Assert.assertTrue(viewModel.parkingLotUsersCount.isEmpty())
    }

    @Test
    fun `Given no available spots when getNearestParkingLot is called, should not set nearestParkingLotLiveData`() = runBlocking {
        val currentLocation = Pair(44.57317333333334, -123.282065)
        val mockApiResponse = ParkingLotsResponse(listOf(
            ParkingLot(44.568078130296776, -123.279363220437, "", 0, "$10", "Johnson Hall", "2 Hr Parking", "timestamp", 0)
        ))

        viewModel.getNearestParkingLot(mockApiResponse, currentLocation)

        coVerify(exactly = 0) {
            parkingService.getETA(any(), any())
        }

        Assert.assertNull(viewModel.nearestParkingLotLiveData?.value)
        assertTrue(viewModel.parkingLotWeightsLiveData?.value.isNullOrEmpty())
    }

}
