package com.araujo.jordan.kobaia

import android.app.Activity
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.hamcrest.Matcher
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File
import java.util.EnumSet

/**
 * What Kobaia does to the app under test around a test, so every test starts from a clean slate.
 * Both entry points — the [Kobaia] rule and [launch] — go through here.
 */
internal object AppUnderTest {

    /**
     * The context of the app under test (not the context of the instrumentation APK)
     */
    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Wipe every piece of state the app under test can leak into the next test
     */
    fun clearData() {
        clearPreferences()
        clearDatabases()
        clearFiles()
    }

    /**
     * Finish every activity of the app under test that has not been destroyed yet, and wait for
     * them to actually go away.
     *
     * `finish()` only asks: without the wait, the next attempt of a flaky test starts while the
     * screen the failed one left behind is still on its way out, and lands on top of it.
     */
    fun finishAllActivities() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            runningActivities().filterNot { it.isFinishing }.forEach { it.finish() }
        }

        val giveUpAt = SystemClock.uptimeMillis() + TEARDOWN_TIMEOUT
        while (SystemClock.uptimeMillis() < giveUpAt) {
            var stillRunning = true
            instrumentation.runOnMainSync { stillRunning = runningActivities().isNotEmpty() }
            if (!stillRunning) return
            Thread.sleep(TEARDOWN_POLLING_INTERVAL)
        }
    }

    /**
     * The activities of the app under test that have been created and not destroyed yet.
     * Only safe to call from the main thread, which is what the lifecycle monitor requires.
     */
    private fun runningActivities(): List<Activity> {
        val activityLifecycleMonitor = ActivityLifecycleMonitorRegistry.getInstance()
        return EnumSet.range(Stage.CREATED, Stage.STOPPED)
            .flatMap { activityLifecycleMonitor.getActivitiesInStage(it) }
    }

    /**
     * Clear every SharedPreferences file of the app under test.
     *
     * The values are cleared through the SharedPreferences API rather than by deleting the backing
     * file, so that any instance the app is already holding stops serving the old values from
     * memory.
     */
    private fun clearPreferences() {
        File(targetContext.applicationInfo.dataDir, PREFERENCES_DIRECTORY)
            .list()
            .orEmpty()
            .filter { it.endsWith(PREFERENCES_EXTENSION) }
            .forEach { fileName ->
                targetContext
                    .getSharedPreferences(
                        fileName.removeSuffix(PREFERENCES_EXTENSION),
                        Context.MODE_PRIVATE
                    )
                    .edit()
                    .clear()
                    .commit()
            }
    }

    /**
     * Empty every table of every database of the app under test.
     *
     * The rows are deleted but the databases themselves are kept, so an open connection or an
     * already created schema stays valid for the app while the test runs.
     */
    private fun clearDatabases() {
        targetContext.databaseList()
            .map { targetContext.getDatabasePath(it) }
            .filter { it.exists() }
            .forEach { databaseFile ->
                SQLiteDatabase.openDatabase(
                    databaseFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READWRITE
                ).use { database ->
                    tableNamesOf(database).forEach { database.delete(it, null, null) }
                }
            }
    }

    /**
     * The tables of a database, leaving out the ones SQLite and Android maintain themselves,
     * which are not test state and may reject a delete.
     */
    private fun tableNamesOf(database: SQLiteDatabase): List<String> {
        val tableNames = mutableListOf<String>()
        database.rawQuery(SELECT_TABLE_NAMES, arrayOf(TABLE_TYPE)).use { cursor ->
            while (cursor.moveToNext()) tableNames.add(cursor.getString(0))
        }
        return tableNames.filterNot { it.startsWith(INTERNAL_TABLE_PREFIX) || it == METADATA_TABLE }
    }

    /**
     * Delete every file the app under test wrote to its files and cache directories.
     * The directories themselves are left in place, only their contents go.
     */
    private fun clearFiles() {
        listOf(targetContext.filesDir, targetContext.cacheDir).forEach { directory ->
            directory.walkTopDown().filter { it.isFile }.forEach { it.delete() }
        }
    }

    private const val TEARDOWN_TIMEOUT = 5000L
    private const val TEARDOWN_POLLING_INTERVAL = 50L
    private const val PREFERENCES_DIRECTORY = "shared_prefs"
    private const val PREFERENCES_EXTENSION = ".xml"
    private const val SELECT_TABLE_NAMES = "SELECT name FROM sqlite_master WHERE type = ?"
    private const val TABLE_TYPE = "table"
    private const val INTERNAL_TABLE_PREFIX = "sqlite_"
    private const val METADATA_TABLE = "android_metadata"
}

/**
 * Wipe the state of the app under test before and after the test body runs.
 */
internal class ClearDataRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                AppUnderTest.clearData()
                base.evaluate()
                // Deliberately not in a finally block: when a test fails, the state it left
                // behind survives so it can be inspected.
                AppUnderTest.clearData()
            }
        }
}

/**
 * Run something until it passes, to keep an occasional flaky failure from breaking the build.
 * It is only reported as failed once every attempt has been used up, and the failure that is
 * reported is the one from the last attempt.
 *
 * A retry that nobody sees is how a flaky test goes unnoticed, so every failed attempt is logged
 * under the [KOBAIA_TAG] tag. A test that was skipped rather than failed — `assumeTrue` and
 * friends — is not retried at all: the assumption will not become true on the second try.
 *
 * @param label what is being run, for the log
 * @param attempts the maximum number of attempts (anything below 1 counts as a single run)
 * @param attempt what to run
 */
internal fun retryOnFailure(label: String, attempts: Int, attempt: () -> Unit) {
    val maximumAttempts = attempts.coerceAtLeast(1)
    var lastError: Throwable? = null
    repeat(maximumAttempts) { previousAttempts ->
        try {
            attempt()
            return
        } catch (skipped: AssumptionViolatedException) {
            throw skipped
        } catch (error: Throwable) {
            lastError = error
            val attemptNumber = previousAttempts + 1
            val outcome = if (attemptNumber < maximumAttempts) "retrying" else "giving up"
            Log.w(KOBAIA_TAG, "$label failed on attempt $attemptNumber of $maximumAttempts, $outcome", error)
            // Whatever the failed attempt left on screen has to go, otherwise the next attempt
            // starts on top of it instead of on a fresh activity.
            AppUnderTest.finishAllActivities()
        }
    }
    lastError?.let { throw it }
}

/**
 * The logcat tag Kobaia reports under
 */
internal const val KOBAIA_TAG = "Kobaia"

/**
 * Re-run a test that fails. @see retryOnFailure
 */
internal class FlakyTestRule : TestRule {

    private var defaultAttempts = 1

    /**
     * Set how many times a test may run before it is reported as failed
     * @param attempts the maximum number of attempts (anything below 1 counts as a single run)
     */
    fun allowFlakyAttemptsByDefault(attempts: Int) = apply {
        defaultAttempts = attempts.coerceAtLeast(1)
    }

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() =
                retryOnFailure(description.displayName, defaultAttempts) { base.evaluate() }
        }
}

/**
 * Make the test wait without holding up the main thread, so the app keeps drawing and
 * processing while the wait is on.
 */
internal object KobaiaSleep {

    /**
     * Wait for the app to go idle and then keep the main thread looping for a while
     * @param millis how long to wait for, in milliseconds
     */
    fun sleep(millis: Long) {
        onView(isRoot()).perform(sleepViewAction(millis))
    }

    private fun sleepViewAction(millis: Long) = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isRoot()

        override fun getDescription() = "Wait for at least $millis millis"

        override fun perform(uiController: UiController, view: View) {
            uiController.loopMainThreadUntilIdle()
            uiController.loopMainThreadForAtLeast(millis)
        }
    }
}
