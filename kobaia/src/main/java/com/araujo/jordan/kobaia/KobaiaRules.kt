package com.araujo.jordan.kobaia

import android.app.Activity
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.SystemClock
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.ResultsReporter
import androidx.test.uiautomator.UiDevice
import androidx.test.runner.lifecycle.Stage
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

    fun resetRotation() {
        try {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            if (device.isNaturalOrientation) {
                device.unfreezeRotation()
            } else {
                device.setOrientationNatural()
                device.unfreezeRotation()
            }
        } catch (couldNotReset: Throwable) {
            Log.w(KOBAIA_TAG, "Could not reset the display orientation", couldNotReset)
        }
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
                AppUnderTest.resetRotation()
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
            // Before anything is torn down: the screen as the test left it is the whole point.
            FailureScreenshot.capture(label, attemptNumber)
            // Whatever the failed attempt left on screen has to go, otherwise the next attempt
            // starts on top of it instead of on a fresh activity.
            AppUnderTest.finishAllActivities()
        }
    }
    lastError?.let { throw it }
}

/**
 * A picture of the screen as a failing attempt left it.
 *
 * A stack trace says which assertion failed; it does not say what was on screen instead, which for
 * a UI test is most of the answer. The file is registered with UIAutomator's [ResultsReporter], so
 * it is reported back to the instrumentation and shows up alongside the test rather than only on
 * the device, under the app's external media directory.
 */
internal object FailureScreenshot {

    /**
     * Photograph the screen, and never make things worse by trying.
     *
     * @param label the test the screenshot belongs to
     * @param attemptNumber which attempt failed, so the retries do not overwrite each other
     */
    fun capture(label: String, attemptNumber: Int) {
        try {
            val reporter = ResultsReporter(label)
            val screenshot = reporter.addNewFile(
                filename = "${label.asFileName()}-attempt-$attemptNumber.png",
                title = "$label, failed attempt $attemptNumber"
            )
            if (Kobaia.device().takeScreenshot(screenshot)) reporter.reportToInstrumentation()
        } catch (couldNotCapture: Throwable) {
            // Deliberately Throwable, and deliberately swallowed. Reporting needs a mounted
            // external storage, and fails with an Error rather than an Exception when there is
            // none — which must not become the failure the test reports instead of the real one.
            Log.w(KOBAIA_TAG, "Could not capture the screen of the failed $label", couldNotCapture)
        }
    }

    /**
     * A test name as something a file system will accept: `logsIn(com.example.LoginTest)` has
     * brackets in it, and a parameterised test can have anything at all.
     */
    private fun String.asFileName() = replace(UNSAFE_IN_A_FILE_NAME, "_").take(FILE_NAME_LIMIT)

    private val UNSAFE_IN_A_FILE_NAME = Regex("[^A-Za-z0-9.-]")
    private const val FILE_NAME_LIMIT = 100
}

/**
 * The wait UIAutomator does underneath every single thing Kobaia asks it for.
 *
 * Before reading the screen — every poll of every finder, and every click, text read and state
 * check on a view already found — UIAutomator waits for the accessibility event stream to fall
 * quiet for half a second. On a screen that has settled that returns immediately and costs
 * nothing. On a screen that never settles, which is a spinner, a blinking cursor or most Compose
 * screens with something in flight, the events never stop, so it waits out its whole budget and
 * gives up.
 *
 * That budget is [Configurator.setWaitForIdleTimeout], and it defaults to **ten seconds**. Left
 * alone it dwarfs everything Kobaia does: `find(text, wait = 50)` spends ten seconds on a single
 * look, and the `wait` you passed stops meaning anything at all. Lowered to [IDLE_TIMEOUT], a
 * settled screen behaves exactly as before and an animating one stops charging for a stillness
 * that is never coming.
 *
 * Applied once, before the first test, unless [Kobaia.tuneUiAutomatorTimeouts] says otherwise.
 */
internal object UiAutomatorTimeouts {

    private var tuned = false

    fun tuneOnce(idleTimeout: Long = 500L) {
        if (tuned || !Kobaia.tuneUiAutomatorTimeouts) return
        tuned = true
        Configurator.getInstance().waitForIdleTimeout = idleTimeout
    }
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
 * Make the test wait without holding up the app under test.
 *
 * The wait happens on the instrumentation thread, so the app keeps drawing and processing
 * throughout — Kobaia drives it from outside its process. This deliberately does not route
 * through Espresso: waiting for the main thread to go idle never returns on a screen that
 * animates continuously, which a Compose screen with a spinner or a blinking cursor does.
 */
internal object KobaiaSleep {

    /**
     * Hold the test for as long as asked, and no longer
     * @param millis how long to wait for, in milliseconds
     */
    fun sleep(millis: Long) = SystemClock.sleep(millis)
}
