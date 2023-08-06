package com.example.msproject.api

import androidx.lifecycle.MutableLiveData

interface HttpRequest {
    fun callParkingLotsApi(
        loadingStateLiveData: MutableLiveData<LoadingState>,
        showProgressLoader: Boolean,
        callback: (String?) -> Unit
    )
}