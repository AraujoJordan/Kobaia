package com.araujo.jordan.kobaiasample

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.araujo.jordan.kobaia.Kobaia
import com.araujo.jordan.kobaia.Kobaia.Companion.assertTextExist
import com.araujo.jordan.kobaia.Kobaia.Companion.descriptionClick
import com.araujo.jordan.kobaia.Kobaia.Companion.scrollUntilFindText
import com.araujo.jordan.kobaia.Kobaia.Companion.slowingTypeNumberInKeyboard
import com.araujo.jordan.kobaia.Kobaia.Companion.textClick
import com.araujo.jordan.kobaia.Kobaia.Companion.uiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Simple Kobaia test example
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class KobaiaInstrumentedTest {

    @get:Rule
    val kobaiaRules = Kobaia(KobaiaTestActivity::class.java)

    @Test(expected = java.lang.AssertionError::class)
    fun shouldFailIfNotFind() {
        kobaiaRules.launchActivity()
        assertTextExist("This text doesn't exist!")
    }

    @Test
    fun testShouldContinueIfExceptionIsContained() {
        kobaiaRules.launchActivity()
        try {
            assertTextExist("This text doesn't exist!")
        } catch (err: java.lang.AssertionError) {
            assertTextExist("CLICK ME!")
        }
    }

    @Test
    fun testApp() {
        kobaiaRules.launchActivity()
        textClick("CLICK ME!")
        descriptionClick("fluffy")
        textClick("YOU CAN CLICK ME!", 15000)
        slowingTypeNumberInKeyboard("editField", "133.37")
        uiDevice()?.pressBack()
        uiDevice()?.pressHome()
        textClick("Kobaia")
        scrollUntilFindText("SCROLL TO CLICK ME!")
        assertTextExist("SCROLL TO CLICK ME!")
    }
}
