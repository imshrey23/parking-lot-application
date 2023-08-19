package com.example.msproject.com.example.msproject.api.ApiService

import android.util.Log
import com.example.msproject.BuildConfig
import com.example.msproject.com.example.msproject.model.ParkingLotInfo
import com.example.msproject.api.model.ParkingLotsResponse
import com.example.msproject.api.model.distance.DistanceMatrixResponse
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.net.URL

class ParkingServiceImpl :
    ParkingService {

    override suspend fun getParkingLots(): ParkingLotsResponse? {
        val response: Response =
            ServiceBuilder.getBuilder(ApiConstant.PARKING_LOTS_API, RequestType.GET)
        var parkingLotsResponse: ParkingLotsResponse? = null

        if (response.isSuccessful) {
            val responseString = response.body?.string()
            parkingLotsResponse = Gson().fromJson(responseString, ParkingLotsResponse::class.java)
        }
        return parkingLotsResponse
    }

    override suspend fun getParkingLotInfo(parkingLotName: String): ParkingLotInfo? {
        val response: Response =
            ServiceBuilder.getBuilder(ApiConstant.PARKING_LOT_API + parkingLotName, RequestType.GET)
        var parkingLotInfoResp: ParkingLotInfo? = null

        if (response.isSuccessful) {
            val responseString = response.body?.string()
            parkingLotInfoResp = Gson().fromJson(responseString, ParkingLotInfo::class.java)
        }
        return parkingLotInfoResp
    }

    override suspend fun reserveParkingSpot(parkingLotName: String, deviceId: String, timeToReach: Long) {
        val jsonData = JSONObject()
        jsonData.put("parkingLotName", parkingLotName)
        jsonData.put("deviceId", deviceId)
        jsonData.put("timeToReachDestination", timeToReach)

        val requestBody = jsonData.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val response: Response =
            ServiceBuilder.getBuilder(ApiConstant.UPDATE_PARKING_LOT, RequestType.POST, requestBody)

        if (response.isSuccessful) {
            Log.i("reserveParkingSpot", "Reserved Parking Spot.")
        } else {
            Log.e("reserveParkingSpot", "Error in reserving parking spot.")
        }
    }

    override suspend fun getDistanceMatrix(
        origin: Pair<Double, Double>,
        destination: Pair<Double, Double>
    ): Double {
        val url = ApiConstant.DISTANCE_MATRIX +
                "origins=${origin.first},${origin.second}&" +
                "destinations=${destination.first},${destination.second}&" +
                "mode=driving&" +
                "departure_time=now&" +
                "key=${BuildConfig.PLACES_API_KEY}"

        val response = URL(url).readText()
        val distanceMatrixResponse = Gson().fromJson(response, DistanceMatrixResponse::class.java)

        if (distanceMatrixResponse.rows.isNotEmpty() &&
            distanceMatrixResponse.rows[0].elements.isNotEmpty() &&
            distanceMatrixResponse.rows[0].elements[0].duration != null
        ) {
            return distanceMatrixResponse.rows[0].elements[0].duration?.value?.toDouble()
                ?: throw Exception("Duration value is unexpectedly null.")
        } else {
            throw Exception("Invalid response format or missing data.")
        }
    }


}



