package com.araujo.jordan.kobaiasample

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.araujo.jordan.kobaia.Kobaia
import com.araujo.jordan.kobaia.Kobaia.Companion.assertTagChecked
import com.araujo.jordan.kobaia.Kobaia.Companion.assertTagDisabled
import com.araujo.jordan.kobaia.Kobaia.Companion.assertTagUnchecked
import com.araujo.jordan.kobaia.Kobaia.Companion.assertTagVisible
import com.araujo.jordan.kobaia.Kobaia.Companion.assertVisible
import com.araujo.jordan.kobaia.Kobaia.Companion.clearTextInTag
import com.araujo.jordan.kobaia.Kobaia.Companion.clickTag
import com.araujo.jordan.kobaia.Kobaia.Companion.longClickTag
import com.araujo.jordan.kobaia.Kobaia.Companion.scrollToTag
import com.araujo.jordan.kobaia.Kobaia.Companion.typeIntoTag
import com.araujo.jordan.kobaia.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Kobaia against a Jetpack Compose screen.
 * Covers Compose testTags, text finders, long clicks, checkboxes, disabled state, text typing,
 * and lazy list scrolling.
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class ComposeSampleTest {

    @get:Rule
    val kobaia = Kobaia(ComposeSampleActivity::class.java)

    @Test
    fun testApp() = launch<ComposeSampleActivity> {
        assertTagVisible("greeting")
        longClickTag("greeting")
        assertVisible("Kobaia long clicked me!")

        assertTagUnchecked("acceptCheckbox")
        clickTag("acceptCheckbox")
        assertTagChecked("acceptCheckbox")
        assertTagDisabled("disabledButton")

        clickTag("greetButton")
        assertVisible("Kobaia clicked me!")

        typeIntoTag("Kobaia", tag = "nameField")
        assertVisible("You typed: Kobaia")
        clearTextInTag("nameField")

        scrollToTag("item40")
        assertTagVisible("item40")
    }

    /**
     * The very same test, written with the JUnit rule and the infix flavour of the functions
     */
    @Test
    fun testAppWithInfixFunctions() {
        kobaia.launchActivity()
        kobaia assertTagVisible "greeting"
        kobaia longClickTag "greeting"
        kobaia assertVisible "Kobaia long clicked me!"

        kobaia assertTagUnchecked "acceptCheckbox"
        kobaia clickTag "acceptCheckbox"
        kobaia assertTagChecked "acceptCheckbox"
        kobaia assertTagDisabled "disabledButton"

        kobaia clickTag "greetButton"
        kobaia assertVisible "Kobaia clicked me!"

        kobaia type "Kobaia" intoTag "nameField"
        kobaia assertVisible "You typed: Kobaia"
        kobaia clearTextInTag "nameField"

        kobaia scrollToTag "item40"
        kobaia assertTagVisible "item40"
    }
}
