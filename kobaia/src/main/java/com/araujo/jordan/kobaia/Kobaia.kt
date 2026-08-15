package com.araujo.jordan.kobaia

import android.app.Activity
import android.content.Intent
import androidx.test.espresso.IdlingPolicies
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
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
        const val DEFAULT_MAXIMUM_SCROLLS = 10

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
         * Whether Kobaia replaces UIAutomator's global timeouts with its own, which are far
         * shorter. UIAutomator waits up to 10 seconds for a selector and a second for every
         * swipe, on top of the waiting Kobaia already does itself.
         *
         * Set it to false before your first test if you would rather keep the platform defaults.
         */
        var tuneUiAutomatorTimeouts = true

        private const val INITIAL_TOUCH_MODE_ENABLED = true

        /**
         * How long to wait for the soft keyboard to come up. Deliberately short: a screen with a
         * blinking text cursor never goes idle, so waiting for idle is waiting for the timeout.
         */
        private const val KEYBOARD_WAITING_TIME = 500L

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
         * out, so the five second default would be paid in full every time. Pass a longer one when
         * you are waiting for something to go away rather than checking it never arrived.
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
            device().wait(Until.findObjects(selector), wait)
                .orEmpty()
                .onEach(UiObject2::longClick)
                .isNotEmpty()

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
            device().wait(Until.findObjects(selector), wait)
                .orEmpty()
                .onEach(UiObject2::click)
                .isNotEmpty()

        // ---------------------------------------------------------------------------------------
        // Typing
        // ---------------------------------------------------------------------------------------

        /**
         * Set a text on the field with the given content description, without going through the
         * keyboard. This method won't fail your test if the field is not on screen
         * @param text the text that will be set
         * @param into the description of the field that will receive the text
         * @param wait how long you want to wait for the field (Default is 5000 milliseconds)
         * @return the field that received the text, or null if it never showed up
         */
        fun type(
            text: String,
            into: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): UiObject2? = findDescription(into, wait)?.apply { this.text = text }

        /**
         * Type a text on the field with the given content description by tapping the soft
         * keyboard, one character at a time.
         * This function is useful to test text change listeners or other types of dynamic changes
         * while the user is typing in the screen
         * @param text the text that will be typed (its characters have to be keys of the keyboard)
         * @param into the description of the field that will receive the text
         * @param wait how long you want to wait for the field and the keyboard (Default is 5000 milliseconds)
         */
        fun typeOnKeyboard(
            text: String,
            into: String,
            wait: Long = DEFAULT_WAITING_TIME
        ) = typeCharacterByCharacter(findDescription(into, wait), text, wait)

        /**
         * Tap a field open and then tap out its text on the soft keyboard, one key at a time
         * @param field the field to type into, or null if it never showed up
         * @param text the text to type
         * @param wait how long to wait for the keyboard to come up
         */
        private fun typeCharacterByCharacter(field: UiObject2?, text: String, wait: Long) {
            field?.click()
            device().waitForIdle(KEYBOARD_WAITING_TIME) // wait for keyboard
            text.forEach { character ->
                find(character.toString(), QUICK_WAITING_TIME)?.click()
            }
        }

        /**
         * Set a text on the field with the given Compose testTag (or View resource id), without
         * going through the keyboard. This method won't fail your test if the field is not on
         * screen
         * @param text the text that will be set
         * @param tag the testTag of the field that will receive the text
         * @param wait how long you want to wait for the field (Default is 5000 milliseconds)
         * @return the field that received the text, or null if it never showed up
         */
        fun typeIntoTag(
            text: String,
            tag: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): UiObject2? = findTag(tag, wait)?.apply { this.text = text }

        /**
         * Type a text on the field with the given Compose testTag (or View resource id) by tapping
         * the soft keyboard, one character at a time.
         * This function is useful to test text change listeners or other types of dynamic changes
         * while the user is typing in the screen
         * @param text the text that will be typed (its characters have to be keys of the keyboard)
         * @param tag the testTag of the field that will receive the text
         * @param wait how long you want to wait for the field and the keyboard (Default is 5000 milliseconds)
         */
        fun typeOnKeyboardIntoTag(
            text: String,
            tag: String,
            wait: Long = DEFAULT_WAITING_TIME
        ) = typeCharacterByCharacter(findTag(tag, wait), text, wait)

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
            val scrollableView = UiScrollable(UiSelector().scrollable(true))
            repeat(maximumScrolls.coerceAtLeast(1)) {
                find()?.let { return it }
                val scrolledFurther = try {
                    scrollableView.scrollForward()
                } catch (noScrollableView: UiObjectNotFoundException) {
                    // A screen that is still loading its list has nothing to scroll yet. Like
                    // every other finder, this one reports a miss with null rather than failing.
                    return null
                }
                // The end of the list: one last look at what that final swipe brought into view.
                if (!scrolledFurther) return find()
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
        fun rotateLandscape() = device().setOrientationLandscape()

        /**
         * Turn the device to portrait and hold it there. @see rotateLandscape
         */
        fun rotatePortrait() = device().setOrientationPortrait()

        /**
         * Put the device back the way it was, and let it rotate on its own again
         */
        fun rotateNatural() {
            device().setOrientationNatural()
            device().unfreezeRotation()
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
