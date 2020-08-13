package com.araujo.jordan.kobaia

import android.app.Activity
import android.content.Intent
import androidx.test.espresso.IdlingPolicies
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.*
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions.sleep
import com.schibsted.spain.barista.rule.cleardata.ClearDatabaseRule
import com.schibsted.spain.barista.rule.cleardata.ClearFilesRule
import com.schibsted.spain.barista.rule.cleardata.ClearPreferencesRule
import com.schibsted.spain.barista.rule.flaky.FlakyTestRule
import org.junit.Assert.assertTrue
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class Kobaia<T : Activity>(activityClass: Class<T>) : TestRule {

    companion object {
        private const val DEFAULT_FLAKY_ATTEMPTS = 10
        private const val LAUNCH_ACTIVITY_AUTOMATICALLY = false
        private const val INITIAL_TOUCH_MODE_ENABLED = true

        // According to Forrester Research and Aberdeen Group, consumers abandon a website/app after
        // waiting 3 seconds for a page to load. Adding that to the enter/exit loading of 500ms, we have
        // 4 seconds as out threshold of timeout for a good UX.
        val defaultWaitingTime = 4000L

        var uiDevice: UiDevice? = null

        inline fun <reified T : Activity> create(): Kobaia<T> = Kobaia.create(T::class.java)

        @JvmStatic
        fun <T : Activity> create(activityClass: Class<T>): Kobaia<T> {
            return Kobaia(activityClass)
        }

        /**
         * Get UIObject2 device by Text that appear on screen.
         * This method also wait for it for some milliseconds
         * @param text the text that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 200 milliseconds)
         */
        fun byText(
            text: String,
            wait: Long = defaultWaitingTime
        ) = try {
            uiDevice?.wait(Until.findObjects(By.text(text)), wait)?.firstOrNull()
        } catch (err: Exception) {
            throw NoSuchElementException("Couldn't find $text text")
        }

        /**
         * Get UIObject2 device by Pattern that appear on screen.
         * This method also wait for it for some milliseconds
         * @param pattern the Pattern that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 200 milliseconds)
         */
        fun byText(
            pattern: Pattern,
            wait: Long = defaultWaitingTime
        ) = try {
            uiDevice?.wait(Until.findObjects(By.text(pattern)), wait)?.firstOrNull()
        } catch (err: Exception) {
            throw NoSuchElementException("Couldn't find ${pattern.pattern()} pattern")
        }

        /**
         * Check if Text is visible on screen.
         * This method also wait for it for some milliseconds
         * @param text the text that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 200 milliseconds)
         */
        fun textExists(
            text: String,
            wait: Long = defaultWaitingTime
        ) = try {
            byText(text, wait) != null
        } catch (err: Exception) {
            false
        }


        /**
         * Check if a Pattern is visible on screen.
         * This method also wait for it for some milliseconds
         * @param pattern the pattern that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 200 milliseconds)
         */
        fun textExists(
            pattern: Pattern,
            wait: Long = defaultWaitingTime
        ) = try {
            byText(pattern, wait) != null
        } catch (err: Exception) {
            false
        }

        /**
         * Check if there the given text is content visible on screen.
         * This method also wait for it for some milliseconds
         * @param text the text that you want to search in your screen (could be a text inside another text)
         * @param wait how long you want to wait for it (Default is 200 milliseconds)
         */
        fun containsText(
            text: String,
            wait: Long = defaultWaitingTime
        ) = try {
            !uiDevice?.wait(Until.findObjects(By.textContains(text)), wait).isNullOrEmpty()
        } catch (err: Exception) {
            false
        }


        /**
         * Check if Description is visible on screen. This is useful to search for Images or EditTexts
         * This method also wait for it for some milliseconds
         * @param text the description that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 200 milliseconds)
         */
        fun descriptionExist(
            text: String,
            wait: Long = defaultWaitingTime
        ) = try {
            !uiDevice?.wait(Until.findObjects(By.desc(text)), wait).isNullOrEmpty()
        } catch (err: Exception) {
            false
        }


        /**
         * Check if Description is visible on screen. This is useful to search for Images or EditTexts
         * This method also wait for it for some milliseconds
         * @param pattern the text pattern from the description that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 200 milliseconds)
         */
        fun descriptionExist(
            pattern: Pattern,
            wait: Long = defaultWaitingTime
        ) = try {
            !uiDevice?.wait(Until.findObjects(By.desc(pattern)), wait).isNullOrEmpty()
        } catch (err: Exception) {
            false
        }

        /**
         * Get UIObject2 device by Description that appear on screen. This is useful to search for Images or EditTexts
         * This method also wait for it for some milliseconds
         * @param pattern the text pattern from the description that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 200 milliseconds)
         */
        fun byDescription(
            pattern: Pattern,
            wait: Long = defaultWaitingTime
        ) = try {
            uiDevice?.wait(Until.findObjects(By.desc(pattern)), wait)?.firstOrNull()
        } catch (err: Exception) {
            throw NoSuchElementException("Couldn't find ${pattern.pattern()} pattern")
        }

        /**
         * Get UIObject2 device by Description that appear on screen. This is useful to search for Images or EditTexts
         * This method also wait for it for some milliseconds
         * @param text the description that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 200 milliseconds)
         */
        fun byDescription(
            text: String,
            wait: Long = defaultWaitingTime
        ) = try {
            uiDevice?.wait(Until.findObjects(By.desc(text)), wait)?.firstOrNull()
        } catch (err: Exception) {
            throw NoSuchElementException("Couldn't find $text description")
        }


        /**
         * Click in an UIObject2 by text. This method won't fail your test if this object is not clicked
         * This method also wait for it for some milliseconds
         * @param text the text that you want to be clicked in your screen
         * @param wait how long you want to wait for it (Default is 200 milliseconds)
         */
        fun textClick(
            text: String,
            wait: Long = defaultWaitingTime
        ) = try {
            uiDevice?.wait(Until.findObjects(By.text(text)), wait)?.forEach { it.click() }
        } catch (err: Exception) {
            throw NoSuchElementException("Couldn't find $text text")
        }

        /**
         * Click in an UIObject2 by text. This method won't fail your test if this object is not clicked
         * This method also wait for it for some milliseconds
         * @param text the subtext that is content where you want to be clicked in your screen
         */
        fun containsClick(
            text: String,
            wait: Long = defaultWaitingTime
        ) = try {
            uiDevice?.wait(Until.findObjects(By.textContains(text)), wait)?.forEach { it.click() }
        } catch (err: Exception) {
            throw NoSuchElementException("Couldn't find $text text")
        }

        /**
         * Click in an UIObject2 by pattern. This method won't fail your test if this object is not clicked
         * This method also wait for it for some milliseconds
         * @param pattern the text pattern that you want to be clicked in your screen
         * @param wait how long you want to wait for it (Default is 200 milliseconds)
         */
        fun textClick(
            pattern: Pattern,
            wait: Long = defaultWaitingTime
        ) = try {
            uiDevice?.wait(Until.findObjects(By.text(pattern)), wait)?.forEach { it.click() }
        } catch (err: Exception) {
            throw NoSuchElementException("Couldn't find ${pattern.pattern()} pattern")
        }

        /**
         * Click in an UIObject2 by description. This is useful to search for Images or EditTexts
         * This method won't fail your test if this object is not clicked
         * This method also wait for it for some milliseconds
         * @param text the text that you want to be clicked in your screen
         * @param wait how long you want to wait for it (Default is 200 milliseconds)
         */
        fun descriptionClick(
            text: String,
            wait: Long = defaultWaitingTime
        ) = try {
            uiDevice?.wait(Until.findObjects(By.desc(text)), wait)?.forEach { it.click() }
        } catch (err: Exception) {
            throw NoSuchElementException("Couldn't find $text text")
        }


        /**
         * Click in an UIObject2 by description pattern. This is useful to search for Images or EditTexts
         * This method won't fail your test if this object is not clicked
         * This method also wait for it for some milliseconds
         * @param pattern the text pattern that you want to be clicked in your screen
         * @param wait how long you want to wait for it (Default is 200 milliseconds)
         */
        fun descriptionClick(
            pattern: Pattern,
            wait: Long = defaultWaitingTime
        ) = try {
            uiDevice?.wait(Until.findObjects(By.desc(pattern)), wait)?.forEach { it.click() }
        } catch (err: Exception) {
            throw NoSuchElementException("Couldn't find ${pattern.pattern()} pattern")
        }


        /**
         * Type in the Soft Numeric Keyboard the text
         * This function is useful to test text change listeners or other types of dynamic changes
         * while the user is typing in the screen
         * @param fieldDescription The field description that will be put the text
         * @param text the numeric text that will be included
         * @param wait how long you want to wait for it (Default is 200 milliseconds)
         */
        fun slowingTypeNumberInKeyboard(
            fieldDescription: String,
            text: String,
            wait: Long = defaultWaitingTime
        ) {
            try {
                val field =
                    uiDevice?.wait(Until.findObjects(By.desc(fieldDescription)), wait)
                        ?.firstOrNull()
                field?.click()
                uiDevice?.waitForIdle(wait) //wait for keyboard
                text.forEach {
                    uiDevice?.wait(Until.findObjects(By.text(it.toString())), 50)?.firstOrNull()
                        ?.click()
                }
            } catch (err: Exception) {
                throw NoSuchElementException("Couldn't find $fieldDescription text")
            }
        }

        /**
         * Scroll a Scrollable view until find a text.
         * @param text that want to be find
         * @param maximumScrolls how many times will scroll until give up (Default: 5)
         */
        fun scrollUntilFindText(
            text: String,
            maximumScrolls: Int = 5
        ): UiObject2? {
            repeat(maximumScrolls) {
                UiScrollable(UiSelector().scrollable(true)).scrollIntoView(UiSelector().text(text))
                try {
                    return byText(text)
                } catch (err: Exception) {
                    println("NOT FOUND, ATTEMPT $it")
                }
            }
            return null
        }

        /**
         * Scroll Vertically a RecycleView, ListVIew or ScrollView until find a text.
         * @param pattern pattern that want to be find
         * @param maximumScrolls how many times will scroll until give up (Default: 5)
         */
        fun scrollUntilFindPattern(
            pattern: Pattern,
            maximumScrolls: Int = 5
        ): UiObject2? {
            repeat(maximumScrolls) {
                UiScrollable(UiSelector().scrollable(true)).scrollIntoView(
                    UiSelector().textMatches(
                        pattern.pattern()
                    )
                )
                try {
                    return byText(pattern)
                } catch (err: Exception) {
                    println("NOT FOUND, ATTEMPT $it")
                }
            }
            return null
        }

        /**
         * Scroll Vertically a RecycleView, ListVIew or ScrollView until find a text.
         * This is useful to search for Images or EditTexts
         * @param text that want to be find
         * @param maximumScrolls how many times will scroll until give up (Default: 5)
         */
        fun scrollUntilFindDescription(
            text: String,
            maximumScrolls: Int = 5
        ): UiObject2? {
            repeat(maximumScrolls) {
                UiScrollable(UiSelector().scrollable(true)).scrollIntoView(
                    UiSelector().description(
                        text
                    )
                )
                try {
                    return byDescription(text)
                } catch (err: Exception) {
                    println("NOT FOUND, ATTEMPT $it")
                }
            }
            return null
        }

        /**
         * Scroll Vertically a RecycleView, ListVIew or ScrollView until find a text.
         * This is useful to search for Images or EditTexts
         * @param pattern that want to be find
         * @param maximumScrolls how many times will scroll until give up (Default: 5)
         * @param scrollXStartPosition Start X position of the scroll (Default: 500)
         * @param scrollYStartPosition Start Y position of the scroll (Default: 1500)
         * @param scrollPixels How many pixels will be scrolled (Default: 300)
         */
        fun scrollUntilFindDescription(
            pattern: Pattern,
            maximumScrolls: Int = 5
        ): UiObject2? {
            repeat(maximumScrolls) {
                UiScrollable(UiSelector().scrollable(true)).scrollIntoView(
                    UiSelector().descriptionMatches(
                        pattern.pattern()
                    )
                )
                try {
                    return byDescription(pattern)
                } catch (err: Exception) {
                    println("NOT FOUND, ATTEMPT $it")
                }
            }
            return null
        }

        /**
         * Check if a text is visible on screen.
         * This method also wait for it for some milliseconds
         * @param text the text that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 200 milliseconds)
         */
        fun assertTextExist(
            text: String,
            wait: Long = defaultWaitingTime
        ) = assertTrue(
            "$text should be visible", try {
                textExists(text, wait)
            } catch (err: Exception) {
                false
            }
        )


        /**
         * Check if a text is visible on screen.
         * This method also wait for it for some milliseconds
         * @param text the text that you want to search in your screen
         * @param wait how long you want to wait for it (Default is 200 milliseconds)
         */
        fun assertTextExist(
            text: Pattern,
            wait: Long = defaultWaitingTime
        ) = assertTrue(
            "$text should be visible", try {
                textExists(text, wait)
            } catch (err: Exception) {
                false
            }
        )

        /**
         * Make the app wait using Coroutine
         * @param wait time in milliseconds
         */
        fun waitTest(wait: Long = defaultWaitingTime) = sleep(wait)

    }

    private val clearPreferencesRule: ClearPreferencesRule = ClearPreferencesRule()
    private val clearDatabaseRule: ClearDatabaseRule = ClearDatabaseRule()
    private val clearFilesRule: ClearFilesRule = ClearFilesRule()
    private val flakyTestRule: FlakyTestRule = FlakyTestRule().apply {
        allowFlakyAttemptsByDefault(DEFAULT_FLAKY_ATTEMPTS)
    }
    val activityTestRule: ActivityTestRule<T> = ActivityTestRule(
        activityClass,
        INITIAL_TOUCH_MODE_ENABLED,
        LAUNCH_ACTIVITY_AUTOMATICALLY
    )

    override fun apply(base: Statement, description: Description): Statement {
        return RuleChain.outerRule(flakyTestRule)
            .around(activityTestRule)
            .around(clearPreferencesRule)
            .around(clearDatabaseRule)
            .around(clearFilesRule)
            .apply(base, description)
    }

    fun launchActivity() {
        IdlingPolicies.setMasterPolicyTimeout(5, TimeUnit.SECONDS)
        IdlingPolicies.setIdlingResourceTimeout(5, TimeUnit.SECONDS)
        activityTestRule.launchActivity(null)
        while (uiDevice == null && uiDevice?.isScreenOn == true)
            uiDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    fun launchActivity(startIntent: Intent) {
        IdlingPolicies.setMasterPolicyTimeout(5, TimeUnit.SECONDS)
        IdlingPolicies.setIdlingResourceTimeout(5, TimeUnit.SECONDS)
        activityTestRule.launchActivity(startIntent)
        while (uiDevice == null && uiDevice?.isScreenOn == true) {
            uiDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        }
    }

}