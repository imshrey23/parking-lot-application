package com.example.msproject.common

import android.content.ContentResolver
import android.content.pm.PackageManager
import android.provider.Settings
import com.example.msproject.model.distance.DistanceMatrixResponse
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.cloud.translate.Translation
import com.google.gson.Gson

object CommonUtils {

    fun isMapsInstalled(packageManager: PackageManager): Boolean? {
        val mapsPackage = "com.google.android.apps.maps"
        val isMapsInstalled = try {
            packageManager.getPackageInfo(mapsPackage, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        return isMapsInstalled
    }

    fun getDeviceId(contentResolver: ContentResolver): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun translate(text: String, targetLanguage: String): String {
        // Set up the translation service
        val translate = TranslateOptions.newBuilder()
            .setApiKey("AIzaSyCvSl5ugDPB8g_NPPEtK2NwMqB6D0zzF0Y")
            .build()
            .service

        // Perform the translation
        val translation: Translation = translate.translate(
            text,
            Translate.TranslateOption.targetLanguage(targetLanguage)
        )

        // Return the translated text
        return translation.translatedText
    }

    // Used by findNearestParkingLot
    fun getDrivingDurationFromDistanceMatrixApiResponse(response: String): Double? {
        val gson = Gson()
        val distanceMatrixResponse = gson.fromJson(response, DistanceMatrixResponse::class.java)
        return if (distanceMatrixResponse.rows.isNotEmpty() &&
            distanceMatrixResponse.rows[0].elements.isNotEmpty() &&
            distanceMatrixResponse.rows[0].elements[0].duration != null
        ) {
            distanceMatrixResponse.rows[0].elements[0].duration?.value?.toDouble()
        } else {
            null
        }
    }
}