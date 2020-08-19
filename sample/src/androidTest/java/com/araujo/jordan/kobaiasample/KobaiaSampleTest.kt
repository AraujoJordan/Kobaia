package com.araujo.jordan.kobaiasample

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.araujo.jordan.kobaia.Kobaia
import com.araujo.jordan.kobaia.Kobaia.Companion.assertTextExist
import com.araujo.jordan.kobaia.Kobaia.Companion.byDescription
import com.araujo.jordan.kobaia.Kobaia.Companion.textClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class KobaiaSampleTest {

    @get:Rule
    val kobaiaRules = Kobaia(SplashActivity::class.java)

    @Test
    fun testApp() {
        kobaiaRules.launchActivity()
        assertTextExist("Kobaia")
        assertTextExist("SKIP")
        assertTextExist("NEXT")
        textClick("SKIP")
        textClick("GET STARTED")
        textClick("LOG IN")
        byDescription("Enter your email")?.text = "right_email@kobaia.com"
        byDescription("Enter your password")?.text = "12345678"
        textClick("ENTER")
        assertTextExist("Welcome to Kobaia!")
    }
}
