package com.araujo.jordan.kobaia

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.KeyCharacterMap
import androidx.test.espresso.IdlingPolicies
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.waitForStableInActiveWindow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * A test lib built on top of UIAutomator to provide a simple and discoverable API that removes
 * the boilerplate and verbosity of common UI test tasks.
 *
 * This is the JUnit `TestRule` flavour. When you do not need the activity to outlive a single
 * test method, [launch] does the same without a rule to declare:
 *
 * ```kotlin
 * @Test
 * fun testApp() = launch<SplashActivity> { click("SKIP") }
 * ```
 *
 * Every interaction has the same name whichever way you call it:
 *
 * ```kotlin
 * click("SKIP")         // imported from Kobaia.Companion, or inside a launch block
 * kobaia click "SKIP"   // infix, through the rule you declared
 * ```
 *
 * @param activityClass the activity under test
 * @param flakyAttempts how many times a failing test is retried before it is reported as failed
 * @param launchActivityAutomatically launch the activity before the test body runs, instead of
 * waiting for a [launchActivity] call
 */
class Kobaia<T : Activity>(
    activityClass: Class<T>,
    flakyAttempts: Int = DEFAULT_FLAKY_ATTEMPTS,
    launchActivityAutomatically: Boolean = false
) : TestRule, KobaiaInteractions {

    companion object {

        /**
         * How long the finders keep polling the screen before giving up, in milliseconds
         */
        const val DEFAULT_WAITING_TIME = 5000L

        /**
         * How many times a failing test is retried before it is reported as failed
         */
        const val DEFAULT_FLAKY_ATTEMPTS = 5

        /**
         * How many times the scrolling functions swipe before giving up
         */
        const val DEFAULT_MAXIMUM_SCROLLS = 20

        /**
         * How long Espresso may wait for the app under test to go idle, in milliseconds.
         *
         * This is Espresso's own default, and it only applies if you mix Espresso interactions
         * into a Kobaia test — Kobaia itself does not wait on idleness, which is what lets it
         * drive screens that never go idle, such as a Compose screen with a running animation.
         */
        const val DEFAULT_IDLING_LIMIT = 60_000L

        /**
         * A wait for something that is either already on screen or not coming: long enough to
         * survive a frame or two, short enough to probe with.
         *
         * Pass it when you expect a miss — `click("Not now", wait = QUICK_WAITING_TIME)` — so the
         * check costs 50 milliseconds instead of the full five seconds.
         */
        const val QUICK_WAITING_TIME = 50L

        /**
         * Whether Kobaia replaces UIAutomator's own idle timeout with a much shorter one.
         *
         * UIAutomator gives itself ten seconds to see the screen go quiet before every single look
         * at it, which on a screen that never goes quiet is ten seconds charged to a `wait` you
         * asked to be fifty milliseconds. Kobaia lowers it to half a second, once, before your
         * first test.
         *
         * Set it to false before your first test if you would rather keep the platform default.
         */
        var tuneUiAutomatorTimeouts = true

        private const val INITIAL_TOUCH_MODE_ENABLED = true

        /**
         * How long to wait for a field to take focus once it has been tapped. Key events go
         * wherever the focus is, so typing before it lands types into the previous screen.
         */
        private const val FOCUS_WAITING_TIME = 500L

        /**
         * How long the screen has to hold still before [waitForStable] calls it settled.
         *
         * Long enough that a screen between two frames of the same change does not look stable,
         * short enough that it is not most of the waiting.
         */
        private const val STABLE_INTERVAL = 500L

        /**
         * How long a rotation gets to finish rebuilding the screen before the test carries on
         * regardless. @see rotateLandscape
         */
        private const val ROTATION_SETTLING_TIME = 2000L

        /**
         * The keyboard that typed characters are turned into key presses with. The virtual one has
         * a key for every character, so what can be typed does not depend on the layout the device
         * happens to be showing.
         *
         * Loaded here rather than borrowed from UIAutomator's own helper, whose class initialiser
         * also resolves an external storage directory and throws when none is mounted — which
         * would take typing down with it, permanently, for a reason having nothing to do with
         * typing.
         */
        private val KEYBOARD_LAYOUT: KeyCharacterMap =
            KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

        /**
         * How much of the scrollable view a single swipe covers.
         *
         * Short of a full page on purpose: swiping the whole height would put the target past the
         * top of the screen between two of the checks [scrollUntilFound] makes.
         */
        private const val SCROLL_PERCENTAGE = 0.8f

        /**
         * The buttons of the system permission dialog, whose ids moved from the package installer
         * to the permission controller, and which say "allow" in several different ways
         */
        private val ALLOW_PERMISSION_BUTTONS: Pattern =
            Pattern.compile(".*:id/permission_allow.*button")
        private val DENY_PERMISSION_BUTTONS: Pattern =
            Pattern.compile(".*:id/permission_deny.*button")
        private val ALLOW_PERMISSION_LABELS: Pattern =
            Pattern.compile("(?i)^(allow|while using the app|only this time|ok)$")
        private val DENY_PERMISSION_LABELS: Pattern =
            Pattern.compile("(?i)^(deny|don.t allow|no thanks)$")

        inline fun <reified T : Activity> create(): Kobaia<T> = create(T::class.java)

        @JvmStatic
        fun <T : Activity> create(activityClass: Class<T>): Kobaia<T> = Kobaia(activityClass)

        // ---------------------------------------------------------------------------------------
        // Finding
        // ---------------------------------------------------------------------------------------

        /**
         * Get the first UiObject2 with the given text that appears on screen.
         * This method also waits for it for some milliseconds
         * @param text the text that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun find(
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): UiObject2? = findFirst(By.text(text), wait)

        /**
         * Get the first UiObject2 whose text matches the given pattern that appears on screen.
         * This method also waits for it for some milliseconds
         * @param pattern the Pattern that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun find(
            pattern: Pattern,
            wait: Long = DEFAULT_WAITING_TIME
        ): UiObject2? = findFirst(By.text(pattern), wait)

        /**
         * Get the first UiObject2 with the given content description that appears on screen.
         * This is useful to search for Images or EditTexts
         * This method also waits for it for some milliseconds
         * @param text the description that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun findDescription(
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): UiObject2? = findFirst(By.desc(text), wait)

        /**
         * Get the first UiObject2 whose content description matches the given pattern.
         * This is useful to search for Images or EditTexts
         * This method also waits for it for some milliseconds
         * @param pattern the text pattern from the description that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun findDescription(
            pattern: Pattern,
            wait: Long = DEFAULT_WAITING_TIME
        ): UiObject2? = findFirst(By.desc(pattern), wait)

        /**
         * Get the first UiObject2 with the given Compose testTag (or View resource id) that
         * appears on screen.
         * This method also waits for it for some milliseconds
         * @param tag the testTag that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun findTag(
            tag: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): UiObject2? = findFirst(By.res(tag), wait)

        /**
         * Get the first UiObject2 whose Compose testTag (or View resource id) matches the given
         * pattern.
         * This method also waits for it for some milliseconds
         * @param pattern the testTag pattern that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun findTag(
            pattern: Pattern,
            wait: Long = DEFAULT_WAITING_TIME
        ): UiObject2? = findFirst(By.res(pattern), wait)

        /**
         * The first view the selector matches, waiting for it to show up
         * @param selector what the view has to match
         * @param wait how long to wait for it before giving up, in milliseconds
         */
        private fun findFirst(selector: BySelector, wait: Long): UiObject2? =
            device().wait(Until.findObject(selector), wait)

        /**
         * Whether anything matching the selector shows up before the wait runs out.
         * Cheaper than [findFirst]: nothing has to be wrapped in a UiObject2 to answer.
         * @param selector what the view has to match
         * @param wait how long to wait for it before giving up, in milliseconds
         */
        private fun isShowing(selector: BySelector, wait: Long): Boolean =
            device().wait(Until.hasObject(selector), wait) == true

        // ---------------------------------------------------------------------------------------
        // Checking
        // ---------------------------------------------------------------------------------------

        /**
         * Check if a text is visible on screen.
         * This method also waits for it for some milliseconds
         * @param text the text that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun isVisible(
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = isShowing(By.text(text), wait)

        /**
         * Check if a text pattern is visible on screen.
         * This method also waits for it for some milliseconds
         * @param pattern the pattern that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun isVisible(
            pattern: Pattern,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = isShowing(By.text(pattern), wait)

        /**
         * Check if the given text is part of a text visible on screen.
         * This method also waits for it for some milliseconds
         * @param text the text that you want to search in your screen (could be a text inside another text)
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun containsText(
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = isShowing(By.textContains(text), wait)

        /**
         * Check if a content description is visible on screen.
         * This is useful to search for Images or EditTexts
         * This method also waits for it for some milliseconds
         * @param text the description that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun isDescriptionVisible(
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = isShowing(By.desc(text), wait)

        /**
         * Check if a content description matching the pattern is visible on screen.
         * This is useful to search for Images or EditTexts
         * This method also waits for it for some milliseconds
         * @param pattern the text pattern from the description that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun isDescriptionVisible(
            pattern: Pattern,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = isShowing(By.desc(pattern), wait)

        /**
         * Check if a Compose testTag (or View resource id) is visible on screen.
         * This method also waits for it for some milliseconds
         * @param tag the testTag that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun isTagVisible(
            tag: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = isShowing(By.res(tag), wait)

        /**
         * Check if a Compose testTag (or View resource id) matching the pattern is visible.
         * This method also waits for it for some milliseconds
         * @param pattern the testTag pattern that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun isTagVisible(
            pattern: Pattern,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = isShowing(By.res(pattern), wait)

        /**
         * Assert that a text is visible on screen, failing the test with a readable message if it
         * is not.
         * This method also waits for it for some milliseconds
         * @param text the text that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun assertVisible(
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ) = assertTrue("$text should be visible", isVisible(text, wait))

        /**
         * Assert that a text pattern is visible on screen, failing the test with a readable
         * message if it is not.
         * This method also waits for it for some milliseconds
         * @param pattern the pattern that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun assertVisible(
            pattern: Pattern,
            wait: Long = DEFAULT_WAITING_TIME
        ) = assertTrue("$pattern should be visible", isVisible(pattern, wait))

        /**
         * Assert that a Compose testTag (or View resource id) is visible on screen, failing the
         * test with a readable message if it is not.
         * This method also waits for it for some milliseconds
         * @param tag the testTag that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun assertTagVisible(
            tag: String,
            wait: Long = DEFAULT_WAITING_TIME
        ) = assertTrue("A view tagged $tag should be visible", isTagVisible(tag, wait))

        /**
         * Assert that a Compose testTag (or View resource id) matching the pattern is visible on
         * screen, failing the test with a readable message if it is not.
         * This method also waits for it for some milliseconds
         * @param pattern the testTag pattern that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun assertTagVisible(
            pattern: Pattern,
            wait: Long = DEFAULT_WAITING_TIME
        ) = assertTrue("A view tagged $pattern should be visible", isTagVisible(pattern, wait))

        /**
         * Assert that a content description is visible on screen, failing the test with a
         * readable message if it is not.
         * @param text the description that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun assertDescriptionVisible(
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ) = assertTrue("A view described as $text should be visible", isDescriptionVisible(text, wait))

        /**
         * Assert that a text is **not** on screen.
         *
         * The wait is short by default: confirming an absence means polling until the wait runs
         * out, so the five second default would be paid in full every time.
         *
         * A longer wait makes this stricter, not more patient — it fails the moment the text turns
         * up at any point during it. Use [waitUntilGone] for something that is on screen now and
         * has to leave.
         * @param text the text that should not be in your screen
         * @param wait how long to keep looking before concluding it is absent (Default is 50 milliseconds)
         */
        fun assertNotVisible(
            text: String,
            wait: Long = QUICK_WAITING_TIME
        ) = assertFalse("$text should not be visible", isVisible(text, wait))

        /**
         * Assert that a text pattern is **not** on screen. @see assertNotVisible
         * @param pattern the pattern that should not be in your screen
         * @param wait how long to keep looking before concluding it is absent (Default is 50 milliseconds)
         */
        fun assertNotVisible(
            pattern: Pattern,
            wait: Long = QUICK_WAITING_TIME
        ) = assertFalse("$pattern should not be visible", isVisible(pattern, wait))

        /**
         * Assert that a content description is **not** on screen. @see assertNotVisible
         * @param text the description that should not be in your screen
         * @param wait how long to keep looking before concluding it is absent (Default is 50 milliseconds)
         */
        fun assertDescriptionNotVisible(
            text: String,
            wait: Long = QUICK_WAITING_TIME
        ) = assertFalse(
            "A view described as $text should not be visible",
            isDescriptionVisible(text, wait)
        )

        /**
         * Assert that a Compose testTag (or View resource id) is **not** on screen.
         * @see assertNotVisible
         * @param tag the testTag that should not be in your screen
         * @param wait how long to keep looking before concluding it is absent (Default is 50 milliseconds)
         */
        fun assertTagNotVisible(
            tag: String,
            wait: Long = QUICK_WAITING_TIME
        ) = assertFalse("A view tagged $tag should not be visible", isTagVisible(tag, wait))

        /**
         * Wait for a text that is on screen now to go away — a spinner, a toast, a splash screen.
         *
         * The mirror image of [assertNotVisible], and the one that waiting longer actually helps:
         * this polls while the text is still there and returns as soon as it has gone, where
         * `assertNotVisible` polls for the text to appear and fails if it does.
         *
         * It reports a text that never leaves with false rather than failing, like every other
         * non-`assert` interaction. Follow it with [assertNotVisible] to make it a requirement.
         * @param text the text that should go away
         * @param wait how long to give it to go (Default is 5000 milliseconds)
         * @return whether it had gone before the wait ran out
         */
        fun waitUntilGone(
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = isGone(By.text(text), wait)

        /**
         * Wait for a text matching the pattern to go away. @see waitUntilGone
         * @param pattern the pattern that should go away
         * @param wait how long to give it to go (Default is 5000 milliseconds)
         * @return whether it had gone before the wait ran out
         */
        fun waitUntilGone(
            pattern: Pattern,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = isGone(By.text(pattern), wait)

        /**
         * Wait for a content description to go away. @see waitUntilGone
         * @param text the description that should go away
         * @param wait how long to give it to go (Default is 5000 milliseconds)
         * @return whether it had gone before the wait ran out
         */
        fun waitUntilDescriptionGone(
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = isGone(By.desc(text), wait)

        /**
         * Wait for a Compose testTag (or View resource id) to go away. @see waitUntilGone
         * @param tag the testTag that should go away
         * @param wait how long to give it to go (Default is 5000 milliseconds)
         * @return whether it had gone before the wait ran out
         */
        fun waitUntilTagGone(
            tag: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = isGone(By.res(tag), wait)

        /**
         * Whether everything the selector matches has left the screen before the wait runs out.
         *
         * The inverse of [isShowing] in more than name: this one keeps polling for as long as
         * something is still matching, and gives up when the wait is spent.
         * @param selector what the views that have to go match
         * @param wait how long to give them to go, in milliseconds
         */
        private fun isGone(selector: BySelector, wait: Long): Boolean =
            device().wait(Until.gone(selector), wait) == true

        // ---------------------------------------------------------------------------------------
        // State
        // ---------------------------------------------------------------------------------------

        /**
         * Whether the first view with this text is enabled. A view that never showed up counts as
         * not enabled — use [assertEnabled] when the difference matters
         * @param text the text of the view to look at
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun isEnabled(
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = find(text, wait)?.isEnabled == true

        /**
         * Whether the first view with this Compose testTag (or View resource id) is enabled.
         * @param tag the testTag of the view to look at
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun isTagEnabled(
            tag: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = findTag(tag, wait)?.isEnabled == true

        /**
         * Whether the first view with this text is checked — a checkbox, a switch, a radio button.
         * @param text the text of the view to look at
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun isChecked(
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = find(text, wait)?.isChecked == true

        /**
         * Whether the first view with this Compose testTag (or View resource id) is checked.
         * @param tag the testTag of the view to look at
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun isTagChecked(
            tag: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = findTag(tag, wait)?.isChecked == true

        /**
         * Assert that the view with this text is on screen and enabled
         */
        fun assertEnabled(text: String, wait: Long = DEFAULT_WAITING_TIME) =
            assertTrue("$text should be enabled", requireVisible(By.text(text), wait, text).isEnabled)

        /**
         * Assert that the view with this text is on screen and disabled
         */
        fun assertDisabled(text: String, wait: Long = DEFAULT_WAITING_TIME) =
            assertFalse("$text should be disabled", requireVisible(By.text(text), wait, text).isEnabled)

        /**
         * Assert that the view with this Compose testTag is on screen and enabled
         */
        fun assertTagEnabled(tag: String, wait: Long = DEFAULT_WAITING_TIME) =
            assertTrue("$tag should be enabled", requireVisible(By.res(tag), wait, tag).isEnabled)

        /**
         * Assert that the view with this Compose testTag is on screen and disabled
         */
        fun assertTagDisabled(tag: String, wait: Long = DEFAULT_WAITING_TIME) =
            assertFalse("$tag should be disabled", requireVisible(By.res(tag), wait, tag).isEnabled)

        /**
         * Assert that the view with this text is on screen and checked
         */
        fun assertChecked(text: String, wait: Long = DEFAULT_WAITING_TIME) =
            assertTrue("$text should be checked", requireVisible(By.text(text), wait, text).isChecked)

        /**
         * Assert that the view with this text is on screen and not checked
         */
        fun assertUnchecked(text: String, wait: Long = DEFAULT_WAITING_TIME) =
            assertFalse("$text should be unchecked", requireVisible(By.text(text), wait, text).isChecked)

        /**
         * Assert that the view with this Compose testTag is on screen and checked
         */
        fun assertTagChecked(tag: String, wait: Long = DEFAULT_WAITING_TIME) =
            assertTrue("$tag should be checked", requireVisible(By.res(tag), wait, tag).isChecked)

        /**
         * Assert that the view with this Compose testTag is on screen and not checked
         */
        fun assertTagUnchecked(tag: String, wait: Long = DEFAULT_WAITING_TIME) =
            assertFalse("$tag should be unchecked", requireVisible(By.res(tag), wait, tag).isChecked)

        /**
         * The first view the selector matches, failing the test if it never showed up.
         *
         * The state assertions go through this rather than through the nullable finders, so that
         * asking whether a missing view is disabled fails as "not visible" instead of passing.
         * @param selector what the view has to match
         * @param wait how long to wait for it before giving up, in milliseconds
         * @param description how to name the view in the failure message
         */
        private fun requireVisible(selector: BySelector, wait: Long, description: String): UiObject2 =
            findFirst(selector, wait) ?: throw AssertionError("$description should be visible")

        // ---------------------------------------------------------------------------------------
        // Clicking
        // ---------------------------------------------------------------------------------------

        /**
         * Click every UiObject2 with the given text. This method won't fail your test if nothing
         * is clicked
         * This method also waits for it for some milliseconds
         * @param text the text that you want to be clicked in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether anything was clicked at all
         */
        fun click(
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = clickAll(By.text(text), wait)

        /**
         * Click every UiObject2 whose text matches the pattern. This method won't fail your test
         * if nothing is clicked
         * This method also waits for it for some milliseconds
         * @param pattern the text pattern that you want to be clicked in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether anything was clicked at all
         */
        fun click(
            pattern: Pattern,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = clickAll(By.text(pattern), wait)

        /**
         * Click every UiObject2 that contains the given text. This method won't fail your test if
         * nothing is clicked
         * This method also waits for it for some milliseconds
         * @param text the subtext of what you want to be clicked in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether anything was clicked at all
         */
        fun clickContaining(
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = clickAll(By.textContains(text), wait)

        /**
         * Click every UiObject2 with the given content description. This is useful to search for
         * Images or EditTexts
         * This method won't fail your test if nothing is clicked
         * This method also waits for it for some milliseconds
         * @param text the description of what you want to be clicked in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether anything was clicked at all
         */
        fun clickDescription(
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = clickAll(By.desc(text), wait)

        /**
         * Click every UiObject2 whose content description matches the pattern. This is useful to
         * search for Images or EditTexts
         * This method won't fail your test if nothing is clicked
         * This method also waits for it for some milliseconds
         * @param pattern the description pattern of what you want to be clicked in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether anything was clicked at all
         */
        fun clickDescription(
            pattern: Pattern,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = clickAll(By.desc(pattern), wait)

        /**
         * Click every UiObject2 with the given Compose testTag (or View resource id). This method
         * won't fail your test if nothing is clicked
         * This method also waits for it for some milliseconds
         * @param tag the testTag of what you want to be clicked in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether anything was clicked at all
         */
        fun clickTag(
            tag: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = clickAll(By.res(tag), wait)

        /**
         * Click every UiObject2 whose Compose testTag (or View resource id) matches the pattern.
         * This method won't fail your test if nothing is clicked
         * This method also waits for it for some milliseconds
         * @param pattern the testTag pattern of what you want to be clicked in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether anything was clicked at all
         */
        fun clickTag(
            pattern: Pattern,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = clickAll(By.res(pattern), wait)

        /**
         * Click every view the selector matches, waiting for them to show up
         * @param selector what the views have to match
         * @param wait how long to wait for them before giving up, in milliseconds
         * @return whether anything was clicked at all
         */
        /**
         * Long click every UiObject2 with the given text. This method won't fail your test if
         * nothing is clicked
         * @param text the text that you want to be long clicked in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether anything was long clicked at all
         */
        fun longClick(
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = longClickAll(By.text(text), wait)

        /**
         * Long click every UiObject2 whose text matches the pattern
         * @param pattern the text pattern that you want to be long clicked in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether anything was long clicked at all
         */
        fun longClick(
            pattern: Pattern,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = longClickAll(By.text(pattern), wait)

        /**
         * Long click every UiObject2 with the given content description
         * @param text the description of what you want to be long clicked in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether anything was long clicked at all
         */
        fun longClickDescription(
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = longClickAll(By.desc(text), wait)

        /**
         * Long click every UiObject2 whose content description matches the pattern
         * @param pattern the description pattern of what you want to be long clicked
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether anything was long clicked at all
         */
        fun longClickDescription(
            pattern: Pattern,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = longClickAll(By.desc(pattern), wait)

        /**
         * Long click every UiObject2 with the given Compose testTag (or View resource id)
         * @param tag the testTag of what you want to be long clicked in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether anything was long clicked at all
         */
        fun longClickTag(
            tag: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = longClickAll(By.res(tag), wait)

        /**
         * Long click every UiObject2 whose Compose testTag (or View resource id) matches the
         * pattern
         * @param pattern the testTag pattern of what you want to be long clicked
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether anything was long clicked at all
         */
        fun longClickTag(
            pattern: Pattern,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = longClickAll(By.res(pattern), wait)

        /**
         * Long click every view the selector matches, waiting for them to show up
         * @param selector what the views have to match
         * @param wait how long to wait for them before giving up, in milliseconds
         * @return whether anything was long clicked at all
         */
        private fun longClickAll(selector: BySelector, wait: Long): Boolean =
            actOnAll(selector, wait, UiObject2::longClick)

        /**
         * Click the first view the selector matches, and only that one — for the times when
         * clicking every match would be wrong, such as a dialog offering several ways to say yes
         * @param selector what the view has to match
         * @param wait how long to wait for it before giving up, in milliseconds
         * @return whether anything was clicked
         */
        private fun clickFirst(selector: BySelector, wait: Long): Boolean {
            val view = findFirst(selector, wait) ?: return false
            view.click()
            return true
        }

        private fun clickAll(selector: BySelector, wait: Long): Boolean =
            actOnAll(selector, wait, UiObject2::click)

        /**
         * Do something to every view the selector matches, and report whether anything was done.
         *
         * The views are found in one pass and acted on one after another, so the first one can
         * take the screen out from under the rest — clicking a button that navigates away is the
         * ordinary case, not the exotic one. A view that has gone by the time its turn comes is
         * skipped: it cannot be acted on, and it is not a reason to fail a test that is already
         * on the next screen.
         * @param selector what the views have to match
         * @param wait how long to wait for them before giving up, in milliseconds
         * @param act what to do to each of them
         * @return whether anything was acted on at all
         */
        private fun actOnAll(
            selector: BySelector,
            wait: Long,
            act: (UiObject2) -> Unit
        ): Boolean {
            val views = device().wait(Until.findObjects(selector), wait).orEmpty()
            var actedOnAnything = false
            views.forEach { view ->
                try {
                    act(view)
                    actedOnAnything = true
                } catch (viewIsGone: StaleObjectException) {
                    Log.d(KOBAIA_TAG, "A view matching $selector went away before its turn")
                }
            }
            return actedOnAnything
        }

        // ---------------------------------------------------------------------------------------
        // Typing
        // ---------------------------------------------------------------------------------------

        /**
         * Set a text on the field with the given content description, in one go rather than key by
         * key. A field that refuses to be set that way is typed into instead, so the text arrives
         * either way. This method won't fail your test if the field is not on screen
         * @param text the text that will be set
         * @param into the description of the field that will receive the text
         * @param wait how long you want to wait for the field (Default is 5000 milliseconds)
         * @return the field that received the text, or null if it never showed up
         */
        fun type(
            text: String,
            into: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): UiObject2? = setTextOn(findDescription(into, wait), text)

        /**
         * Put a text into a field, and make sure it actually arrived.
         *
         * Setting text is a single accessibility action, and a field is free to refuse it — a
         * Compose `TextField` reached by its content description does exactly that. UIAutomator
         * writes a line to the log when the action is refused and carries on, so without this the
         * field stays empty, [type] still hands back the field it found, and the test only
         * discovers something is wrong several screens later.
         * @param field the field to fill, or null if it never showed up
         * @param text the text it should end up containing
         * @return the field, or null if it never showed up
         */
        private fun setTextOn(field: UiObject2?, text: String): UiObject2? {
            val fieldOnScreen = field ?: return null
            fieldOnScreen.text = text
            if (fieldOnScreen.wait(Until.textEquals(text), QUICK_WAITING_TIME) == true) {
                return fieldOnScreen
            }
            // Emptied first, so that a field which took the text but does not read it back — one
            // that masks a password, say — ends up with the text once rather than twice.
            fieldOnScreen.clear()
            typeCharacterByCharacter(fieldOnScreen, text)
            return fieldOnScreen
        }

        /**
         * Type a text on the field with the given content description one key press at a time.
         * This function is useful to test text change listeners or other types of dynamic changes
         * while the user is typing in the screen
         * @param text the text that will be typed (anything the keyboard can produce)
         * @param into the description of the field that will receive the text
         * @param wait how long you want to wait for the field (Default is 5000 milliseconds)
         */
        fun typeOnKeyboard(
            text: String,
            into: String,
            wait: Long = DEFAULT_WAITING_TIME
        ) = typeCharacterByCharacter(findDescription(into, wait), text)

        /**
         * Tap a field open and then type its text into it, one key press at a time.
         *
         * The characters are sent as key events rather than tapped on the soft keyboard, so the
         * app sees the same one-key-at-a-time arrival a `TextWatcher` is there to react to,
         * without the text having to be spelled out of keys that happen to be on screen: capitals,
         * symbols and anything on a page of the keyboard that is not showing all work.
         * @param field the field to type into, or null if it never showed up
         * @param text the text to type
         */
        private fun typeCharacterByCharacter(field: UiObject2?, text: String) {
            val fieldOnScreen = field ?: return
            fieldOnScreen.click()
            // Key events go wherever the focus is, so the tap has to have landed first. A field
            // that never takes focus is not worth failing over: the keys are sent regardless.
            fieldOnScreen.wait(Until.focused(true), FOCUS_WAITING_TIME)
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            text.forEach { character ->
                // A character the keyboard cannot produce yields no events at all. Skipping it is
                // the same miss every other interaction reports quietly, rather than a crash.
                KEYBOARD_LAYOUT.getEvents(charArrayOf(character))
                    ?.forEach(instrumentation::sendKeySync)
            }
        }

        /**
         * Set a text on the field with the given Compose testTag (or View resource id), in one go
         * rather than key by key. A field that refuses to be set that way is typed into instead.
         * This method won't fail your test if the field is not on screen
         * @param text the text that will be set
         * @param tag the testTag of the field that will receive the text
         * @param wait how long you want to wait for the field (Default is 5000 milliseconds)
         * @return the field that received the text, or null if it never showed up
         */
        fun typeIntoTag(
            text: String,
            tag: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): UiObject2? = setTextOn(findTag(tag, wait), text)

        /**
         * Type a text on the field with the given Compose testTag (or View resource id) one key
         * press at a time.
         * This function is useful to test text change listeners or other types of dynamic changes
         * while the user is typing in the screen
         * @param text the text that will be typed (anything the keyboard can produce)
         * @param tag the testTag of the field that will receive the text
         * @param wait how long you want to wait for the field (Default is 5000 milliseconds)
         */
        fun typeOnKeyboardIntoTag(
            text: String,
            tag: String,
            wait: Long = DEFAULT_WAITING_TIME
        ) = typeCharacterByCharacter(findTag(tag, wait), text)

        /**
         * Empty the field with the given content description
         * @param into the description of the field to clear
         * @param wait how long you want to wait for the field (Default is 5000 milliseconds)
         * @return the field that was cleared, or null if it never showed up
         */
        fun clearText(
            into: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): UiObject2? = findDescription(into, wait)?.apply { clear() }

        /**
         * Empty the field with the given Compose testTag (or View resource id)
         * @param tag the testTag of the field to clear
         * @param wait how long you want to wait for the field (Default is 5000 milliseconds)
         * @return the field that was cleared, or null if it never showed up
         */
        fun clearTextInTag(
            tag: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): UiObject2? = findTag(tag, wait)?.apply { clear() }

        // ---------------------------------------------------------------------------------------
        // Scrolling
        // ---------------------------------------------------------------------------------------

        /**
         * Scroll the first scrollable view forward (RecyclerView, ListView, ScrollView, …) until a text
         * is visible.
         * @param text the text that you want to find
         * @param maximumScrolls how many times it will swipe before giving up (Default: 10)
         * @return the text view, or null if it was not reached
         */
        fun scrollTo(
            text: String,
            maximumScrolls: Int = DEFAULT_MAXIMUM_SCROLLS
        ): UiObject2? = scrollUntilFound(maximumScrolls) { find(text, QUICK_WAITING_TIME) }

        /**
         * Scroll the first scrollable view forward (RecyclerView, ListView, ScrollView, …) until a text
         * pattern is visible.
         * @param pattern the pattern that you want to find
         * @param maximumScrolls how many times it will swipe before giving up (Default: 10)
         * @return the text view, or null if it was not reached
         */
        fun scrollTo(
            pattern: Pattern,
            maximumScrolls: Int = DEFAULT_MAXIMUM_SCROLLS
        ): UiObject2? = scrollUntilFound(maximumScrolls) { find(pattern, QUICK_WAITING_TIME) }

        /**
         * Scroll the first scrollable view forward (RecyclerView, ListView, ScrollView, …) until a content
         * description is visible. This is useful to search for Images or EditTexts
         * @param text the description that you want to find
         * @param maximumScrolls how many times it will swipe before giving up (Default: 10)
         * @return the view, or null if it was not reached
         */
        fun scrollToDescription(
            text: String,
            maximumScrolls: Int = DEFAULT_MAXIMUM_SCROLLS
        ): UiObject2? = scrollUntilFound(maximumScrolls) { findDescription(text, QUICK_WAITING_TIME) }

        /**
         * Scroll the first scrollable view forward (RecyclerView, ListView, ScrollView, …) until a content
         * description matching the pattern is visible.
         * This is useful to search for Images or EditTexts
         * @param pattern the description pattern that you want to find
         * @param maximumScrolls how many times it will swipe before giving up (Default: 10)
         * @return the view, or null if it was not reached
         */
        fun scrollToDescription(
            pattern: Pattern,
            maximumScrolls: Int = DEFAULT_MAXIMUM_SCROLLS
        ): UiObject2? = scrollUntilFound(maximumScrolls) { findDescription(pattern, QUICK_WAITING_TIME) }

        /**
         * Scroll the first scrollable view forward (LazyColumn, RecyclerView, ListView, ScrollView, …)
         * until a Compose testTag (or View resource id) is visible.
         * @param tag the testTag that you want to find
         * @param maximumScrolls how many times it will swipe before giving up (Default: 10)
         * @return the view, or null if it was not reached
         */
        fun scrollToTag(
            tag: String,
            maximumScrolls: Int = DEFAULT_MAXIMUM_SCROLLS
        ): UiObject2? = scrollUntilFound(maximumScrolls) { findTag(tag, QUICK_WAITING_TIME) }

        /**
         * Scroll the first scrollable view forward (LazyColumn, RecyclerView, ListView, ScrollView, …)
         * until a Compose testTag (or View resource id) matching the pattern is visible.
         * @param pattern the testTag pattern that you want to find
         * @param maximumScrolls how many times it will swipe before giving up (Default: 10)
         * @return the view, or null if it was not reached
         */
        fun scrollToTag(
            pattern: Pattern,
            maximumScrolls: Int = DEFAULT_MAXIMUM_SCROLLS
        ): UiObject2? = scrollUntilFound(maximumScrolls) { findTag(pattern, QUICK_WAITING_TIME) }

        /**
         * Swipe the first scrollable view forward until the target turns up, checking before
         * every swipe so that a target already on screen costs none.
         *
         * It searches forward from wherever the list currently sits rather than rewinding to the
         * top first, and stops as soon as the list says it cannot scroll any further, so the cost
         * of a miss is bounded by [maximumScrolls] swipes.
         * @param maximumScrolls how many times to swipe before giving up
         * @param find how the target is looked up between swipes
         */
        private fun scrollUntilFound(maximumScrolls: Int, find: () -> UiObject2?): UiObject2? {
            repeat(maximumScrolls.coerceAtLeast(1)) {
                find()?.let { return it }
                // Looked up again on every pass rather than held on to: a list that recycles its
                // rows, as a RecyclerView and a LazyColumn both do, can replace the very node the
                // last swipe was performed on. Nothing scrollable on screen means the list is
                // still loading — like every other finder, this one reports that with null.
                // Waited for properly rather than probed for: a screen that has only just been
                // launched has not composed its list yet, and a quick look would conclude there is
                // nothing to scroll a frame before there is. The full wait is only ever paid when
                // there is genuinely nothing scrollable, which is the miss this reports as null.
                val scrollableView =
                    findFirst(By.scrollable(true), DEFAULT_WAITING_TIME) ?: return null
                scrollableView.scroll(Direction.DOWN, SCROLL_PERCENTAGE)
            }
            return find()
        }

        // ---------------------------------------------------------------------------------------
        // Device
        // ---------------------------------------------------------------------------------------

        /**
         * Make the app wait
         * @param wait time in milliseconds
         */
        fun waitFor(wait: Long = DEFAULT_WAITING_TIME) = KobaiaSleep.sleep(wait)

        /**
         * Wait for the screen to stop changing, and no longer.
         *
         * The alternative to guessing a number in [waitFor]: it returns as soon as the screen has
         * held still for [STABLE_INTERVAL], however long that takes, instead of sleeping through
         * the wait whether or not anything is still moving.
         *
         * It watches what the app reports to the accessibility tree, not the pixels and not the
         * main thread, so a screen that animates forever — a spinner, a blinking cursor, most
         * Compose screens with something in flight — is a screen that never settles. That costs
         * the full [wait] and comes back false, the same way a finder that misses does. It never
         * hangs, and it never fails the test on its own.
         *
         * @param wait how long to give the screen to settle before giving up, in milliseconds
         * @return whether the screen settled, rather than running out of time
         */
        fun waitForStable(wait: Long = DEFAULT_WAITING_TIME): Boolean =
            !device().waitForStableInActiveWindow(
                stableTimeoutMs = wait,
                stableIntervalMs = STABLE_INTERVAL,
                // Comparing screenshots as well would mean a cursor blinking in a corner counts as
                // a screen still in motion, and nothing on Compose would ever be stable.
                requireStableScreenshot = false
            ).isTimeout

        /**
         * Get the UiDevice using the InstrumentationRegistry, so anything UIAutomator can do
         * (hardware keys, the launcher, the notification shade, other apps) is one step away
         */
        fun device(): UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        /**
         * Press the back button
         */
        fun pressBack(): Boolean = device().pressBack()

        /**
         * Press the home button, sending the app under test to the background
         */
        fun pressHome(): Boolean = device().pressHome()

        /**
         * Press enter, which submits most forms
         */
        fun pressEnter(): Boolean = device().pressEnter()

        /**
         * Open the recent apps switcher
         */
        fun pressRecentApps(): Boolean = device().pressRecentApps()

        /**
         * Close the soft keyboard.
         *
         * There is no way to ask the system to close only the keyboard, so this presses back —
         * which closes it when it is open, and navigates back when it is not.
         */
        fun closeKeyboard(): Boolean = device().pressBack()

        /**
         * Pull down the notification shade. The notifications themselves are read and clicked with
         * the same functions as anything else
         */
        fun openNotifications(): Boolean = device().openNotification()

        /**
         * Grant the runtime permission the system is currently asking for.
         *
         * The dialog belongs to the system rather than to your app, which is exactly the kind of
         * thing UIAutomator can reach. Its buttons are matched by resource id first, and by their
         * label as a fallback, so it survives both the wording and the package moving between
         * Android versions.
         * @param wait how long to wait for the dialog (Default is 5000 milliseconds)
         * @return whether a permission dialog was there to answer
         */
        fun allowPermission(wait: Long = DEFAULT_WAITING_TIME): Boolean =
            clickFirst(By.res(ALLOW_PERMISSION_BUTTONS), wait) ||
                clickFirst(By.text(ALLOW_PERMISSION_LABELS), QUICK_WAITING_TIME)

        /**
         * Refuse the runtime permission the system is currently asking for. @see allowPermission
         * @param wait how long to wait for the dialog (Default is 5000 milliseconds)
         * @return whether a permission dialog was there to answer
         */
        fun denyPermission(wait: Long = DEFAULT_WAITING_TIME): Boolean =
            clickFirst(By.res(DENY_PERMISSION_BUTTONS), wait) ||
                clickFirst(By.text(DENY_PERMISSION_LABELS), QUICK_WAITING_TIME)

        /**
         * Turn the device to landscape and hold it there.
         *
         * The rotation stays frozen until [rotateNatural] puts it back, so that the app cannot
         * quietly rotate underneath the rest of the test.
         */
        fun rotateLandscape() {
            device().setOrientationLandscape()
            settleAfterRotation()
        }

        /**
         * Turn the device to portrait and hold it there. @see rotateLandscape
         */
        fun rotatePortrait() {
            device().setOrientationPortrait()
            settleAfterRotation()
        }

        /**
         * Put the device back the way it was, and let it rotate on its own again
         */
        fun rotateNatural() {
            device().setOrientationNatural()
            device().unfreezeRotation()
            settleAfterRotation()
        }

        /**
         * Hold until the rotated screen has finished rebuilding itself.
         *
         * UIAutomator returns from a rotation as soon as the display has swapped its width and
         * height, which happens long before the activity that was destroyed and recreated has
         * anything on screen. The finders would poll through that on their own, but a check that
         * expects to find nothing would not: it would be answered by a screen that is still empty
         * because it has not been drawn yet, and pass for the wrong reason.
         *
         * Bounded well under the usual wait, because this one is paid whether or not anything was
         * wrong: an app that never settles gets on with the test instead.
         */
        private fun settleAfterRotation() {
            waitForStable(ROTATION_SETTLING_TIME)
        }

        // ---------------------------------------------------------------------------------------
        // Deprecated names, kept so tests written against older versions keep compiling
        // ---------------------------------------------------------------------------------------

        @Deprecated("Renamed", ReplaceWith("find(text, wait)"))
        fun byText(text: String, wait: Long = DEFAULT_WAITING_TIME) = find(text, wait)

        @Deprecated("Renamed", ReplaceWith("find(pattern, wait)"))
        fun byText(pattern: Pattern, wait: Long = DEFAULT_WAITING_TIME) = find(pattern, wait)

        @Deprecated("Renamed", ReplaceWith("findDescription(text, wait)"))
        fun byDescription(text: String, wait: Long = DEFAULT_WAITING_TIME) =
            findDescription(text, wait)

        @Deprecated("Renamed", ReplaceWith("findDescription(pattern, wait)"))
        fun byDescription(pattern: Pattern, wait: Long = DEFAULT_WAITING_TIME) =
            findDescription(pattern, wait)

        @Deprecated("Renamed", ReplaceWith("isVisible(text, wait)"))
        fun textExists(text: String, wait: Long = DEFAULT_WAITING_TIME) = isVisible(text, wait)

        @Deprecated("Renamed", ReplaceWith("isVisible(pattern, wait)"))
        fun textExists(pattern: Pattern, wait: Long = DEFAULT_WAITING_TIME) =
            isVisible(pattern, wait)

        @Deprecated("Renamed", ReplaceWith("isDescriptionVisible(text, wait)"))
        fun descriptionExist(text: String, wait: Long = DEFAULT_WAITING_TIME) =
            isDescriptionVisible(text, wait)

        @Deprecated("Renamed", ReplaceWith("isDescriptionVisible(pattern, wait)"))
        fun descriptionExist(pattern: Pattern, wait: Long = DEFAULT_WAITING_TIME) =
            isDescriptionVisible(pattern, wait)

        @Deprecated("Renamed", ReplaceWith("assertVisible(text, wait)"))
        fun assertTextExist(text: String, wait: Long = DEFAULT_WAITING_TIME) =
            assertVisible(text, wait)

        @Deprecated("Renamed", ReplaceWith("assertVisible(text, wait)"))
        fun assertTextExist(text: Pattern, wait: Long = DEFAULT_WAITING_TIME) =
            assertVisible(text, wait)

        @Deprecated("Renamed", ReplaceWith("click(text, wait)"))
        fun textClick(text: String, wait: Long = DEFAULT_WAITING_TIME) = click(text, wait)

        @Deprecated("Renamed", ReplaceWith("click(pattern, wait)"))
        fun textClick(pattern: Pattern, wait: Long = DEFAULT_WAITING_TIME) = click(pattern, wait)

        @Deprecated("Renamed", ReplaceWith("clickContaining(text, wait)"))
        fun containsClick(text: String, wait: Long = DEFAULT_WAITING_TIME) =
            clickContaining(text, wait)

        @Deprecated("Renamed", ReplaceWith("clickDescription(text, wait)"))
        fun descriptionClick(text: String, wait: Long = DEFAULT_WAITING_TIME) =
            clickDescription(text, wait)

        @Deprecated("Renamed", ReplaceWith("clickDescription(pattern, wait)"))
        fun descriptionClick(pattern: Pattern, wait: Long = DEFAULT_WAITING_TIME) =
            clickDescription(pattern, wait)

        @Deprecated("Renamed", ReplaceWith("typeOnKeyboard(text, fieldDescription, wait)"))
        fun slowingTypeNumberInKeyboard(
            fieldDescription: String,
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ) = typeOnKeyboard(text, fieldDescription, wait)

        @Deprecated("Renamed", ReplaceWith("scrollTo(text, maximumScrolls)"))
        fun scrollUntilFindText(text: String, maximumScrolls: Int = DEFAULT_MAXIMUM_SCROLLS) =
            scrollTo(text, maximumScrolls)

        @Deprecated("Renamed", ReplaceWith("scrollTo(pattern, maximumScrolls)"))
        fun scrollUntilFindPattern(pattern: Pattern, maximumScrolls: Int = DEFAULT_MAXIMUM_SCROLLS) =
            scrollTo(pattern, maximumScrolls)

        @Deprecated("Renamed", ReplaceWith("scrollToDescription(text, maximumScrolls)"))
        fun scrollUntilFindDescription(
            text: String,
            maximumScrolls: Int = DEFAULT_MAXIMUM_SCROLLS
        ) = scrollToDescription(text, maximumScrolls)

        @Deprecated("Renamed", ReplaceWith("scrollToDescription(pattern, maximumScrolls)"))
        fun scrollUntilFindDescription(
            pattern: Pattern,
            maximumScrolls: Int = DEFAULT_MAXIMUM_SCROLLS
        ) = scrollToDescription(pattern, maximumScrolls)

        @Deprecated("Renamed", ReplaceWith("waitFor(wait)"))
        fun waitTest(wait: Long = DEFAULT_WAITING_TIME) = waitFor(wait)

        @Deprecated("Renamed", ReplaceWith("device()"))
        fun uiDevice() = device()
    }

    private val clearDataRule: ClearDataRule = ClearDataRule()
    private val flakyTestRule: FlakyTestRule = FlakyTestRule().apply {
        allowFlakyAttemptsByDefault(flakyAttempts)
    }
    val activityTestRule: ActivityTestRule<T> = ActivityTestRule(
        activityClass,
        INITIAL_TOUCH_MODE_ENABLED,
        launchActivityAutomatically
    )

    override fun apply(base: Statement, description: Description): Statement {
        UiAutomatorTimeouts.tuneOnce()
        return RuleChain.outerRule(flakyTestRule)
            .around(activityTestRule)
            .around(clearDataRule)
            .apply(base, description)
    }

    /**
     * Launch test activity
     * @param startIntent the intent used to start the activity, or null for a plain launch
     * @param waitLimit how long Espresso waits for the app to go idle, in milliseconds
     */
    fun launchActivity(startIntent: Intent? = null, waitLimit: Long = DEFAULT_IDLING_LIMIT) {
        IdlingPolicies.setMasterPolicyTimeout(waitLimit, TimeUnit.MILLISECONDS)
        IdlingPolicies.setIdlingResourceTimeout(waitLimit, TimeUnit.MILLISECONDS)
        activityTestRule.launchActivity(startIntent)
        device()
    }
}
