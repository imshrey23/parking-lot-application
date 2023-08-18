package com.example.msproject.com.example.msproject.api.ApiService

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
//            RequestType.DELETE -> request.delete(body)
        }

        return okHttpClient.newCall(request.build()).execute()
    }
}