package com.example.msproject.api

import android.util.Log
import com.example.msproject.BuildConfig
import com.example.msproject.com.example.msproject.model.ParkingLotInfo
import com.example.msproject.model.ParkingLotsResponse
import com.google.gson.Gson
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ApiService {

    fun getParkingLots(
        callback: (ParkingLotsResponse?) -> Unit
    ) {
        val url = ApiConstant.PARKING_LOTS_API
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("API Error", e.message ?: "Unknown Error")
                callback(null)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {

                val responseString = response.body?.string()
                val parkingLotResponse = Gson().fromJson(responseString, ParkingLotsResponse::class.java)
                callback(parkingLotResponse)
            }
        })
    }


    fun getParkingLotInfo(parkingLotName: String, callback: (ParkingLotInfo?) -> Unit) {
        val url = ApiConstant.PARKING_LOT_API + parkingLotName
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("API Error", e.message ?: "Unknown Error")
                callback(null)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                val responseString = response.body?.string()
                val parkingLotResponse = Gson().fromJson(responseString, ParkingLotInfo::class.java)
                callback(parkingLotResponse)
            }
        })
    }


    fun deleteExpiredDocuments() {
        try {
            val url = URL(ApiConstant.DELETE_OLD_RECORD)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "DELETE"
            connection.connectTimeout = 5000

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.i("deleteExpiredDocuments", "Deleted old data.")
            } else {
                Log.e("deleteExpiredDocuments","Failed to delete old data.")
            }

            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun reserveParkingSpot(parkingLotName: String, deviceId: String, timeToReach: Long) {
        val urlString = ApiConstant.UPDATE_PARKING_LOT

        Thread {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                // TODO
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")

                val jsonData = JSONObject()
                jsonData.put("parkingLotName", parkingLotName)
                jsonData.put("deviceId", deviceId)
                jsonData.put("timeToReachDestination", timeToReach)

                val output = BufferedOutputStream(connection.outputStream)
                output.write(jsonData.toString().toByteArray())
                output.flush()
                output.close()

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.i(String.toString(), "Success")
                } else {
                    Log.i(String.toString(), "Error")
                }

                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // Used only by findNearestParkingLot
    fun callDistanceMatrixApi(
        origin: Pair<Double, Double>,
        destination: Pair<Double, Double>
    ): String {
        val url = ApiConstant.DISTANCE_MATRIX +
                "origins=${origin.first},${origin.second}&" +
                "destinations=${destination.first},${destination.second}&" +
                "mode=driving&" +
                "key=${BuildConfig.PLACES_API_KEY}"
        return URL(url).readText()
    }

}


