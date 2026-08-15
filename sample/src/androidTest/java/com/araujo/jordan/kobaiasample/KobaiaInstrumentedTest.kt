package com.araujo.jordan.kobaiasample

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.araujo.jordan.kobaia.Kobaia
import com.araujo.jordan.kobaia.Kobaia.Companion.assertVisible
import com.araujo.jordan.kobaia.Kobaia.Companion.click
import com.araujo.jordan.kobaia.Kobaia.Companion.clickDescription
import com.araujo.jordan.kobaia.Kobaia.Companion.device
import com.araujo.jordan.kobaia.Kobaia.Companion.scrollTo
import com.araujo.jordan.kobaia.Kobaia.Companion.typeOnKeyboard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Simple Kobaia test example
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class KobaiaInstrumentedTest {

    @get:Rule
    val kobaia = Kobaia(KobaiaTestActivity::class.java)

    @Test(expected = java.lang.AssertionError::class)
    fun shouldFailIfNotFind() {
        kobaia.launchActivity()
        assertVisible("This text doesn't exist!")
    }

    @Test
    fun testShouldContinueIfExceptionIsContained() {
        kobaia.launchActivity()
        try {
            assertVisible("This text doesn't exist!")
        } catch (err: java.lang.AssertionError) {
            assertVisible("CLICK ME!")
        }
    }

    @Test
    fun testApp() {
        kobaia.launchActivity()
        click("CLICK ME!")
        clickDescription("fluffy")
        click("YOU CAN CLICK ME!", 15000)
        typeOnKeyboard("133.37", into = "editField")
        device().pressBack()
        device().pressHome()
        click("Kobaia")
        scrollTo("SCROLL TO CLICK ME!")
        assertVisible("SCROLL TO CLICK ME!")
    }

    /**
     * The very same test, written with the infix flavour of the very same functions
     */
    @Test
    fun testAppWithInfixFunctions() {
        kobaia.launchActivity()
        kobaia click "CLICK ME!"
        kobaia clickDescription "fluffy"
        // the infix functions always use the default wait, so a custom one asks for the plain call
        click("YOU CAN CLICK ME!", 15000)
        kobaia typeOnKeyboard "133.37" into "editField"
        device().pressBack()
        device().pressHome()
        kobaia click "Kobaia"
        kobaia scrollTo "SCROLL TO CLICK ME!"
        kobaia assertVisible "SCROLL TO CLICK ME!"
    }
}
