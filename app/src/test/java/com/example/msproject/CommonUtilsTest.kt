package com.example.msproject


import android.content.ContentResolver
import android.content.pm.PackageManager
import com.example.msproject.common.CommonUtils
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import android.provider.Settings
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.cloud.translate.Translation
import io.mockk.*


@RunWith(JUnit4::class)
class CommonUtilsTest {

    @Test
    fun testIsMapsInstalled() {
        val packageManager = mockk<PackageManager>()
        val packageName = "com.google.android.apps.maps"

        every {
            packageManager.getPackageInfo(packageName, 0)
        } returns mockk()

        assertTrue(CommonUtils.isMapsInstalled(packageManager))

        every {
            packageManager.getPackageInfo(packageName, 0)
        } throws PackageManager.NameNotFoundException()

        assertTrue(!CommonUtils.isMapsInstalled(packageManager))
    }

    @Test
    fun testGetDeviceId() {
        val contentResolver = mockk<ContentResolver>()
        val androidId = "test_android_id"

        // Mock the Settings.Secure static method
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) } returns androidId

        assertTrue(androidId == CommonUtils.getDeviceId(contentResolver))

        verify { Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) }
    }

    @Test
    fun testTranslate() {
        val text = "Hello"
        val targetLanguage = "es"
        val translatedText = "Hola"

        val translation = mockk<Translation>()
        val translate = mockk<Translate>()
        val translateOptionsBuilder = mockk<TranslateOptions.Builder>()
        val translateOptions = mockk<TranslateOptions>()

        every {
            translate.translate(text, Translate.TranslateOption.targetLanguage(targetLanguage))
        } returns translation
        every { translation.translatedText } returns translatedText

        every { translateOptionsBuilder.setApiKey(BuildConfig.PLACES_API_KEY) } returns translateOptionsBuilder
        every { translateOptionsBuilder.build() } returns translateOptions
        every { translateOptions.service } returns translate

        // Mock the static method
        mockkStatic(TranslateOptions::newBuilder)
        every { TranslateOptions.newBuilder() } answers { translateOptionsBuilder }

        assertTrue(translatedText == CommonUtils.translate(text, targetLanguage))
    }

}
