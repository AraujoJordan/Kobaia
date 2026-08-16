package com.araujo.jordan.kobaia

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
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
import java.io.File
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
         * How much of a view a pinch covers by default: from (or to) its centre, halfway to (or
         * from) its edges.
         */
        private const val PINCH_PERCENTAGE = 0.5f

        /**
         * How much of the screen a [swipe] covers by default.
         *
         * Short of edge to edge on purpose: system gestures live at the edges of the screen, and a
         * swipe that starts in the status bar opens the notification shade instead of scrolling.
         */
        private const val SWIPE_PERCENTAGE = 0.75f

        /**
         * How smoothly a [swipe] moves, in the steps UIAutomator divides it into
         */
        private const val SWIPE_STEPS = 50

        /**
         * The label clipboard content is copied under. It is what a clipboard manager shows as the
         * source of the clip, and nothing reads it back.
         */
        private const val CLIPBOARD_LABEL = "kobaia"

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
         * Get the first UiObject2 matching a UIAutomator selector you built yourself.
         *
         * The way out when the three families are not enough: `By.clazz`, `By.checkable`,
         * `By.hasChild`, `By.pkg` and any combination of them are reachable from here without
         * leaving the DSL behind for a bare [device] call.
         *
         * ```kotlin
         * find(By.clazz(android.widget.SeekBar::class.java))
         * click(By.text("OPEN").hasAncestor(By.res("item3")))
         * ```
         * @param selector what the view has to match
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun find(
            selector: BySelector,
            wait: Long = DEFAULT_WAITING_TIME
        ): UiObject2? = findFirst(selector, wait)

        /**
         * Every UiObject2 the selector matches, in the order UIAutomator walks the tree.
         *
         * The finders hand back the first match; this hands back all of them, which is what
         * "the second row" and "how many are there" both need.
         * @param selector what the views have to match
         * @param wait how long you want to wait for the first of them (Default is 5000 milliseconds)
         * @return the matches, or an empty list if none turned up
         */
        fun findAll(
            selector: BySelector,
            wait: Long = DEFAULT_WAITING_TIME
        ): List<UiObject2> = findEvery(selector, wait)

        /**
         * Every UiObject2 with the given text. @see findAll
         * @param text the text the views have to have
         * @param wait how long you want to wait for the first of them (Default is 5000 milliseconds)
         */
        fun findAll(
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): List<UiObject2> = findEvery(By.text(text), wait)

        /**
         * Every UiObject2 with the given Compose testTag (or View resource id). @see findAll
         * @param tag the testTag the views have to have
         * @param wait how long you want to wait for the first of them (Default is 5000 milliseconds)
         */
        fun findAllTags(
            tag: String,
            wait: Long = DEFAULT_WAITING_TIME
        ): List<UiObject2> = findEvery(By.res(tag), wait)

        /**
         * Every view the selector matches, waiting for the first of them to show up.
         *
         * A miss costs the whole wait, like every other finder: [Until.findObjects] comes back as
         * soon as one view matches, so only "none at all" is charged in full.
         * @param selector what the views have to match
         * @param wait how long to wait for the first of them before giving up, in milliseconds
         */
        private fun findEvery(selector: BySelector, wait: Long): List<UiObject2> =
            device().wait(Until.findObjects(scoped(selector)), wait).orEmpty()

        // ---------------------------------------------------------------------------------------
        // Counting
        // ---------------------------------------------------------------------------------------

        /**
         * How many views on screen have this text.
         *
         * ```kotlin
         * assertEquals(3, countOf("OPEN"))
         * ```
         * @param text the text to count
         * @param wait how long you want to wait for the first of them (Default is 5000 milliseconds)
         */
        fun countOf(text: String, wait: Long = DEFAULT_WAITING_TIME): Int =
            findEvery(By.text(text), wait).size

        /**
         * How many views on screen have this content description. @see countOf
         * @param text the content description to count
         * @param wait how long you want to wait for the first of them (Default is 5000 milliseconds)
         */
        fun countOfDescription(text: String, wait: Long = DEFAULT_WAITING_TIME): Int =
            findEvery(By.desc(text), wait).size

        /**
         * How many views on screen have this Compose testTag (or View resource id). @see countOf
         * @param tag the testTag to count
         * @param wait how long you want to wait for the first of them (Default is 5000 milliseconds)
         */
        fun countOfTag(tag: String, wait: Long = DEFAULT_WAITING_TIME): Int =
            findEvery(By.res(tag), wait).size

        /**
         * How many views on screen the selector matches. @see countOf
         * @param selector what the views have to match
         * @param wait how long you want to wait for the first of them (Default is 5000 milliseconds)
         */
        fun countOf(selector: BySelector, wait: Long = DEFAULT_WAITING_TIME): Int =
            findEvery(selector, wait).size

        /**
         * Assert that exactly this many views on screen have this text.
         *
         * Counting nothing costs the whole wait, since there is no first match to come back on —
         * pass a short one when the expected count is zero.
         * @param text the text to count
         * @param expected how many there should be
         * @param wait how long you want to wait for the first of them (Default is 5000 milliseconds)
         */
        fun assertCount(text: String, expected: Int, wait: Long = DEFAULT_WAITING_TIME) {
            val found = countOf(text, if (expected == 0) QUICK_WAITING_TIME else wait)
            if (found != expected) {
                throw AssertionError(
                    "There should be $expected views with the text \"$text\", but there " +
                        "${if (found == 1) "was 1" else "were $found"}." + ScreenReport.explain(text)
                )
            }
        }

        /**
         * Assert that exactly this many views on screen have this Compose testTag.
         * @see assertCount
         * @param tag the testTag to count
         * @param expected how many there should be
         * @param wait how long you want to wait for the first of them (Default is 5000 milliseconds)
         */
        fun assertTagCount(tag: String, expected: Int, wait: Long = DEFAULT_WAITING_TIME) {
            val found = countOfTag(tag, if (expected == 0) QUICK_WAITING_TIME else wait)
            if (found != expected) {
                throw AssertionError(
                    "There should be $expected views tagged $tag, but there " +
                        "${if (found == 1) "was 1" else "were $found"}." + ScreenReport.explain(tag)
                )
            }
        }

        // ---------------------------------------------------------------------------------------
        // Reading text
        // ---------------------------------------------------------------------------------------

        /**
         * The text held by the view with this Compose testTag (or View resource id).
         *
         * What a label *says* rather than whether it is there — a total, a name, a formatted
         * price. Null when nothing matched, like the finders.
         *
         * ```kotlin
         * assertEquals("R$ 12,00", textOfTag("totalLabel"))
         * ```
         * @param tag the testTag of the view to read
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun textOfTag(tag: String, wait: Long = DEFAULT_WAITING_TIME): String? =
            findFirst(By.res(tag), wait)?.safeText()

        /**
         * The text held by the view with this content description. @see textOfTag
         * @param text the content description of the view to read
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun textOfDescription(text: String, wait: Long = DEFAULT_WAITING_TIME): String? =
            findFirst(By.desc(text), wait)?.safeText()

        /**
         * The text held by the first view the selector matches. @see textOfTag
         * @param selector what the view has to match
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun textOf(selector: BySelector, wait: Long = DEFAULT_WAITING_TIME): String? =
            findFirst(selector, wait)?.safeText()

        /**
         * Assert that the view with this Compose testTag holds exactly this text.
         *
         * Fails as "not visible" when nothing carries the tag, so a typo in the tag does not read
         * as a wrong value.
         * @param tag the testTag of the view to read
         * @param expected the text it should hold
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun assertTagTextEquals(tag: String, expected: String, wait: Long = DEFAULT_WAITING_TIME) {
            val actual = requireVisible(By.res(tag), wait, "A view tagged $tag").safeText()
            if (actual != expected) {
                throw AssertionError(
                    "The view tagged $tag should read \"$expected\", but it reads \"$actual\""
                )
            }
        }

        /**
         * Assert that the view with this Compose testTag holds a text containing this one.
         * @see assertTagTextEquals
         * @param tag the testTag of the view to read
         * @param expected the text it should contain
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun assertTagTextContains(tag: String, expected: String, wait: Long = DEFAULT_WAITING_TIME) {
            val actual = requireVisible(By.res(tag), wait, "A view tagged $tag").safeText()
            if (!actual.contains(expected)) {
                throw AssertionError(
                    "The view tagged $tag should contain \"$expected\", but it reads \"$actual\""
                )
            }
        }

        /**
         * The first view the selector matches, waiting for it to show up
         * @param selector what the view has to match
         * @param wait how long to wait for it before giving up, in milliseconds
         */
        private fun findFirst(selector: BySelector, wait: Long): UiObject2? =
            device().wait(Until.findObject(scoped(selector)), wait)

        /**
         * Whether anything matching the selector shows up before the wait runs out.
         * Cheaper than [findFirst]: nothing has to be wrapped in a UiObject2 to answer.
         * @param selector what the view has to match
         * @param wait how long to wait for it before giving up, in milliseconds
         */
        private fun isShowing(selector: BySelector, wait: Long): Boolean =
            device().wait(Until.hasObject(scoped(selector)), wait) == true

        // ---------------------------------------------------------------------------------------
        // Scoping
        // ---------------------------------------------------------------------------------------

        /**
         * The container the current [within] block narrowed the search to, if there is one.
         *
         * Held per thread rather than in a plain field because a test runs on the instrumentation
         * thread while the app runs on its own, and a scope that leaked between them would silently
         * narrow a search some other test made.
         */
        private val currentScope = ThreadLocal<BySelector?>()

        /**
         * A selector narrowed to the container of the enclosing [within] block, or unchanged when
         * there is none. Every finder passes through here, which is what makes the scope apply to
         * all of them at once instead of to a handful that remembered to ask.
         */
        private fun scoped(selector: BySelector): BySelector =
            currentScope.get()?.let { selector.hasAncestor(it) } ?: selector

        /**
         * Narrow every search inside the block to the descendants of one container.
         *
         * A row in a list repeats its labels — every row has its own `OPEN`, and `click("OPEN")`
         * clicks all of them. Naming the row first is what makes one of them addressable:
         *
         * ```kotlin
         * withinTag("item3") {
         *     assertVisible("Kobaia")
         *     click("OPEN")
         * }
         * ```
         *
         * Blocks nest, and the inner container is required to be inside the outer one. Scrolling
         * and swiping narrow too, so a horizontal carousel above a vertical list can be turned by
         * naming it — outside a block they act on the first scrollable on screen, which is
         * whichever one happens to come first in the tree.
         *
         * The container itself is **not** in scope, only what is under it: a scope is a place to
         * look inside, so `withinTag("item3") { assertTagVisible("item3") }` finds nothing.
         *
         * @param tag the testTag (or View resource id) of the container to search inside
         * @param block the interactions to run against that container only
         * @return whatever the block returns
         */
        fun <R> withinTag(tag: String, block: () -> R): R = withinSelector(By.res(tag), block)

        /**
         * Narrow every search inside the block to the descendants of the view with this text.
         * @see withinTag
         * @param text the text of the container to search inside
         * @param block the interactions to run against that container only
         * @return whatever the block returns
         */
        fun <R> within(text: String, block: () -> R): R = withinSelector(By.text(text), block)

        /**
         * Narrow every search inside the block to the descendants of the view with this content
         * description.
         * @see withinTag
         * @param text the content description of the container to search inside
         * @param block the interactions to run against that container only
         * @return whatever the block returns
         */
        fun <R> withinDescription(text: String, block: () -> R): R =
            withinSelector(By.desc(text), block)

        /**
         * Narrow every search inside the block to the descendants of whatever the selector matches.
         * @see withinTag
         * @param container what the container has to match
         * @param block the interactions to run against that container only
         * @return whatever the block returns
         */
        fun <R> within(container: BySelector, block: () -> R): R = withinSelector(container, block)

        /**
         * Run a block with the search narrowed to a container, and put the previous scope back
         * afterwards — including when the block throws, so a failing assertion inside a `within`
         * does not leave every later test in the class searching inside a row that is long gone.
         * @param container what the container has to match
         * @param block what to run inside it
         */
        private fun <R> withinSelector(container: BySelector, block: () -> R): R {
            val enclosing = currentScope.get()
            // Nested blocks compose: the inner container has to be inside the outer one, or
            // `withinTag("item3") { withinTag("row") { … } }` would match a row in any item.
            currentScope.set(enclosing?.let { container.hasAncestor(it) } ?: container)
            return try {
                block()
            } finally {
                currentScope.set(enclosing)
            }
        }

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
         * Check if anything matching a selector you built yourself is visible on screen.
         * @see find
         * @param selector what the view has to match
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun isVisible(
            selector: BySelector,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = isShowing(selector, wait)

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
        ) = assertShowing(By.text(text), wait) { notFound("\"$text\"", wait, text) }

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
        ) = assertShowing(By.text(pattern), wait) { notFound("A text matching $pattern", wait, null) }

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
        ) = assertShowing(By.res(tag), wait) { notFound("A view tagged $tag", wait, tag) }

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
        ) = assertShowing(By.res(pattern), wait) { notFound("A view tagged $pattern", wait, null) }

        /**
         * Assert that a content description is visible on screen, failing the test with a
         * readable message if it is not.
         * @param text the description that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun assertDescriptionVisible(
            text: String,
            wait: Long = DEFAULT_WAITING_TIME
        ) = assertShowing(By.desc(text), wait) { notFound("A view described as $text", wait, text) }

        /**
         * Assert that something matching a selector you built yourself is visible on screen.
         * @see find
         * @param selector what the view has to match
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         */
        fun assertVisible(
            selector: BySelector,
            wait: Long = DEFAULT_WAITING_TIME
        ) = assertShowing(selector, wait) { notFound("A view matching $selector", wait, null) }

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
        ) = assertNotShowing(By.text(text), wait) { stillThere("\"$text\"", wait) }

        /**
         * Assert that a text pattern is **not** on screen. @see assertNotVisible
         * @param pattern the pattern that should not be in your screen
         * @param wait how long to keep looking before concluding it is absent (Default is 50 milliseconds)
         */
        fun assertNotVisible(
            pattern: Pattern,
            wait: Long = QUICK_WAITING_TIME
        ) = assertNotShowing(By.text(pattern), wait) { stillThere("A text matching $pattern", wait) }

        /**
         * Assert that a content description is **not** on screen. @see assertNotVisible
         * @param text the description that should not be in your screen
         * @param wait how long to keep looking before concluding it is absent (Default is 50 milliseconds)
         */
        fun assertDescriptionNotVisible(
            text: String,
            wait: Long = QUICK_WAITING_TIME
        ) = assertNotShowing(By.desc(text), wait) { stillThere("A view described as $text", wait) }

        /**
         * Assert that a Compose testTag (or View resource id) is **not** on screen.
         * @see assertNotVisible
         * @param tag the testTag that should not be in your screen
         * @param wait how long to keep looking before concluding it is absent (Default is 50 milliseconds)
         */
        fun assertTagNotVisible(
            tag: String,
            wait: Long = QUICK_WAITING_TIME
        ) = assertNotShowing(By.res(tag), wait) { stillThere("A view tagged $tag", wait) }

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
            device().wait(Until.gone(scoped(selector)), wait) == true

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
         * Assert that the view with this text is on screen and clickable
         */
        fun assertClickable(text: String, wait: Long = DEFAULT_WAITING_TIME) =
            assertTrue("$text should be clickable", requireVisible(By.text(text), wait, text).isClickable)

        /**
         * Assert that the view with this text is on screen and not clickable
         */
        fun assertNotClickable(text: String, wait: Long = DEFAULT_WAITING_TIME) =
            assertFalse("$text should not be clickable", requireVisible(By.text(text), wait, text).isClickable)

        /**
         * Assert that the view with this Compose testTag is on screen and clickable
         */
        fun assertTagClickable(tag: String, wait: Long = DEFAULT_WAITING_TIME) =
            assertTrue("$tag should be clickable", requireVisible(By.res(tag), wait, tag).isClickable)

        /**
         * Assert that the view with this Compose testTag is on screen and not clickable
         */
        fun assertTagNotClickable(tag: String, wait: Long = DEFAULT_WAITING_TIME) =
            assertFalse("$tag should not be clickable", requireVisible(By.res(tag), wait, tag).isClickable)

        /**
         * Put the view with this text into the checked state, whichever state it is in now —
         * a checkbox, a switch, a radio button. Unlike [click], calling it twice changes nothing.
         * This method won't fail your test if the view is not on screen
         * @param text the text of the view to check
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether the view ended up checked
         */
        fun check(text: String, wait: Long = DEFAULT_WAITING_TIME): Boolean =
            setChecked(By.text(text), true, wait)

        /**
         * Put the view with this content description into the checked state. @see check
         * @param text the description of the view to check
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether the view ended up checked
         */
        fun checkDescription(text: String, wait: Long = DEFAULT_WAITING_TIME): Boolean =
            setChecked(By.desc(text), true, wait)

        /**
         * Put the view with this Compose testTag (or View resource id) into the checked state.
         * @see check
         * @param tag the testTag of the view to check
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether the view ended up checked
         */
        fun checkTag(tag: String, wait: Long = DEFAULT_WAITING_TIME): Boolean =
            setChecked(By.res(tag), true, wait)

        /**
         * Put the view with this text into the unchecked state, whichever state it is in now.
         * This method won't fail your test if the view is not on screen
         * @param text the text of the view to uncheck
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether the view ended up unchecked
         */
        fun uncheck(text: String, wait: Long = DEFAULT_WAITING_TIME): Boolean =
            setChecked(By.text(text), false, wait)

        /**
         * Put the view with this content description into the unchecked state. @see uncheck
         * @param text the description of the view to uncheck
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether the view ended up unchecked
         */
        fun uncheckDescription(text: String, wait: Long = DEFAULT_WAITING_TIME): Boolean =
            setChecked(By.desc(text), false, wait)

        /**
         * Put the view with this Compose testTag (or View resource id) into the unchecked state.
         * @see uncheck
         * @param tag the testTag of the view to uncheck
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether the view ended up unchecked
         */
        fun uncheckTag(tag: String, wait: Long = DEFAULT_WAITING_TIME): Boolean =
            setChecked(By.res(tag), false, wait)

        /**
         * Click the view only when it is not in the wanted state yet, and confirm it arrived.
         *
         * A click toggles, so a test that wants a checkbox checked cannot just click it — on a
         * retry of a flaky test the view may already be checked, and the click would undo it.
         * @param selector what the view has to match
         * @param wanted the state the view should end up in
         * @param wait how long to wait for the view before giving up, in milliseconds
         * @return whether the view ended up in the wanted state
         */
        private fun setChecked(selector: BySelector, wanted: Boolean, wait: Long): Boolean {
            val view = findFirst(selector, wait) ?: return false
            if (view.isChecked != wanted) view.click()
            return view.wait(Until.checked(wanted), FOCUS_WAITING_TIME) == true
        }

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
            findFirst(selector, wait) ?: throw AssertionError(notFound(description, wait, description))

        /**
         * Why a view that had to be there is not, with the screen that was there instead.
         *
         * Built only when something has already failed, never on the way to a passing assertion —
         * reading the screen is not free, and [ScreenReport] is the reason a failure message is
         * worth more than the timeout it cost.
         * @param description how to name the view that is missing
         * @param wait how long it was looked for, in milliseconds
         * @param target the plain text that was searched for, or null when it was a pattern and
         * there is no near miss worth suggesting
         */
        private fun notFound(description: String, wait: Long, target: String?): String =
            "$description should be visible, but was not found after ${wait}ms." +
                ScreenReport.explain(target)

        /**
         * Assert that the selector matches something, describing the screen when it does not.
         *
         * The message is a `by` argument rather than a value so that a passing assertion never
         * builds it — which is the whole reason the screen can be described at all.
         * @param selector what the view has to match
         * @param wait how long to wait for it before giving up, in milliseconds
         * @param message what to fail with, evaluated only on a failure
         */
        private inline fun assertShowing(selector: BySelector, wait: Long, message: () -> String) {
            if (!isShowing(selector, wait)) throw AssertionError(message())
        }

        /**
         * Assert that the selector matches nothing. @see assertNotVisible
         * @param selector what no view may match
         * @param wait how long to keep looking before concluding it is absent, in milliseconds
         * @param message what to fail with, evaluated only on a failure
         */
        private inline fun assertNotShowing(selector: BySelector, wait: Long, message: () -> String) {
            if (isShowing(selector, wait)) throw AssertionError(message())
        }

        /**
         * Why a view that had to be gone is still there.
         *
         * No screen report here: the thing the assertion complained about is on screen, so listing
         * what else is would be noise. What is worth saying is how long it was looked for, since
         * for this family a longer wait is what makes the assertion stricter.
         */
        private fun stillThere(description: String, wait: Long): String =
            "$description should not be visible, but it was still on screen during ${wait}ms."

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
         * Click every view matching a selector you built yourself. This method won't fail your
         * test if nothing is clicked
         * @see find
         * @param selector what the views have to match
         * @param wait how long you want to wait for them (Default is 5000 milliseconds)
         * @return whether anything was clicked at all
         */
        fun click(
            selector: BySelector,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = clickAll(selector, wait)

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

        /**
         * Click every view the selector matches, waiting for them to show up
         * @param selector what the views have to match
         * @param wait how long to wait for them before giving up, in milliseconds
         * @return whether anything was clicked at all
         */
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
            val views = device().wait(Until.findObjects(scoped(selector)), wait).orEmpty()
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

            // The promise this function exists to keep applies to the fallback too. It cannot be
            // an assertion — interactions never throw on a miss — but a field that refused both
            // ways of writing to it is worth a line, because the test will fail somewhere else
            // entirely and this is the only place that knows why.
            if (fieldOnScreen.wait(Until.textEquals(text), QUICK_WAITING_TIME) != true) {
                Log.w(
                    KOBAIA_TAG,
                    "Typed \"$text\" but the field still reads \"${fieldOnScreen.safeText()}\" — " +
                        "it refused both setText and the keyboard"
                )
            }
            return fieldOnScreen
        }

        /**
         * The text of a view, for a log line that must not throw: a view read after it has left
         * the screen raises [StaleObjectException], and a diagnostic is never worth a failure.
         */
        private fun UiObject2.safeText(): String =
            try {
                text.orEmpty()
            } catch (gone: Throwable) {
                "<gone>"
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
         * @param maximumScrolls how many times it will swipe before giving up (Default: 20)
         * @param direction which way to scroll looking for it (Default: Direction.DOWN)
         * @return the text view, or null if it was not reached
         */
        fun scrollTo(
            text: String,
            maximumScrolls: Int = DEFAULT_MAXIMUM_SCROLLS,
            direction: Direction = Direction.DOWN
        ): UiObject2? = scrollUntilFound(maximumScrolls, direction) { find(text, QUICK_WAITING_TIME) }

        /**
         * Scroll the first scrollable view forward (RecyclerView, ListView, ScrollView, …) until a text
         * pattern is visible.
         * @param pattern the pattern that you want to find
         * @param maximumScrolls how many times it will swipe before giving up (Default: 20)
         * @param direction which way to scroll looking for it (Default: Direction.DOWN)
         * @return the text view, or null if it was not reached
         */
        fun scrollTo(
            pattern: Pattern,
            maximumScrolls: Int = DEFAULT_MAXIMUM_SCROLLS,
            direction: Direction = Direction.DOWN
        ): UiObject2? = scrollUntilFound(maximumScrolls, direction) { find(pattern, QUICK_WAITING_TIME) }

        /**
         * Scroll the first scrollable view forward (RecyclerView, ListView, ScrollView, …) until a content
         * description is visible. This is useful to search for Images or EditTexts
         * @param text the description that you want to find
         * @param maximumScrolls how many times it will swipe before giving up (Default: 20)
         * @param direction which way to scroll looking for it (Default: Direction.DOWN)
         * @return the view, or null if it was not reached
         */
        fun scrollToDescription(
            text: String,
            maximumScrolls: Int = DEFAULT_MAXIMUM_SCROLLS,
            direction: Direction = Direction.DOWN
        ): UiObject2? =
            scrollUntilFound(maximumScrolls, direction) { findDescription(text, QUICK_WAITING_TIME) }

        /**
         * Scroll the first scrollable view forward (RecyclerView, ListView, ScrollView, …) until a content
         * description matching the pattern is visible.
         * This is useful to search for Images or EditTexts
         * @param pattern the description pattern that you want to find
         * @param maximumScrolls how many times it will swipe before giving up (Default: 20)
         * @param direction which way to scroll looking for it (Default: Direction.DOWN)
         * @return the view, or null if it was not reached
         */
        fun scrollToDescription(
            pattern: Pattern,
            maximumScrolls: Int = DEFAULT_MAXIMUM_SCROLLS,
            direction: Direction = Direction.DOWN
        ): UiObject2? =
            scrollUntilFound(maximumScrolls, direction) { findDescription(pattern, QUICK_WAITING_TIME) }

        /**
         * Scroll the first scrollable view forward (LazyColumn, RecyclerView, ListView, ScrollView, …)
         * until a Compose testTag (or View resource id) is visible.
         * @param tag the testTag that you want to find
         * @param maximumScrolls how many times it will swipe before giving up (Default: 20)
         * @param direction which way to scroll looking for it (Default: Direction.DOWN)
         * @return the view, or null if it was not reached
         */
        fun scrollToTag(
            tag: String,
            maximumScrolls: Int = DEFAULT_MAXIMUM_SCROLLS,
            direction: Direction = Direction.DOWN
        ): UiObject2? = scrollUntilFound(maximumScrolls, direction) { findTag(tag, QUICK_WAITING_TIME) }

        /**
         * Scroll the first scrollable view forward (LazyColumn, RecyclerView, ListView, ScrollView, …)
         * until a Compose testTag (or View resource id) matching the pattern is visible.
         * @param pattern the testTag pattern that you want to find
         * @param maximumScrolls how many times it will swipe before giving up (Default: 20)
         * @param direction which way to scroll looking for it (Default: Direction.DOWN)
         * @return the view, or null if it was not reached
         */
        fun scrollToTag(
            pattern: Pattern,
            maximumScrolls: Int = DEFAULT_MAXIMUM_SCROLLS,
            direction: Direction = Direction.DOWN
        ): UiObject2? = scrollUntilFound(maximumScrolls, direction) { findTag(pattern, QUICK_WAITING_TIME) }

        /**
         * Swipe the first scrollable view forward until the target turns up, checking before
         * every swipe so that a target already on screen costs none.
         *
         * It searches on from wherever the list currently sits rather than rewinding to the
         * top first, and stops as soon as the list says it cannot scroll any further, so the cost
         * of a miss is bounded by [maximumScrolls] swipes.
         * @param maximumScrolls how many times to swipe before giving up
         * @param direction which way to scroll looking for the target
         * @param find how the target is looked up between swipes
         */
        private fun scrollUntilFound(
            maximumScrolls: Int,
            direction: Direction = Direction.DOWN,
            find: () -> UiObject2?
        ): UiObject2? {
            for (swipe in 1..maximumScrolls.coerceAtLeast(1)) {
                find()?.let { return it }
                val scrollableView =
                    findFirst(By.scrollable(true), DEFAULT_WAITING_TIME) ?: return null
                if (!scrollableView.scroll(direction, SCROLL_PERCENTAGE)) break
            }
            return find()
        }

        // ---------------------------------------------------------------------------------------
        // Gestures
        // ---------------------------------------------------------------------------------------

        /**
         * Swipe across the middle of the first scrollable view — or of the whole screen when
         * there is none — in the given direction.
         *
         * The gesture is aimed at the view because a drag belongs to whatever is under the finger
         * when it lands: a swipe meant for a list but starting on a fixed header above it is
         * delivered to the header, and scrolls nothing at all.
         *
         * This is the raw gesture, for the things a scroll does not reach: pulling a list down to
         * refresh it, turning a pager's page. To *find* something inside a list, use [scrollTo]
         * instead — it checks for the target between swipes rather than going past it.
         *
         * The swipe covers [percent] of the view, short of its edges on purpose: system gestures
         * live at the edges of the screen, and a swipe starting there opens the notification
         * shade instead of scrolling.
         * @param direction which way the finger moves (Direction.UP scrolls the content down)
         * @param percent how much of the view the swipe covers (Default: 0.75)
         * @param steps how smoothly it moves, in the steps UIAutomator divides it into (Default: 50)
         * @return whether the swipe was performed
         */
        fun swipe(
            direction: Direction,
            percent: Float = SWIPE_PERCENTAGE,
            steps: Int = SWIPE_STEPS
        ): Boolean {
            val device = device()
            val area = findFirst(By.scrollable(true), QUICK_WAITING_TIME)?.visibleBounds
                ?: Rect(0, 0, device.displayWidth, device.displayHeight)
            val centerX = area.centerX()
            val centerY = area.centerY()
            val across = (area.width() * percent / 2).toInt()
            val down = (area.height() * percent / 2).toInt()
            return when (direction) {
                Direction.UP ->
                    device.swipe(centerX, centerY + down, centerX, centerY - down, steps)
                Direction.DOWN ->
                    device.swipe(centerX, centerY - down, centerX, centerY + down, steps)
                Direction.LEFT ->
                    device.swipe(centerX + across, centerY, centerX - across, centerY, steps)
                Direction.RIGHT ->
                    device.swipe(centerX - across, centerY, centerX + across, centerY, steps)
            }
        }

        /**
         * Double click every UiObject2 with the given text. This method won't fail your test if
         * nothing is clicked
         * This method also waits for it for some milliseconds
         * @param text the text that you want to be double clicked in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether anything was double clicked at all
         */
        fun doubleClick(text: String, wait: Long = DEFAULT_WAITING_TIME): Boolean =
            doubleClickAll(By.text(text), wait)

        /**
         * Double click every UiObject2 whose text matches the pattern. This method won't fail your
         * test if nothing is clicked
         * @param pattern the text pattern that you want to be double clicked in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether anything was double clicked at all
         */
        fun doubleClick(pattern: Pattern, wait: Long = DEFAULT_WAITING_TIME): Boolean =
            doubleClickAll(By.text(pattern), wait)

        /**
         * Double click every UiObject2 with the given content description. This method won't fail
         * your test if nothing is clicked
         * @param text the description of what you want to be double clicked in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether anything was double clicked at all
         */
        fun doubleClickDescription(text: String, wait: Long = DEFAULT_WAITING_TIME): Boolean =
            doubleClickAll(By.desc(text), wait)

        /**
         * Double click every UiObject2 with the given Compose testTag (or View resource id). This
         * method won't fail your test if nothing is clicked
         * @param tag the testTag of what you want to be double clicked in your screen
         * @param wait how long you want to wait for it (Default is 5000 milliseconds)
         * @return whether anything was double clicked at all
         */
        fun doubleClickTag(tag: String, wait: Long = DEFAULT_WAITING_TIME): Boolean =
            doubleClickAll(By.res(tag), wait)

        /**
         * Double click every view the selector matches: two taps on its centre, close enough
         * together to read as one gesture
         */
        private fun doubleClickAll(selector: BySelector, wait: Long): Boolean =
            actOnAll(selector, wait) { view ->
                val center = view.visibleCenter
                device().click(center.x, center.y)
                device().click(center.x, center.y)
            }

        /**
         * Pinch the first view with this text open, as when zooming into a map or a photo.
         * This method won't fail your test if the view is not on screen
         * @param text the text of the view to pinch
         * @param percent how much of the view the pinch covers (Default: 0.5)
         * @param wait how long you want to wait for the view (Default is 5000 milliseconds)
         * @return whether the pinch was performed
         */
        fun pinchOut(
            text: String,
            percent: Float = PINCH_PERCENTAGE,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = pinch(By.text(text), percent, open = true, wait = wait)

        /**
         * Pinch the first view with this content description open, as when zooming into a map or
         * a photo. @see pinchOut
         * @param text the description of the view to pinch
         * @param percent how much of the view the pinch covers (Default: 0.5)
         * @param wait how long you want to wait for the view (Default is 5000 milliseconds)
         * @return whether the pinch was performed
         */
        fun pinchOutDescription(
            text: String,
            percent: Float = PINCH_PERCENTAGE,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = pinch(By.desc(text), percent, open = true, wait = wait)

        /**
         * Pinch the first view with this Compose testTag (or View resource id) open, as when
         * zooming into a map or a photo. @see pinchOut
         * @param tag the testTag of the view to pinch
         * @param percent how much of the view the pinch covers (Default: 0.5)
         * @param wait how long you want to wait for the view (Default is 5000 milliseconds)
         * @return whether the pinch was performed
         */
        fun pinchOutTag(
            tag: String,
            percent: Float = PINCH_PERCENTAGE,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = pinch(By.res(tag), percent, open = true, wait = wait)

        /**
         * Pinch the first view with this text closed, as when zooming out of a map or a photo.
         * This method won't fail your test if the view is not on screen
         * @param text the text of the view to pinch
         * @param percent how much of the view the pinch covers (Default: 0.5)
         * @param wait how long you want to wait for the view (Default is 5000 milliseconds)
         * @return whether the pinch was performed
         */
        fun pinchIn(
            text: String,
            percent: Float = PINCH_PERCENTAGE,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = pinch(By.text(text), percent, open = false, wait = wait)

        /**
         * Pinch the first view with this content description closed, as when zooming out of a map
         * or a photo. @see pinchIn
         * @param text the description of the view to pinch
         * @param percent how much of the view the pinch covers (Default: 0.5)
         * @param wait how long you want to wait for the view (Default is 5000 milliseconds)
         * @return whether the pinch was performed
         */
        fun pinchInDescription(
            text: String,
            percent: Float = PINCH_PERCENTAGE,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = pinch(By.desc(text), percent, open = false, wait = wait)

        /**
         * Pinch the first view with this Compose testTag (or View resource id) closed, as when
         * zooming out of a map or a photo. @see pinchIn
         * @param tag the testTag of the view to pinch
         * @param percent how much of the view the pinch covers (Default: 0.5)
         * @param wait how long you want to wait for the view (Default is 5000 milliseconds)
         * @return whether the pinch was performed
         */
        fun pinchInTag(
            tag: String,
            percent: Float = PINCH_PERCENTAGE,
            wait: Long = DEFAULT_WAITING_TIME
        ): Boolean = pinch(By.res(tag), percent, open = false, wait = wait)

        /**
         * Pinch the first view the selector matches, in or out. Unlike a click, a pinch is one
         * gesture on one view, so only the first match is pinched — pinching every match would
         * pinch a view that the first one just zoomed out from under the second's coordinates.
         * @return whether the pinch was performed
         */
        private fun pinch(selector: BySelector, percent: Float, open: Boolean, wait: Long): Boolean {
            val view = findFirst(selector, wait) ?: return false
            // UIAutomator reports nothing about how the gesture landed, so "performed" is all the
            // return can honestly promise.
            if (open) view.pinchOpen(percent) else view.pinchClose(percent)
            return true
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
         * Wake the device up, as pressing the power button would. The screen may still be locked;
         * the finders work through the lock screen as through anything else
         */
        fun wakeUp() = device().wakeUp()

        /**
         * Press any key, by its [android.view.KeyEvent] keycode — `KEYCODE_DEL`, `KEYCODE_TAB`,
         * the volume keys. [pressBack], [pressEnter] and friends are this with the keycode filled in
         * @param keyCode the key to press, one of the `KEYCODE_*` constants
         */
        fun pressKey(keyCode: Int): Boolean = device().pressKeyCode(keyCode)

        /**
         * Photograph the screen now, on demand — [FailureScreenshot] does it on a failure, this
         * does it because the test asked.
         *
         * The file has to live somewhere the app under test may write, such as its cache
         * directory; screenshots go missing silently on a path it may not.
         * @param file where the picture goes
         * @return whether the screenshot was taken
         */
        fun screenshot(file: File): Boolean = device().takeScreenshot(file)

        /**
         * Copy a text onto the device clipboard, so a test can paste it — into the field being
         * tested, or into another app.
         * @param text the text to copy
         */
        fun copyToClipboard(text: String) {
            clipboard().setPrimaryClip(ClipData.newPlainText(CLIPBOARD_LABEL, text))
        }

        /**
         * The text currently on the device clipboard, or null when it is empty.
         *
         * Reading the clipboard is something only the foreground app may do since Android 10, and
         * mid-test the foreground app is the one under test — which is whose clipboard this reads.
         */
        fun clipboardText(): String? =
            clipboard().primaryClip
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.text
                ?.toString()

        /**
         * The clipboard of the app under test (not of the instrumentation APK)
         */
        private fun clipboard(): ClipboardManager =
            InstrumentationRegistry.getInstrumentation()
                .targetContext
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

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
