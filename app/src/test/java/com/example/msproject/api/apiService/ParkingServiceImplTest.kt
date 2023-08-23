package com.example.msproject.api.apiService

import com.google.gson.JsonObject
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ParkingServiceImplTest {

    companion object {
        init {
            mockkObject(ServiceBuilder)
        }
    }


    @MockK
    lateinit var response: Response

    @MockK
    lateinit var responseBody: ResponseBody

    @MockK
    lateinit var jsonObject: JSONObject

    @InjectMockKs
    lateinit var parkingServiceImpl: ParkingServiceImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }


    @Test
    fun `test getParkingLots success`() = runBlocking {
        val json = """{
    "parkingLots": [
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
        assertEquals(1, result?.parkingLots?.size)
        assertEquals("Johnson Hall", result?.parkingLots?.get(0)?.parking_lot_name)


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
}
