package com.example.msproject


import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasPackage
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.libraries.places.widget.AutocompleteActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.rule.GrantPermissionRule


@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Before
    fun setUp() {
        // Initialize ActivityScenario
        ActivityScenario.launch(MainActivity::class.java)
    }


    @get:Rule var permissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)
    @Test
    fun testMapFragmentLaunch(){

        onView(withId(R.id.map_fragment)).check(matches(isDisplayed()))
    }

    @Test
    fun testMoreInfoButton() {
        // Click on the more info button
        onView(withId(R.id.fabMoreInfo)).perform(click())

        // Verify that MoreInfoActivity is launched
        onView(withId(R.id.more_info_layout)).check(matches(isDisplayed()))

        pressBack()

        onView(withId(R.id.main)).check(matches(isDisplayed()))

    }

    @Test
    fun testNavigateButton() {
        // Initialize Intents
        Intents.init()

        Thread.sleep(2000)

        // Click on the navigate button
        onView(withId(R.id.fabNavigation)).perform(click())

        // Verify that Google Maps is launched
        intended(hasPackage("com.google.android.apps.maps"))

        // Release Intents
        Intents.release()

    }

    @Test
    fun testSearchBar() {
        // Initialize Intents
        Intents.init()

        // Click on the search bar
        onView(withId(R.id.search_bar)).perform(click())

        // Verify that Autocomplete intent is launched
        intended(hasComponent(AutocompleteActivity::class.java.name))

        // Release Intents
        Intents.release()

    }






}

