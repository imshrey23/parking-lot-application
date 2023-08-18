package com.example.msproject.com.example.msproject.api.ApiService

object ApiConstant {
    const val PARKING_LOTS_API =
        "https://658ytxfrod.execute-api.us-east-1.amazonaws.com/dev/parking_lots"
    const val PARKING_LOT_API = "http://10.0.2.2:8000/parkingLots/:parkingLotName"
//    const val DELETE_OLD_RECORD = "http://10.0.2.2:8000/reservedParkingSpot"
    const val UPDATE_PARKING_LOT = "http://10.0.2.2:8000/reserveParkingSpot"
    const val DISTANCE_MATRIX = "https://maps.googleapis.com/maps/api/distancematrix/json?"
}