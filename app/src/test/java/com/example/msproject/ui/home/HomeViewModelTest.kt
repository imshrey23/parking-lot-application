package com.example.msproject.ui.home


import com.example.msproject.MainDispatcherRule
import com.example.msproject.api.apiService.LoadingState
import com.example.msproject.api.apiService.ParkingService
import com.example.msproject.api.model.parking_lots
import com.example.msproject.api.model.ParkingLotsResponse
import com.example.msproject.com.example.msproject.model.ParkingLotInfo
import io.mockk.*
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*



class HomeViewModelTest {

    private var parkingService: ParkingService = mockk()
    private lateinit var viewModel: HomeViewModel
    private lateinit var loadingState: MutableList<LoadingState>
    private val parkingLotUsersCount: MutableMap<String, MutableSet<String>> = mutableMapOf()

    @get:Rule
    val rule = MainDispatcherRule()

    @After
    fun tearDown() {
        clearAllMocks()
    }


    @Test
    fun `Given showProgressLoader is true when getParkingLots is called and fails, should return FAILURE loading state`()= runTest {
        viewModel = HomeViewModel(parkingService)
        loadingState = mutableListOf()
        viewModel.loadingStateLiveData.observeForever {
            loadingState.add(it)
        }

        val location = Pair(0.0, 0.0)
        val showProgressLoader = true

        coEvery {
            parkingService.getParkingLots()
        } returns null

        viewModel.getParkingLots(location, showProgressLoader)

        coVerify {
            parkingService.getParkingLots()
        }
        advanceUntilIdle()
        assertEquals(LoadingState.FAILURE, viewModel.loadingStateLiveData.value)
    }


    @Test
    fun `Given showProgressLoader is true when getParkingLots is called and succeeds, should return SUCCESS loading state`() = runTest {

        viewModel = HomeViewModel(parkingService)

        loadingState = mutableListOf()

        viewModel.loadingStateLiveData.observeForever {
            loadingState.add(it)
        }

        val location = Pair(0.0, 0.0)
        val showProgressLoader = true
        val response = ParkingLotsResponse(
            listOf(
                parking_lots("1234", 0.0, 0.0, "https://detectionlog.s3.amazonaws.com/original_images/2023-05-14T13:42:24.838578_uu8ca0j.jpg", 10, "$10", "Lot1", "1hr", "timestamp", 20)
            )
        )

        coEvery { parkingService.getParkingLots() } returns response

        viewModel.getParkingLots(location, showProgressLoader)

        advanceTimeBy(1000L)

        coVerify { parkingService.getParkingLots() }

        assertEquals(LoadingState.SUCCESS, viewModel.loadingStateLiveData.value)
    }


    @Test
    fun `Given showProgressLoader is true when getParkingLotInfo is called and succeeds, should return list of parking lots info`()= runTest  {
        viewModel = HomeViewModel(parkingService)
        loadingState = mutableListOf()
        viewModel.loadingStateLiveData.observeForever {
            loadingState.add(it)
        }
        val response = ParkingLotsResponse(
            listOf(
                parking_lots("1234",0.0, 0.0, "https://detectionlog.s3.amazonaws.com/original_images/2023-05-14T13:42:24.838578_uu8ca0j.jpg", 10, "$10", "Lot1", "1hr", "timestamp", 20)
            )
        )
        val parkingLotInfoResp = ParkingLotInfo("Lot1", 12)

        parkingLotUsersCount["Lot1"] = mutableSetOf("12")
        coEvery {
            parkingService.getParkingLotInfo("Lot1")
        } returns parkingLotInfoResp

        viewModel.getNumberOfUsersForParkingLots(response)

        advanceTimeBy(1000L)

        coVerify {
            parkingService.getParkingLotInfo(response.parking_lots[0].parking_lot_name)
        }

        assertEquals(
            parkingLotUsersCount["Lot1"].toString(),
            viewModel.parkingLotUsersCount["Lot1"].toString()
        )

    }

    @Test
    fun `Given current location when getNearestParkingLot is called, should verify correct parking lot is found`()= runTest  {
        viewModel = HomeViewModel(parkingService)
        loadingState = mutableListOf()
        viewModel.loadingStateLiveData.observeForever {
            loadingState.add(it)
        }
        val currentLocation = Pair(44.57317333333334, -123.282065)
        val mockApiResponse = ParkingLotsResponse(listOf(
            parking_lots("1234",44.568078130296776, -123.279363220437, "https://detectionlog.s3.amazonaws.com/original_images/2023-05-14T13:42:24.838578_uu8ca0j.jpg", 12, "2", "Johnson Hall", "2 Hr Parking [ 7.00 am to 5.00 pm]", "2023-04-29 18:51:40", 36),
            parking_lots("1234",44.56298278509426, -123.27235573138302, "https://detectionlog.s3.amazonaws.com/original_images/2023-05-30T20:14:51.395821_gotlchv.jpg", 3, "2", "Tebeau Hall", "2 Hr Parking [ 8.30 am to 5.30 am]", "2023-05-30 20:14:51", 41),
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
        val expectedList: MutableList<Pair<parking_lots, Double>> = mutableListOf()
        expectedList.add(Pair(mockApiResponse.parking_lots[0], expectedWeight))
        expectedList.add(Pair(mockApiResponse.parking_lots[1], expectedWeight1))

        advanceUntilIdle()
        assertEquals(expectedList,
            viewModel.parkinglotsWeightsLiveData?.value
        )
        assertEquals(mockApiResponse.parking_lots[0], viewModel.nearestParkinglotsLiveData?.value)
        assertEquals(60.0, viewModel.durationInSecLiveData?.value)
        assertTrue(viewModel.parkinglotsWeightsLiveData?.value?.size == 2)
    }

    @Test
    fun `Given an exception when getParkingLots is called, should set loadingStateLiveData to FAILURE`() = runTest {
        viewModel = HomeViewModel(parkingService)
        loadingState = mutableListOf()
        viewModel.loadingStateLiveData.observeForever {
            loadingState.add(it)
        }
        val location = Pair(0.0, 0.0)
        val showProgressLoader = true

        coEvery {
            parkingService.getParkingLots()
        } throws Exception("Error")

        viewModel.getParkingLots(location, showProgressLoader)

        coVerify {
            parkingService.getParkingLots()
        }
        advanceUntilIdle()
        assertEquals(LoadingState.FAILURE, viewModel.loadingStateLiveData.value)
    }


    @Test
    fun `Given an exception when getNumberOfUsersForParkingLots is called, should log exception and not crash`() = runTest {
        viewModel = HomeViewModel(parkingService)
        loadingState = mutableListOf()
        viewModel.loadingStateLiveData.observeForever {
            loadingState.add(it)
        }
        val response = ParkingLotsResponse(
            listOf(
                parking_lots("1234",0.0, 0.0, "", 10, "$10", "Lot1", "1hr", "timestamp", 20)
            )
        )

        coEvery {
            parkingService.getParkingLotInfo("Lot1")
        } throws Exception("Error")

        viewModel.getNumberOfUsersForParkingLots(response)

        coVerify {
            parkingService.getParkingLotInfo(response.parking_lots[0].parking_lot_name)
        }
        advanceUntilIdle()
        // Assert that there was no change to the parkingLotUsersCount
        assertTrue(viewModel.parkingLotUsersCount.isEmpty())
    }

    @Test
    fun `Given no available spots when getNearestParkingLot is called, should not set nearestParkingLotLiveData`() = runTest {
        viewModel = HomeViewModel(parkingService)
        loadingState = mutableListOf()
        viewModel.loadingStateLiveData.observeForever {
            loadingState.add(it)
        }
        val currentLocation = Pair(44.57317333333334, -123.282065)
        val mockApiResponse = ParkingLotsResponse(listOf(
            parking_lots("1234",44.568078130296776, -123.279363220437, "", 0, "$10", "Johnson Hall", "2 Hr Parking", "timestamp", 0)
        ))

        viewModel.getNearestParkingLot(mockApiResponse, currentLocation)

        coVerify(exactly = 0) {
            parkingService.getETA(any(), any())
        }
        advanceUntilIdle()
        assertNull(viewModel.nearestParkinglotsLiveData?.value)
        assertTrue(viewModel.parkinglotsWeightsLiveData?.value.isNullOrEmpty())
    }

    @Test
    fun `Given no available spots when reserveParkingSpot, when destination is null`() =runTest {

        viewModel = HomeViewModel(parkingService)
        loadingState = mutableListOf()
        viewModel.loadingStateLiveData.observeForever {
            loadingState.add(it)
        }
        val currentLocation = Pair(44.57317333333334, -123.282065)
        val parkingLotName = "Johnson Hall"
        val deviceId = "12133"

        viewModel.destinationLocation = null
        coEvery() {
            parkingService.reserveParkingSpot(parkingLotName,deviceId,0)
        }

        viewModel.reserveParkingSpot(parkingLotName,deviceId,currentLocation)

        coVerify(exactly = 0) {
            parkingService.getETA(any(), any())
        }


    }

    @Test
    fun `Given available spot, when destination is not null, then getETA is invoked`() = runTest {
        viewModel = HomeViewModel(parkingService)
        loadingState = mutableListOf()
        viewModel.loadingStateLiveData.observeForever {
            loadingState.add(it)
        }
        val currentLocation = Pair(44.57317333333334, -123.282065)
        val parkingLotName = "Johnson Hall"
        val deviceId = "12133"
        val destinationLocation = Pair(44.573175, -123.282067)
        viewModel.destinationLocation = destinationLocation

        coEvery {
            parkingService.getETA(currentLocation, destinationLocation)
        } returns 500.0

        viewModel.reserveParkingSpot(parkingLotName, deviceId, currentLocation)

        coVerify(exactly = 1) {
            parkingService.getETA(currentLocation, destinationLocation)
        }
    }

    @Test
    fun `Given available spot and destination, then reserveParkingSpot is invoked with correct time`() = runTest {
        viewModel = HomeViewModel(parkingService)
        loadingState = mutableListOf()
        viewModel.loadingStateLiveData.observeForever {
            loadingState.add(it)
        }
        val currentLocation = Pair(44.57317333333334, -123.282065)
        val parkingLotName = "Johnson Hall"
        val deviceId = "12133"
        val destinationLocation = Pair(44.573175, -123.282067)
        viewModel.destinationLocation = destinationLocation
        val eta = 500L

        coEvery {
            parkingService.getETA(currentLocation, destinationLocation)
        } returns eta.toDouble()
        coEvery {
            parkingService.reserveParkingSpot(parkingLotName, deviceId, any())
        } just Runs

        val timestampSlot: CapturingSlot<Long> = slot()

        viewModel.reserveParkingSpot(parkingLotName, deviceId, currentLocation)

        coVerify(exactly = 1) {
            parkingService.reserveParkingSpot(parkingLotName, deviceId, capture(timestampSlot))
        }

        val expectedTimestamp = System.currentTimeMillis() + eta * 1000
        assertTrue(expectedTimestamp - timestampSlot.captured < 1000) // Assuming max 1 second difference
    }


    @Test
    fun `Given exception thrown, ensure exceptionHandler handles it`() = runTest {
        viewModel = HomeViewModel(parkingService)
        loadingState = mutableListOf()
        viewModel.loadingStateLiveData.observeForever {
            loadingState.add(it)
        }
        val currentLocation = Pair(44.57317333333334, -123.282065)
        val parkingLotName = "Johnson Hall"
        val deviceId = "12133"
        val destinationLocation = Pair(44.573175, -123.282067)
        viewModel.destinationLocation = destinationLocation

        coEvery {
            parkingService.getETA(currentLocation, destinationLocation)
        } throws Exception("Network error")

        viewModel.reserveParkingSpot(parkingLotName, deviceId, currentLocation)

    }


}

