package com.araujo.jordan.kobaia

import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import java.util.regex.Pattern

/**
 * Every Kobaia interaction, as a function you can call plainly or as an infix one — they are the
 * same functions under the same names as the ones on [Kobaia]'s companion object.
 *
 * It is implemented by the [Kobaia] rule, so the rule reads as a sentence:
 *
 * ```kotlin
 * kobaia click "SKIP"
 * ```
 *
 * …and by [KobaiaScope], the receiver of a [launch] block, where there is nothing to import and
 * nothing to name:
 *
 * ```kotlin
 * @Test
 * fun testApp() = launch<SplashActivity> {
 *     click("SKIP")
 * }
 * ```
 *
 * They all use the default wait; call the function on `Kobaia` when you need another one.
 */
interface KobaiaInteractions {

    /**
     * Click every view with this exact text: `kobaia click "SKIP"`
     */
    infix fun click(text: String) = Kobaia.click(text)

    /**
     * Click every view whose text matches this pattern: `kobaia click Pattern.compile("(?i)skip")`
     */
    infix fun click(pattern: Pattern) = Kobaia.click(pattern)

    /**
     * Click every view containing this text: `kobaia clickContaining "Log"`
     */
    infix fun clickContaining(text: String) = Kobaia.clickContaining(text)

    /**
     * Click every view with this content description: `kobaia clickDescription "fluffy"`
     */
    infix fun clickDescription(text: String) = Kobaia.clickDescription(text)

    /**
     * Click every view whose content description matches this pattern
     */
    infix fun clickDescription(pattern: Pattern) = Kobaia.clickDescription(pattern)

    /**
     * Click every view with this Compose testTag: `kobaia clickTag "loginButton"`
     */
    infix fun clickTag(tag: String) = Kobaia.clickTag(tag)

    /**
     * Click every view whose Compose testTag matches this pattern
     */
    infix fun clickTag(pattern: Pattern) = Kobaia.clickTag(pattern)

    /**
     * Long click every view with this exact text: `kobaia longClick "Terms of use"`
     */
    infix fun longClick(text: String) = Kobaia.longClick(text)

    /**
     * Long click every view whose text matches this pattern
     */
    infix fun longClick(pattern: Pattern) = Kobaia.longClick(pattern)

    /**
     * Long click every view with this content description
     */
    infix fun longClickDescription(text: String) = Kobaia.longClickDescription(text)

    /**
     * Long click every view with this Compose testTag
     */
    infix fun longClickTag(tag: String) = Kobaia.longClickTag(tag)

    /**
     * The first view with this exact text: `(kobaia find "Terms of use")?.longClick()`
     */
    infix fun find(text: String): UiObject2? = Kobaia.find(text)

    /**
     * The first view whose text matches this pattern
     */
    infix fun find(pattern: Pattern): UiObject2? = Kobaia.find(pattern)

    /**
     * The first view with this content description: `(kobaia findDescription "total")?.text`
     */
    infix fun findDescription(text: String): UiObject2? = Kobaia.findDescription(text)

    /**
     * The first view whose content description matches this pattern
     */
    infix fun findDescription(pattern: Pattern): UiObject2? = Kobaia.findDescription(pattern)

    /**
     * The first view with this Compose testTag: `(kobaia findTag "total")?.text`
     */
    infix fun findTag(tag: String): UiObject2? = Kobaia.findTag(tag)

    /**
     * The first view whose Compose testTag matches this pattern
     */
    infix fun findTag(pattern: Pattern): UiObject2? = Kobaia.findTag(pattern)

    /**
     * Fail the test unless this text is visible: `kobaia assertVisible "Welcome to Kobaia!"`
     */
    infix fun assertVisible(text: String) = Kobaia.assertVisible(text)

    /**
     * Fail the test unless a text matching this pattern is visible
     */
    infix fun assertVisible(pattern: Pattern) = Kobaia.assertVisible(pattern)

    /**
     * Fail the test unless this Compose testTag is visible: `kobaia assertTagVisible "welcome"`
     */
    infix fun assertTagVisible(tag: String) = Kobaia.assertTagVisible(tag)

    /**
     * Fail the test unless a Compose testTag matching this pattern is visible
     */
    infix fun assertTagVisible(pattern: Pattern) = Kobaia.assertTagVisible(pattern)

    /**
     * Fail the test unless this content description is visible
     */
    infix fun assertDescriptionVisible(text: String) = Kobaia.assertDescriptionVisible(text)

    /**
     * Fail the test if this text is visible: `kobaia assertNotVisible "Wrong credentials!"`
     */
    infix fun assertNotVisible(text: String) = Kobaia.assertNotVisible(text)

    /**
     * Fail the test if a text matching this pattern is visible
     */
    infix fun assertNotVisible(pattern: Pattern) = Kobaia.assertNotVisible(pattern)

    /**
     * Fail the test if this content description is visible
     */
    infix fun assertDescriptionNotVisible(text: String) = Kobaia.assertDescriptionNotVisible(text)

    /**
     * Fail the test if this Compose testTag is visible
     */
    infix fun assertTagNotVisible(tag: String) = Kobaia.assertTagNotVisible(tag)

    /**
     * Fail the test unless the view with this text is on screen and enabled
     */
    infix fun assertEnabled(text: String) = Kobaia.assertEnabled(text)

    /**
     * Fail the test unless the view with this text is on screen and disabled
     */
    infix fun assertDisabled(text: String) = Kobaia.assertDisabled(text)

    /**
     * Fail the test unless the view with this Compose testTag is on screen and enabled
     */
    infix fun assertTagEnabled(tag: String) = Kobaia.assertTagEnabled(tag)

    /**
     * Fail the test unless the view with this Compose testTag is on screen and disabled
     */
    infix fun assertTagDisabled(tag: String) = Kobaia.assertTagDisabled(tag)

    /**
     * Fail the test unless the view with this text is on screen and checked
     */
    infix fun assertChecked(text: String) = Kobaia.assertChecked(text)

    /**
     * Fail the test unless the view with this text is on screen and unchecked
     */
    infix fun assertUnchecked(text: String) = Kobaia.assertUnchecked(text)

    /**
     * Fail the test unless the view with this Compose testTag is on screen and checked
     */
    infix fun assertTagChecked(tag: String) = Kobaia.assertTagChecked(tag)

    /**
     * Fail the test unless the view with this Compose testTag is on screen and unchecked
     */
    infix fun assertTagUnchecked(tag: String) = Kobaia.assertTagUnchecked(tag)

    /**
     * Whether the view with this text is enabled
     */
    infix fun isEnabled(text: String): Boolean = Kobaia.isEnabled(text)

    /**
     * Whether the view with this Compose testTag is enabled
     */
    infix fun isTagEnabled(tag: String): Boolean = Kobaia.isTagEnabled(tag)

    /**
     * Whether the view with this text is checked
     */
    infix fun isChecked(text: String): Boolean = Kobaia.isChecked(text)

    /**
     * Whether the view with this Compose testTag is checked
     */
    infix fun isTagChecked(tag: String): Boolean = Kobaia.isTagChecked(tag)

    /**
     * Whether this text is visible: `if (kobaia isVisible "Rate this app") …`
     */
    infix fun isVisible(text: String): Boolean = Kobaia.isVisible(text)

    /**
     * Whether a text matching this pattern is visible
     */
    infix fun isVisible(pattern: Pattern): Boolean = Kobaia.isVisible(pattern)

    /**
     * Whether this text is part of a text on screen: `kobaia containsText "Welcome"`
     */
    infix fun containsText(text: String): Boolean = Kobaia.containsText(text)

    /**
     * Whether this content description is visible: `kobaia isDescriptionVisible "avatar"`
     */
    infix fun isDescriptionVisible(text: String): Boolean = Kobaia.isDescriptionVisible(text)

    /**
     * Whether a content description matching this pattern is visible
     */
    infix fun isDescriptionVisible(pattern: Pattern): Boolean = Kobaia.isDescriptionVisible(pattern)

    /**
     * Whether this Compose testTag is visible: `kobaia isTagVisible "loginButton"`
     */
    infix fun isTagVisible(tag: String): Boolean = Kobaia.isTagVisible(tag)

    /**
     * Whether a Compose testTag matching this pattern is visible
     */
    infix fun isTagVisible(pattern: Pattern): Boolean = Kobaia.isTagVisible(pattern)

    /**
     * Scroll until this text is visible: `kobaia scrollTo "Delete account"`
     */
    infix fun scrollTo(text: String): UiObject2? = Kobaia.scrollTo(text)

    /**
     * Scroll until a text matching this pattern is visible
     */
    infix fun scrollTo(pattern: Pattern): UiObject2? = Kobaia.scrollTo(pattern)

    /**
     * Scroll until this content description is visible: `kobaia scrollToDescription "footer_logo"`
     */
    infix fun scrollToDescription(text: String): UiObject2? = Kobaia.scrollToDescription(text)

    /**
     * Scroll until a content description matching this pattern is visible
     */
    infix fun scrollToDescription(pattern: Pattern): UiObject2? =
        Kobaia.scrollToDescription(pattern)

    /**
     * Scroll until this Compose testTag is visible: `kobaia scrollToTag "footer"`
     */
    infix fun scrollToTag(tag: String): UiObject2? = Kobaia.scrollToTag(tag)

    /**
     * Scroll until a Compose testTag matching this pattern is visible
     */
    infix fun scrollToTag(pattern: Pattern): UiObject2? = Kobaia.scrollToTag(pattern)

    /**
     * Set a text on a field: `kobaia type "12345678" into "Enter your password"`
     */
    infix fun type(text: String) = PendingText(text, throughKeyboard = false)

    /**
     * Type a text on a field through the soft keyboard, one character at a time:
     * `kobaia typeOnKeyboard "133.37" into "editField"`
     */
    infix fun typeOnKeyboard(text: String) = PendingText(text, throughKeyboard = true)

    /**
     * Empty the field with this content description: `kobaia clearText "Enter your email"`
     */
    infix fun clearText(fieldDescription: String): UiObject2? = Kobaia.clearText(fieldDescription)

    /**
     * Empty the field with this Compose testTag
     */
    infix fun clearTextInTag(tag: String): UiObject2? = Kobaia.clearTextInTag(tag)

    /**
     * Hold the test for this many milliseconds: `kobaia waitFor 2000`
     */
    infix fun waitFor(millis: Long) = Kobaia.waitFor(millis)

    /**
     * The UIAutomator device, for everything these functions do not cover
     */
    fun device(): UiDevice = Kobaia.device()

    /** Press the back button */
    fun pressBack(): Boolean = Kobaia.pressBack()

    /** Press the home button, sending the app under test to the background */
    fun pressHome(): Boolean = Kobaia.pressHome()

    /** Press enter, which submits most forms */
    fun pressEnter(): Boolean = Kobaia.pressEnter()

    /** Open the recent apps switcher */
    fun pressRecentApps(): Boolean = Kobaia.pressRecentApps()

    /** Close the soft keyboard, by pressing back */
    fun closeKeyboard(): Boolean = Kobaia.closeKeyboard()

    /** Pull down the notification shade */
    fun openNotifications(): Boolean = Kobaia.openNotifications()

    /** Grant the runtime permission the system is currently asking for */
    fun allowPermission(): Boolean = Kobaia.allowPermission()

    /** Refuse the runtime permission the system is currently asking for */
    fun denyPermission(): Boolean = Kobaia.denyPermission()

    /** Turn the device to landscape and hold it there */
    fun rotateLandscape() = Kobaia.rotateLandscape()

    /** Turn the device to portrait and hold it there */
    fun rotatePortrait() = Kobaia.rotatePortrait()

    /** Put the device back the way it was, and let it rotate on its own again */
    fun rotateNatural() = Kobaia.rotateNatural()

    /**
     * A text on its way to a field: the first half of `type "…" into "…"`
     */
    class PendingText internal constructor(
        private val text: String,
        private val throughKeyboard: Boolean
    ) {

        /**
         * Deliver the text to the field with this content description
         * @param fieldDescription the description of the field that will receive the text
         */
        infix fun into(fieldDescription: String) {
            if (throughKeyboard) Kobaia.typeOnKeyboard(text, fieldDescription)
            else Kobaia.type(text, fieldDescription)
        }

        /**
         * Deliver the text to the field with this Compose testTag (or View resource id):
         * `kobaia type "12345678" intoTag "passwordField"`
         * @param tag the testTag of the field that will receive the text
         */
        infix fun intoTag(tag: String) {
            if (throughKeyboard) Kobaia.typeOnKeyboardIntoTag(text, tag)
            else Kobaia.typeIntoTag(text, tag)
        }
    }
}
