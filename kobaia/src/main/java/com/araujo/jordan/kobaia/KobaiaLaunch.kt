package com.araujo.jordan.kobaia

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingPolicies
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * The activity under test while a [launch] block runs, and every Kobaia interaction with it.
 *
 * It is the receiver of the block, so the interactions need no import and no name of their own:
 *
 * ```kotlin
 * @Test
 * fun testApp() = launch<SplashActivity> {
 *     assertVisible("Kobaia")
 *     click("SKIP")
 *     type("12345678") into "Enter your password"
 * }
 * ```
 *
 * @property scenario the activity under test, for the things the interactions do not cover —
 * `scenario.onActivity { … }`, `scenario.recreate()`, `scenario.moveToState(…)`
 */
class KobaiaScope<T : Activity> internal constructor(
    val scenario: ActivityScenario<T>
) : KobaiaInteractions

/**
 * Run a test on a freshly launched activity, without declaring a rule:
 *
 * ```kotlin
 * @Test
 * fun testApp() = launch<SplashActivity> {
 *     assertVisible("Kobaia")
 *     click("SKIP")
 * }
 * ```
 *
 * It does everything the [Kobaia] rule does — it clears the app's shared preferences, databases
 * and files before the test, launches the activity, retries a failing test, and clears the state
 * again once the test passes. A test that fails every attempt leaves its state behind, so it can
 * be inspected.
 *
 * @param startIntent the intent used to start the activity, or null for a plain launch
 * @param flakyAttempts how many times a failing test is retried before it is reported as failed
 * @param waitLimit how long Espresso waits for the app to go idle, in milliseconds
 * @param test what the test does with the activity, with a [KobaiaScope] as its receiver
 */
inline fun <reified T : Activity> launch(
    startIntent: Intent? = null,
    flakyAttempts: Int = Kobaia.DEFAULT_FLAKY_ATTEMPTS,
    waitLimit: Long = Kobaia.DEFAULT_IDLING_LIMIT,
    noinline test: KobaiaScope<T>.() -> Unit
) = launch(T::class.java, startIntent, flakyAttempts, waitLimit, test)

/**
 * Run a test on a freshly launched activity, without declaring a rule:
 *
 * ```kotlin
 * @Test
 * fun testApp() = SplashActivity::class.launch {
 *     assertVisible("Kobaia")
 *     click("SKIP")
 * }
 * ```
 *
 * @see launch
 */
fun <T : Activity> KClass<T>.launch(
    startIntent: Intent? = null,
    flakyAttempts: Int = Kobaia.DEFAULT_FLAKY_ATTEMPTS,
    waitLimit: Long = Kobaia.DEFAULT_IDLING_LIMIT,
    test: KobaiaScope<T>.() -> Unit
) = launch(java, startIntent, flakyAttempts, waitLimit, test)

/**
 * Run a test on a freshly launched activity, without declaring a rule.
 * The overloads that take the activity as a type argument or as a `KClass` read better; this one
 * is for when you only have the `Class` at hand.
 *
 * @see launch
 */
fun <T : Activity> launch(
    activityClass: Class<T>,
    startIntent: Intent? = null,
    flakyAttempts: Int = Kobaia.DEFAULT_FLAKY_ATTEMPTS,
    waitLimit: Long = Kobaia.DEFAULT_IDLING_LIMIT,
    test: KobaiaScope<T>.() -> Unit
) {
    UiAutomatorTimeouts.tuneOnce()
    IdlingPolicies.setMasterPolicyTimeout(waitLimit, TimeUnit.MILLISECONDS)
    IdlingPolicies.setIdlingResourceTimeout(waitLimit, TimeUnit.MILLISECONDS)

    retryOnFailure(activityClass.simpleName, flakyAttempts) {
        AppUnderTest.finishAllActivities()
        AppUnderTest.clearData()
        AppUnderTest.resetRotation()
        scenarioOf(activityClass, startIntent).use { KobaiaScope(it).test() }
    }

    // Only once the test has passed, and outside the retrying: a state that fails to clear is not
    // a flaky test, and a test that failed every attempt keeps its state so it can be inspected.
    AppUnderTest.finishAllActivities()
    AppUnderTest.clearData()
}

/**
 * Launch the activity under test, with the given intent when there is one
 */
private fun <T : Activity> scenarioOf(
    activityClass: Class<T>,
    startIntent: Intent?
): ActivityScenario<T> =
    if (startIntent == null) ActivityScenario.launch(activityClass)
    else ActivityScenario.launch(startIntent)
