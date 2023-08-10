package com.example.msproject

import android.content.ContentResolver
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.provider.Settings
import com.example.msproject.common.CommonUtils
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

//TODO: Update and add unit tests
@RunWith(MockitoJUnitRunner::class)
class CommonUtilsTest {

    @Test
    fun testIsMapsInstalled() {
        val packageManager = mock(PackageManager::class.java)
        val packageName = "com.google.android.apps.maps"

        `when`(packageManager.getPackageInfo(packageName, 0)).thenReturn(mock(PackageInfo::class.java))
        Assert.assertTrue(CommonUtils.isMapsInstalled(packageManager) == true)

        `when`(packageManager.getPackageInfo(packageName, 0)).thenThrow(PackageManager.NameNotFoundException())
        Assert.assertTrue(CommonUtils.isMapsInstalled(packageManager) == false) // Corrected to assertTrue
    }

    @Test
    fun testGetDeviceId() {
        val contentResolver = mock(ContentResolver::class.java)
        val androidId = "test_android_id"

        mockStatic(Settings.Secure::class.java).use { mock ->
            `when`(Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)).thenReturn(androidId)

            assertEquals(androidId, CommonUtils.getDeviceId(contentResolver))

            mock.verify { Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) }
        }
    }




    @Test
    fun testGetDrivingDurationFromDistanceMatrixApiResponse() {

        val response = """
            {
                "rows": [
                    {
                        "elements": [
                            {
                                "duration": { "value": 300 }
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        CommonUtils.getDrivingDurationFromDistanceMatrixApiResponse(response)
            ?.let { assertEquals(300.0, it, 0.0) }

        val invalidResponse = """ { "rows": [] } """
        Assert.assertNull(
            CommonUtils.getDrivingDurationFromDistanceMatrixApiResponse(
                invalidResponse
            )
        )
    }
}

