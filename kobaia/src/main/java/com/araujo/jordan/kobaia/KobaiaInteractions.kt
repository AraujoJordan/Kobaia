package com.araujo.jordan.kobaia

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
     * Hold the test for this many milliseconds: `kobaia waitFor 2000`
     */
    infix fun waitFor(millis: Long) = Kobaia.waitFor(millis)

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
