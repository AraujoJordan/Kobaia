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
