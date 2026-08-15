package com.araujo.jordan.kobaiasample

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.araujo.jordan.kobaia.Kobaia
import com.araujo.jordan.kobaia.Kobaia.Companion.assertTagVisible
import com.araujo.jordan.kobaia.Kobaia.Companion.assertVisible
import com.araujo.jordan.kobaia.Kobaia.Companion.click
import com.araujo.jordan.kobaia.Kobaia.Companion.clickTag
import com.araujo.jordan.kobaia.Kobaia.Companion.scrollToTag
import com.araujo.jordan.kobaia.Kobaia.Companion.typeIntoTag
import com.araujo.jordan.kobaia.launch
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

    @Test
    fun scrollsALazyColumnToATestTag() = launch<ComposeSampleActivity> {
        scrollToTag("item40")
        assertTagVisible("item40")
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
    }
}
