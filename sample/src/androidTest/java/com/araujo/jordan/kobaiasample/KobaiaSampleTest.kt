package com.araujo.jordan.kobaiasample

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.araujo.jordan.kobaia.launch
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The sample's login flow, driven end to end.
 *
 * Those screens are Jetpack Compose, and nothing in this test says so: the same functions found
 * them when they were XML layouts.
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class KobaiaSampleTest {

    /**
     * Every screen is asserted before it is acted on. Clicks and typing report a miss quietly by
     * design, so a flow that only checks its last screen cannot say which of seven steps went
     * wrong — it can only say that the end never arrived.
     */
    @Test
    fun testApp() = launch<SplashActivity> {
        assertVisible("Kobaia")
        assertVisible("SKIP")
        assertVisible("NEXT")
        click("SKIP")

        assertVisible("GET STARTED")
        click("GET STARTED")

        assertVisible("LOG IN")
        click("LOG IN")

        assertVisible("Enter your email")
        type("right_email@kobaia.com") into "Enter your email"
        type("12345678") into "Enter your password"
        // The credentials have to arrive exactly, or the screen answers with a toast instead
        assertVisible("right_email@kobaia.com")
        assertVisible("12345678")

        click("ENTER")
        assertVisible("Welcome to Kobaia!")
    }

    /**
     * The very same test, launched from the activity class instead of a type argument
     */
    @Test
    fun testAppFromTheActivityClass() = SplashActivity::class.launch {
        assertVisible("Kobaia")
        click("SKIP")
        click("GET STARTED")
        click("LOG IN")
        type("right_email@kobaia.com") into "Enter your email"
        type("12345678") into "Enter your password"
        click("ENTER")
        assertVisible("Welcome to Kobaia!")
    }
}
