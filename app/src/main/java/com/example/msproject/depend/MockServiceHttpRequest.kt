package com.example.msproject.depend

import androidx.lifecycle.MutableLiveData
import com.example.msproject.api.LoadingState

class MockServiceHttpRequest {

    fun callParkingLotsApi(
        loadingStateLiveData: MutableLiveData<LoadingState>,
        showProgressLoader: Boolean,
        callback: (apiResponse: String?) -> Unit
    ) {
        // Simulate the behavior of the API response here and invoke the callback.
        // You can provide different responses based on different test cases.
        val apiResponse = "Mock API response" // Replace this with your desired response.
        callback(apiResponse)
    }

    // Add other mocked methods as needed.
}
