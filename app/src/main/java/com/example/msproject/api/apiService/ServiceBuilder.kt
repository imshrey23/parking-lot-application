package com.example.msproject.api.apiService

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response

object ServiceBuilder {
    fun getBuilder(url: String, methodType: RequestType, body: RequestBody? = null): Response {
        val okHttpClient = OkHttpClient()
        val request = Request.Builder()
        request.url(url)
        when (methodType) {
            RequestType.GET -> request.get()
            RequestType.POST -> body?.let { request.post(it) }
        }

        return okHttpClient.newCall(request.build()).execute()
    }
}