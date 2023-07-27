package com.example.msproject.view_model.http

import android.util.Log
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import com.example.msproject.view_model.Location
import com.google.gson.Gson

class HttpRequest {

    fun callParkingLotsApi(callback: (String?) -> Unit) {
        val url = "https://658ytxfrod.execute-api.us-east-1.amazonaws.com/dev/parking_lots"
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
                callback(responseString)
            }
        })
    }


    fun callParkingLotApi(parkingLotName: String, callback: (String?) -> Unit) {
        val url = "http://10.0.2.2:8000/api/data/parkingLot/$parkingLotName"
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
                callback(responseString)
            }
        })
    }


}