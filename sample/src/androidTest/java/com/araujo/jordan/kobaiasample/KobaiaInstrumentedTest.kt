package com.araujo.jordan.kobaiasample

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.araujo.jordan.kobaia.Kobaia
import com.araujo.jordan.kobaia.Kobaia.Companion.assertVisible
import com.araujo.jordan.kobaia.Kobaia.Companion.click
import com.araujo.jordan.kobaia.Kobaia.Companion.clickDescription
import com.araujo.jordan.kobaia.Kobaia.Companion.device
import com.araujo.jordan.kobaia.Kobaia.Companion.scrollTo
import com.araujo.jordan.kobaia.Kobaia.Companion.typeOnKeyboard
import com.araujo.jordan.kobaia.Kobaia.Companion.waitUntilGone
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

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
        assertVisible("This text doesn't exist!", 1000)
    }

    @Test
    fun testShouldContinueIfExceptionIsContained() {
        kobaia.launchActivity()
        try {
            assertVisible("This text doesn't exist!", 1000)
        } catch (err: java.lang.AssertionError) {
            assertVisible("CLICK ME!")
        }
    }

    /**
     * The button counts itself down — 5, 4, 3 — before it turns into its real label. Waiting for
     * the digits to leave is waiting for the countdown itself, rather than guessing a number and
     * sleeping through it.
     */
    @Test
    fun waitsForTheCountdownToFinish() {
        kobaia.launchActivity()
        assertTrue(
            "the countdown should finish",
            waitUntilGone(Pattern.compile("\\d+"), 15000)
        )
        assertVisible("YOU CAN CLICK ME!")
    }

    @Test
    fun testApp() {
        kobaia.launchActivity()
        click("CLICK ME!")
        clickDescription("fluffy")
        click("YOU CAN CLICK ME!", 15000)
        typeOnKeyboard("133.37", into = "editField")
        device().pressBack()
        scrollTo("SCROLL TO CLICK ME!")
        assertVisible("SCROLL TO CLICK ME!")
    }

    /**
     * Scrolling for a view the list does not have stops at the end of the list rather than
     * spending the whole swipe budget — the miss costs the handful of swipes the list is tall,
     * not the twenty it was allowed.
     */
    @Test
    fun scrollMissStopsAtTheEndOfTheList() {
        kobaia.launchActivity()
        assertNull(scrollTo("This row is not in the list!"))
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
        kobaia scrollTo "SCROLL TO CLICK ME!"
        kobaia assertVisible "SCROLL TO CLICK ME!"
    }

    /**
     * The checkbox is set to a known state rather than toggled: checking an already checked box
     * must leave it checked, which is what makes a retried test safe to repeat.
     */
    @Test
    fun checksAndUnchecksTheTerms() {
        kobaia.launchActivity()
        assertTrue(kobaia check "Accept terms")
        kobaia assertChecked "Accept terms"
        // already checked, by description this time: nothing changes, and that is the point
        assertTrue(kobaia checkDescription "termsCheckbox")
        kobaia assertChecked "Accept terms"
        assertTrue(kobaia uncheck "Accept terms")
        kobaia assertUnchecked "Accept terms"
    }

    /**
     * Clickability is asserted the same way the other state is, and a double click is the click
     * family with one more tap
     */
    @Test
    fun assertsClickableAndDoubleClicks() {
        kobaia.launchActivity()
        kobaia assertClickable "CLICK ME!"
        assertTrue(kobaia doubleClick "CLICK ME!")
    }
}
