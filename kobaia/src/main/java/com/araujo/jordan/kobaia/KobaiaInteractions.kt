package com.araujo.jordan.kobaia

import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import java.io.File
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
     * Wait for a text that is on screen now to go away, and report whether it did:
     * `kobaia waitUntilGone "Loading…"`
     */
    infix fun waitUntilGone(text: String): Boolean = Kobaia.waitUntilGone(text)

    /**
     * Wait for a text matching this pattern to go away
     */
    infix fun waitUntilGone(pattern: Pattern): Boolean = Kobaia.waitUntilGone(pattern)

    /**
     * Wait for this content description to go away
     */
    infix fun waitUntilDescriptionGone(text: String): Boolean =
        Kobaia.waitUntilDescriptionGone(text)

    /**
     * Wait for this Compose testTag to go away: `kobaia waitUntilTagGone "loadingSpinner"`
     */
    infix fun waitUntilTagGone(tag: String): Boolean = Kobaia.waitUntilTagGone(tag)

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
     * Type a text on a field one key press at a time:
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
     * Wait for the screen to stop changing, and no longer. Returns whether it settled before the
     * default wait ran out: `waitForStable()`
     */
    fun waitForStable(): Boolean = Kobaia.waitForStable()

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
     * Fail the test unless the view with this text is on screen and clickable
     */
    infix fun assertClickable(text: String) = Kobaia.assertClickable(text)

    /**
     * Fail the test unless the view with this text is on screen and not clickable
     */
    infix fun assertNotClickable(text: String) = Kobaia.assertNotClickable(text)

    /**
     * Fail the test unless the view with this Compose testTag is on screen and clickable
     */
    infix fun assertTagClickable(tag: String) = Kobaia.assertTagClickable(tag)

    /**
     * Fail the test unless the view with this Compose testTag is on screen and not clickable
     */
    infix fun assertTagNotClickable(tag: String) = Kobaia.assertTagNotClickable(tag)

    /**
     * Check the view with this text, whichever state it is in now: `kobaia check "Accept terms"`
     */
    infix fun check(text: String): Boolean = Kobaia.check(text)

    /**
     * Check the view with this content description, whichever state it is in now
     */
    infix fun checkDescription(text: String): Boolean = Kobaia.checkDescription(text)

    /**
     * Check the view with this Compose testTag, whichever state it is in now
     */
    infix fun checkTag(tag: String): Boolean = Kobaia.checkTag(tag)

    /**
     * Uncheck the view with this text, whichever state it is in now
     */
    infix fun uncheck(text: String): Boolean = Kobaia.uncheck(text)

    /**
     * Uncheck the view with this content description, whichever state it is in now
     */
    infix fun uncheckDescription(text: String): Boolean = Kobaia.uncheckDescription(text)

    /**
     * Uncheck the view with this Compose testTag, whichever state it is in now
     */
    infix fun uncheckTag(tag: String): Boolean = Kobaia.uncheckTag(tag)

    /**
     * Swipe across the middle of the screen: `kobaia swipe Direction.UP`
     */
    infix fun swipe(direction: Direction): Boolean = Kobaia.swipe(direction)

    /**
     * Double click every view with this exact text: `kobaia doubleClick "Like"`
     */
    infix fun doubleClick(text: String): Boolean = Kobaia.doubleClick(text)

    /**
     * Double click every view whose text matches this pattern
     */
    infix fun doubleClick(pattern: Pattern): Boolean = Kobaia.doubleClick(pattern)

    /**
     * Double click every view with this content description
     */
    infix fun doubleClickDescription(text: String): Boolean = Kobaia.doubleClickDescription(text)

    /**
     * Double click every view with this Compose testTag
     */
    infix fun doubleClickTag(tag: String): Boolean = Kobaia.doubleClickTag(tag)

    /**
     * Pinch the first view with this text open, as when zooming in: `kobaia pinchOut "Map"`
     */
    infix fun pinchOut(text: String): Boolean = Kobaia.pinchOut(text)

    /**
     * Pinch the first view with this content description open
     */
    infix fun pinchOutDescription(text: String): Boolean = Kobaia.pinchOutDescription(text)

    /**
     * Pinch the first view with this Compose testTag open
     */
    infix fun pinchOutTag(tag: String): Boolean = Kobaia.pinchOutTag(tag)

    /**
     * Pinch the first view with this text closed, as when zooming out
     */
    infix fun pinchIn(text: String): Boolean = Kobaia.pinchIn(text)

    /**
     * Pinch the first view with this content description closed
     */
    infix fun pinchInDescription(text: String): Boolean = Kobaia.pinchInDescription(text)

    /**
     * Pinch the first view with this Compose testTag closed
     */
    infix fun pinchInTag(tag: String): Boolean = Kobaia.pinchInTag(tag)

    /**
     * Press any key, by its KeyEvent keycode: `kobaia pressKey KeyEvent.KEYCODE_DEL`
     */
    infix fun pressKey(keyCode: Int): Boolean = Kobaia.pressKey(keyCode)

    /**
     * Photograph the screen into this file: `kobaia screenshot file`
     */
    infix fun screenshot(file: File): Boolean = Kobaia.screenshot(file)

    /**
     * Copy this text onto the device clipboard: `kobaia copyToClipboard "user@kobaia.com"`
     */
    infix fun copyToClipboard(text: String) = Kobaia.copyToClipboard(text)

    /** The text currently on the device clipboard, or null when it is empty */
    fun clipboardText(): String? = Kobaia.clipboardText()

    /** Wake the device up, as pressing the power button would */
    fun wakeUp() = Kobaia.wakeUp()

    // -------------------------------------------------------------------------------------------
    // Scrolling the other way
    // -------------------------------------------------------------------------------------------

    /**
     * Scroll a given way until this text turns up: `scrollTo("CLICK ME!", Direction.UP)`.
     *
     * The infix `scrollTo` only ever searches forward, which cannot reach something the list has
     * already gone past.
     */
    fun scrollTo(text: String, direction: Direction): UiObject2? =
        Kobaia.scrollTo(text, direction = direction)

    /** Scroll a given way until this content description turns up. @see scrollTo */
    fun scrollToDescription(text: String, direction: Direction): UiObject2? =
        Kobaia.scrollToDescription(text, direction = direction)

    /** Scroll a given way until this Compose testTag turns up. @see scrollTo */
    fun scrollToTag(tag: String, direction: Direction): UiObject2? =
        Kobaia.scrollToTag(tag, direction = direction)

    // -------------------------------------------------------------------------------------------
    // Scoping
    // -------------------------------------------------------------------------------------------

    /**
     * Narrow every search inside the block to the descendants of the view with this testTag:
     *
     * ```kotlin
     * withinTag("item3") { click("OPEN") }
     * ```
     * @see Kobaia.withinTag
     */
    fun <R> withinTag(tag: String, block: KobaiaInteractions.() -> R): R =
        Kobaia.withinTag(tag) { block() }

    /**
     * Narrow every search inside the block to the descendants of the view with this text
     * @see Kobaia.withinTag
     */
    fun <R> within(text: String, block: KobaiaInteractions.() -> R): R =
        Kobaia.within(text) { block() }

    /**
     * Narrow every search inside the block to the descendants of the view with this description
     * @see Kobaia.withinTag
     */
    fun <R> withinDescription(text: String, block: KobaiaInteractions.() -> R): R =
        Kobaia.withinDescription(text) { block() }

    /**
     * Narrow every search inside the block to the descendants of whatever the selector matches
     * @see Kobaia.withinTag
     */
    fun <R> within(container: BySelector, block: KobaiaInteractions.() -> R): R =
        Kobaia.within(container) { block() }

    // -------------------------------------------------------------------------------------------
    // Selectors you built yourself
    // -------------------------------------------------------------------------------------------

    /** The first view matching a selector of your own: `kobaia find By.clazz(SeekBar::class.java)` */
    infix fun find(selector: BySelector): UiObject2? = Kobaia.find(selector)

    /** Click every view matching a selector of your own */
    infix fun click(selector: BySelector): Boolean = Kobaia.click(selector)

    /** Whether anything matching a selector of your own is on screen */
    infix fun isVisible(selector: BySelector): Boolean = Kobaia.isVisible(selector)

    /** Assert that something matching a selector of your own is on screen */
    infix fun assertVisible(selector: BySelector) = Kobaia.assertVisible(selector)

    /** Every view matching a selector of your own */
    infix fun findAll(selector: BySelector): List<UiObject2> = Kobaia.findAll(selector)

    /** Every view with this exact text */
    infix fun findAll(text: String): List<UiObject2> = Kobaia.findAll(text)

    /** Every view with this Compose testTag (or View resource id) */
    infix fun findAllTags(tag: String): List<UiObject2> = Kobaia.findAllTags(tag)

    // -------------------------------------------------------------------------------------------
    // Counting
    // -------------------------------------------------------------------------------------------

    /** How many views on screen have this text: `kobaia countOf "OPEN"` */
    infix fun countOf(text: String): Int = Kobaia.countOf(text)

    /** How many views on screen have this content description */
    infix fun countOfDescription(text: String): Int = Kobaia.countOfDescription(text)

    /** How many views on screen have this Compose testTag (or View resource id) */
    infix fun countOfTag(tag: String): Int = Kobaia.countOfTag(tag)

    /** How many views on screen match a selector of your own */
    infix fun countOf(selector: BySelector): Int = Kobaia.countOf(selector)

    /** Assert that exactly [expected] views on screen have this text */
    fun assertCount(text: String, expected: Int) = Kobaia.assertCount(text, expected)

    /** Assert that exactly [expected] views on screen have this Compose testTag */
    fun assertTagCount(tag: String, expected: Int) = Kobaia.assertTagCount(tag, expected)

    // -------------------------------------------------------------------------------------------
    // Reading text
    // -------------------------------------------------------------------------------------------

    /** What the view with this Compose testTag says: `kobaia textOfTag "totalLabel"` */
    infix fun textOfTag(tag: String): String? = Kobaia.textOfTag(tag)

    /** What the view with this content description says */
    infix fun textOfDescription(text: String): String? = Kobaia.textOfDescription(text)

    /** What the first view matching a selector of your own says */
    infix fun textOf(selector: BySelector): String? = Kobaia.textOf(selector)

    /** Assert that the view with this Compose testTag says exactly this */
    fun assertTagTextEquals(tag: String, expected: String) =
        Kobaia.assertTagTextEquals(tag, expected)

    /** Assert that the view with this Compose testTag says something containing this */
    fun assertTagTextContains(tag: String, expected: String) =
        Kobaia.assertTagTextContains(tag, expected)

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
