package com.araujo.jordan.kobaiasample

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.araujo.jordan.kobaia.launch
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class KobaiaSampleTest {

    @Test
    fun testApp() = launch<SplashActivity> {
        assertVisible("Kobaia")
        assertVisible("SKIP")
        assertVisible("NEXT")
        click("SKIP")
        click("GET STARTED")
        click("LOG IN")
        type("right_email@kobaia.com") into "Enter your email"
        type("12345678") into "Enter your password"
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
