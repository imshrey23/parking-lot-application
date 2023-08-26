package com.example.msproject.api.apiService

import android.util.Log
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.powermock.core.classloader.annotations.PrepareForTest
import java.net.URL

@RunWith(JUnit4::class)
@PrepareForTest(URL::class)
class ParkingServiceImplTest {

        init {
            mockkObject(ServiceBuilder)
        }

    @MockK
    lateinit var response: Response

    @MockK
    lateinit var responseBody: ResponseBody

    @InjectMockKs
    lateinit var parkingServiceImpl: ParkingServiceImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }


    @Test
    fun `test getParkingLots success`() = runBlocking {
        val json = """{
    "parking_lots": [
        {
            "latitude": "44.568078130296776", 
            "longitude": "44.568078130296776",
            "image_url": "https://image.com",
            "number_of_empty_parking_slots": 12,
            "parking_charges": "2",
            "parking_lot_name": "Johnson Hall",
            "parking_lot_time_limit": "2 Hr Parking [ 7.00 am to 5.00 pm]",
            "timestamp": "2023-05-30 20:14:51",
            "total_number_of_parking_lots": 30
        }
    ]
}"""


        every { response.isSuccessful } returns true
        every { response.body } returns responseBody
        every { responseBody.string() } returns json
        every { ServiceBuilder.getBuilder(ApiConstant.PARKING_LOTS_API, RequestType.GET) } returns response

        val result = parkingServiceImpl.getParkingLots()

        assertNotNull(result)
        assertEquals(1, result?.parking_lots?.size)
        assertEquals("Johnson Hall", result?.parking_lots?.get(0)?.parking_lot_name)


        verify { ServiceBuilder.getBuilder(ApiConstant.PARKING_LOTS_API, RequestType.GET) }
    }

    @Test
    fun `test getParkingLots failure`() = runBlocking {

        every { response.isSuccessful } returns false
        every { ServiceBuilder.getBuilder(ApiConstant.PARKING_LOTS_API, RequestType.GET) } returns response

        var exceptionThrown: Exception? = null
        try {
            parkingServiceImpl.getParkingLots()
        } catch (e: Exception) {
            exceptionThrown = e
        }

        assertNotNull(exceptionThrown)
        assertEquals("Failed to fetch parking lots.", exceptionThrown?.message)

        verify { ServiceBuilder.getBuilder(ApiConstant.PARKING_LOTS_API, RequestType.GET) }
    }

    @Test
    fun `test getParkingLotInfo success`() = runBlocking {
        val json = """{
    "parkingLotName": "Johnson Hall",
    "numberOfUsers": 100
}"""

        every { response.isSuccessful } returns true
        every { response.body } returns responseBody
        every { responseBody.string() } returns json
        every { ServiceBuilder.getBuilder(ApiConstant.PARKING_LOT_API + "Johnson Hall", RequestType.GET) } returns response

        val result = parkingServiceImpl.getParkingLotInfo("Johnson Hall")

        assertNotNull(result)
        assertEquals("Johnson Hall", result?.parkingLotName)
        assertEquals(100, result?.numberOfUsers)

        verify { ServiceBuilder.getBuilder(ApiConstant.PARKING_LOT_API + "Johnson Hall", RequestType.GET) }
    }


    @Test
    fun `test getParkingLotInfo failure`() = runBlocking {
        every { response.isSuccessful } returns false
        every { ServiceBuilder.getBuilder(ApiConstant.PARKING_LOT_API + "Johnson Hall", RequestType.GET) } returns response

        var exceptionThrown: Exception? = null
        try {
            parkingServiceImpl.getParkingLotInfo("Johnson Hall")
        } catch (e: Exception) {
            exceptionThrown = e
        }

        assertNotNull(exceptionThrown)

        verify { ServiceBuilder.getBuilder(ApiConstant.PARKING_LOT_API + "Johnson Hall", RequestType.GET) }
    }

    @Test
    fun `test reserveParkingSpot success`() = runBlocking {

        val jsonData = JSONObject().apply {
            put("parkingLotName", "parkingLotName")
            put("deviceId", "12343")
            put("timeToReachDestination", 1234455)
        }

        val requestBody = jsonData.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        every { response.isSuccessful } returns true

        every { ServiceBuilder.getBuilder(ApiConstant.UPDATE_PARKING_LOT, RequestType.POST, requestBody) } returns response

        var exceptionThrown: Exception? = null
        try {
            parkingServiceImpl.reserveParkingSpot("Johnson Hall", "12343", 1234343)
        } catch (e: Exception) {
            exceptionThrown = e
        }

        assert(response.isSuccessful)
    }

    @Test
    fun `test reserveParkingSpot failure`() = runBlocking {

        val jsonData = JSONObject()
        jsonData.put("parkingLotName", "parkingLotName")
        jsonData.put("deviceId", "12343")
        jsonData.put("timeToReachDestination", 1234455)

        val requestBody = jsonData.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        every { response.isSuccessful } returns false
        every { ServiceBuilder.getBuilder(ApiConstant.UPDATE_PARKING_LOT,  RequestType.POST,requestBody)} returns response


        var exceptionThrown: Exception? = null
        try {
            parkingServiceImpl.reserveParkingSpot("Johnson Hall","12343",1234343)
        } catch (e: Exception) {
            exceptionThrown = e
        }

        assertNotNull(exceptionThrown)
        assert( !response.isSuccessful )
    }
}
