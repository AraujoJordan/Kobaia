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

    @get:Rule
    val kobaia = Kobaia(SplashActivity::class.java)

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

    /**
     * The very same test, written with the JUnit rule and the infix flavour of the functions
     */
    @Test
    fun testAppWithInfixFunctions() {
        kobaia.launchActivity()
        kobaia assertTagVisible "splashTitle"
        kobaia assertVisible "Kobaia"
        kobaia clickTag "welcomeSkipButton"

        kobaia assertTagVisible "welcomeGetStartedButton"
        kobaia clickTag "welcomeGetStartedButton"

        kobaia assertTagVisible "landingLoginButton"
        kobaia clickTag "landingLoginButton"

        kobaia assertTagVisible "loginTitle"
        kobaia type "right_email@kobaia.com" intoTag "emailField"
        kobaia type "12345678" intoTag "passwordField"

        kobaia clickTag "loginButton"
        kobaia assertTagVisible "loggedInText"
        kobaia assertVisible "Welcome to Kobaia!"
    }
}
