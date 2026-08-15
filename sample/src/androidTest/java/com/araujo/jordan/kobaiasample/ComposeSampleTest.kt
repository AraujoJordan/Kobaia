package com.araujo.jordan.kobaiasample

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.araujo.jordan.kobaia.Kobaia
import com.araujo.jordan.kobaia.Kobaia.Companion.QUICK_WAITING_TIME
import com.araujo.jordan.kobaia.Kobaia.Companion.assertTagVisible
import com.araujo.jordan.kobaia.Kobaia.Companion.assertVisible
import com.araujo.jordan.kobaia.Kobaia.Companion.click
import com.araujo.jordan.kobaia.Kobaia.Companion.clickTag
import com.araujo.jordan.kobaia.Kobaia.Companion.assertNotVisible
import com.araujo.jordan.kobaia.Kobaia.Companion.assertTagChecked
import com.araujo.jordan.kobaia.Kobaia.Companion.assertTagDisabled
import com.araujo.jordan.kobaia.Kobaia.Companion.assertTagUnchecked
import com.araujo.jordan.kobaia.Kobaia.Companion.clearTextInTag
import com.araujo.jordan.kobaia.Kobaia.Companion.longClickTag
import com.araujo.jordan.kobaia.Kobaia.Companion.scrollToTag
import com.araujo.jordan.kobaia.Kobaia.Companion.typeIntoTag
import com.araujo.jordan.kobaia.Kobaia.Companion.typeOnKeyboardIntoTag
import com.araujo.jordan.kobaia.launch
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Kobaia against a Jetpack Compose screen. Nothing here is Compose-specific except the testTags:
 * the text of a `Text` is found by the same function that finds the text of a `TextView`.
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class ComposeSampleTest {

    @get:Rule
    val kobaia = Kobaia(ComposeSampleActivity::class.java)

    @Test
    fun findsComposablesByTheirText() = launch<ComposeSampleActivity> {
        assertVisible("Tap the button")
        click("CLICK ME!")
        assertVisible("Kobaia clicked me!")
    }

    @Test
    fun findsComposablesByTheirTestTag() = launch<ComposeSampleActivity> {
        assertTagVisible("greeting")
        clickTag("greetButton")
        assertVisible("Kobaia clicked me!")
    }

    @Test
    fun typesIntoATaggedTextField() = launch<ComposeSampleActivity> {
        typeIntoTag("Kobaia", tag = "nameField")
        assertVisible("You typed: Kobaia")
    }

    /**
     * The other typing flavour: one key press per character, the way a `TextWatcher` sees a
     * person type. The capital and the punctuation are the point — they live on keyboard pages
     * that are not showing, so a `TextField` that receives them proves the keys are sent rather
     * than tapped on screen.
     */
    @Test
    fun typesOnTheKeyboardIntoATaggedTextField() = launch<ComposeSampleActivity> {
        typeOnKeyboardIntoTag("Kobaia 2.0!", tag = "nameField")
        assertVisible("You typed: Kobaia 2.0!")
    }

    @Test
    fun scrollsALazyColumnToATestTag() = launch<ComposeSampleActivity> {
        scrollToTag("item40")
        assertTagVisible("item40")
    }

    /**
     * A Compose screen recomposing is exactly the case an idle-based wait cannot survive, so the
     * settling is measured on the accessibility tree and reported rather than thrown.
     */
    @Test
    fun waitsForTheScreenToSettle() = launch<ComposeSampleActivity> {
        clickTag("greetButton")
        assertTrue("the screen should settle after a click", waitForStable())
        assertVisible("Kobaia clicked me!")
    }

    @Test
    fun longClicksATaggedComposable() = launch<ComposeSampleActivity> {
        longClickTag("greeting")
        assertVisible("Kobaia long clicked me!")
    }

    @Test
    fun readsTheStateOfATaggedComposable() = launch<ComposeSampleActivity> {
        assertTagUnchecked("acceptCheckbox")
        clickTag("acceptCheckbox")
        assertTagChecked("acceptCheckbox")
        assertTagDisabled("disabledButton")
    }

    /**
     * Clearing the field is one action; the screen recomposing to show it is the next frame.
     * `assertNotVisible` answers in 50 ms by design, which is quick enough to catch the text on
     * its way out — waiting for it to go is the check that means what this test means.
     */
    @Test
    fun clearsATaggedTextField() = launch<ComposeSampleActivity> {
        typeIntoTag("Kobaia", tag = "nameField")
        assertVisible("You typed: Kobaia")
        clearTextInTag("nameField")
        assertTrue("the typed text should go", waitUntilGone("You typed: Kobaia"))
    }

    /**
     * The short waits are the point. A rotation destroys and recreates the activity, and Kobaia
     * does not return from one until the screen has settled — so the recreated composable is
     * already there, without a finder having to poll for it.
     */
    @Test
    fun survivesARotation() = launch<ComposeSampleActivity> {
        rotateLandscape()
        assertTagVisible("greeting", QUICK_WAITING_TIME)
        rotateNatural()
        assertTagVisible("greeting", QUICK_WAITING_TIME)
    }

    /**
     * The same screen through the rule, in the infix flavour
     */
    @Test
    fun drivesComposeWithTheInfixFunctions() {
        kobaia.launchActivity()
        kobaia assertTagVisible "greeting"
        kobaia clickTag "greetButton"
        kobaia assertVisible "Kobaia clicked me!"
        kobaia type "Kobaia" intoTag "nameField"
        kobaia assertVisible "You typed: Kobaia"
        kobaia scrollToTag "item40"
        kobaia longClickTag "greeting"
        kobaia assertVisible "Kobaia long clicked me!"
        kobaia assertNotVisible "This text is nowhere on the screen"
    }
}
