package com.araujo.jordan.kobaiasample

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.araujo.jordan.kobaia.Kobaia
import com.araujo.jordan.kobaia.Kobaia.Companion.assertTagVisible
import com.araujo.jordan.kobaia.Kobaia.Companion.assertVisible
import com.araujo.jordan.kobaia.Kobaia.Companion.clickTag
import com.araujo.jordan.kobaia.Kobaia.Companion.typeIntoTag
import com.araujo.jordan.kobaia.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The sample's Compose login flow, driven end to end by Compose testTags.
 * Demonstrates finders, assertions, clicks, and typing targeting Modifier.testTag selectors.
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class ComposeSampleTest {

    @Test
    fun testApp() = launch<SplashActivity> {
        assertTagVisible("splashTitle")
        assertVisible("Kobaia")
        clickTag("welcomeSkipButton")

        assertTagVisible("welcomeGetStartedButton")
        clickTag("welcomeGetStartedButton")

        assertTagVisible("landingLoginButton")
        clickTag("landingLoginButton")

        assertTagVisible("loginTitle")
        typeIntoTag("right_email@kobaia.com", tag = "emailField")
        typeIntoTag("12345678", tag = "passwordField")

        clickTag("loginButton")
        assertTagVisible("loggedInText")
        assertVisible("Welcome to Kobaia!")
    }
}
