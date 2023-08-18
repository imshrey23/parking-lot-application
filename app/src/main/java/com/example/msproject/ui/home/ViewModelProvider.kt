package com.example.msproject.com.example.msproject.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.msproject.com.example.msproject.api.ApiService.ParkingLotsApiService
import com.example.msproject.ui.home.HomeViewModel
import javax.inject.Inject

class HomeViewModelFactory @Inject constructor(private val parkingLotsApiService: ParkingLotsApiService) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(parkingLotsApiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}