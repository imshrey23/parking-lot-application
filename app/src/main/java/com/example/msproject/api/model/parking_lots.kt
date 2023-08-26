package com.example.msproject.api.model

data class parking_lots(
    val id : String,
    val latitude: Double,
    val longitude: Double,
    val image_url: String,
    val number_of_empty_parking_slots: Int,
    val parking_charges: String,
    val parking_lot_name: String,
    val parking_lot_time_limit: String,
    val timestamp: String,
    val total_number_of_parking_lots: Int
)
