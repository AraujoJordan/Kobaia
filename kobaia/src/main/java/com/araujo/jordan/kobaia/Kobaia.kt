package com.araujo.jordan.kobaia

import androidx.test.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import junit.framework.Assert.assertTrue
import java.util.regex.Pattern

fun uiDevice() = UiDevice.getInstance(getInstrumentation())

// According to Forrester Research and Aberdeen Group, consumers abandon a website/app after
// waiting 3 seconds for a page to load. Adding that to the enter/exit loading of 500ms, we have
// 4 seconds as out threshold of timeout for a good UX.
var defaultWaitingTime = 4000L

/**
 * Get UIObject2 device by Text that appear on screen.
 * This method also wait for it for some milliseconds
 * @param text the text that you want to search in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.byText(
    text: String,
    wait: Long = defaultWaitingTime
) = wait(Until.findObject(By.text(text)), wait)

/**
 * Get UIObject2 device by Pattern that appear on screen.
 * This method also wait for it for some milliseconds
 * @param pattern the Pattern that you want to search in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.byText(
    pattern: Pattern,
    wait: Long = defaultWaitingTime
) = wait(Until.findObject(By.text(pattern)), wait)

/**
 * Check if Text is visible on screen.
 * This method also wait for it for some milliseconds
 * @param text the text that you want to search in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.textExists(
    text: String,
    wait: Long = defaultWaitingTime
) = try {
    !wait(Until.findObjects(By.text(text)), wait).isNullOrEmpty()
} catch (err: Exception) {
    false
}

/**
 * Check if a Pattern is visible on screen.
 * This method also wait for it for some milliseconds
 * @param pattern the pattern that you want to search in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.textExists(
    pattern: Pattern,
    wait: Long = defaultWaitingTime
) = try {
    !wait(Until.findObjects(By.text(pattern)), wait).isNullOrEmpty()
} catch (err: Exception) {
    false
}

/**
 * Check if there the given text is content visible on screen.
 * This method also wait for it for some milliseconds
 * @param text the text that you want to search in your screen (could be a text inside another text)
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.containsText(
    text: String,
    wait: Long = defaultWaitingTime
) = try {
    !wait(Until.findObjects(By.textContains(text)), wait).isNullOrEmpty()
} catch (err: Exception) {
    false
}

/**
 * Check if Description is visible on screen. This is useful to search for Images or EditTexts
 * This method also wait for it for some milliseconds
 * @param text the description that you want to search in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.descriptionExist(
    text: String,
    wait: Long = defaultWaitingTime
) = try {
    !wait(Until.findObjects(By.desc(text)), wait).isNullOrEmpty()
} catch (err: Exception) {
    false
}

/**
 * Check if Description is visible on screen. This is useful to search for Images or EditTexts
 * This method also wait for it for some milliseconds
 * @param pattern the text pattern from the description that you want to search in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.descriptionExist(
    pattern: Pattern,
    wait: Long = defaultWaitingTime
) = try {
    !wait(Until.findObjects(By.desc(pattern)), wait).isNullOrEmpty()
} catch (err: Exception) {
    false
}

/**
 * Get UIObject2 device by Description that appear on screen. This is useful to search for Images or EditTexts
 * This method also wait for it for some milliseconds
 * @param pattern the text pattern from the description that you want to search in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.byDescription(
    pattern: Pattern,
    wait: Long = defaultWaitingTime
) = wait(Until.findObject(By.desc(pattern)), wait)

/**
 * Get UIObject2 device by Description that appear on screen. This is useful to search for Images or EditTexts
 * This method also wait for it for some milliseconds
 * @param text the description that you want to search in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.byDescription(
    text: String,
    wait: Long = defaultWaitingTime
) = wait(Until.findObject(By.desc(text)), wait)


/**
 * Click in an UIObject2 by text. This method won't fail your test if this object is not clicked
 * This method also wait for it for some milliseconds
 * @param text the text that you want to be clicked in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.textClick(
    text: String,
    wait: Long = defaultWaitingTime
) = try {
    wait(Until.findObject(By.text(text)), wait).click()
} catch (uiNotFound: Exception) {
    uiNotFound.printStackTrace()
}

/**
 * Click in an UIObject2 by pattern. This method won't fail your test if this object is not clicked
 * This method also wait for it for some milliseconds
 * @param pattern the text pattern that you want to be clicked in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.textClick(
    pattern: Pattern,
    wait: Long = defaultWaitingTime
) = try {
    wait(Until.findObject(By.text(pattern)), wait).click()
} catch (uiNotFound: Exception) {
    uiNotFound.printStackTrace()
}


/**
 * Click in an UIObject2 by description. This is useful to search for Images or EditTexts
 * This method won't fail your test if this object is not clicked
 * This method also wait for it for some milliseconds
 * @param text the text that you want to be clicked in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.descriptionClick(
    text: String,
    wait: Long = defaultWaitingTime
) = try {
    wait(Until.findObject(By.desc(text)), wait).click()
} catch (uiNotFound: Exception) {
    uiNotFound.printStackTrace()
}


/**
 * Click in an UIObject2 by description pattern. This is useful to search for Images or EditTexts
 * This method won't fail your test if this object is not clicked
 * This method also wait for it for some milliseconds
 * @param pattern the text pattern that you want to be clicked in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.descriptionClick(
    pattern: Pattern,
    wait: Long = defaultWaitingTime
) = try {
    wait(Until.findObject(By.desc(pattern)), wait).click()
} catch (uiNotFound: Exception) {
    uiNotFound.printStackTrace()
}

/**
 * Type in the Soft Numeric Keyboard the text
 * This function is useful to test text change listeners or other types of dynamic changes
 * while the user is typing in the screen
 * @param fieldDescription The field description that will be put the text
 * @param text the numeric text that will be included
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.slowingTypeNumberInKeyboard(
    fieldDescription: String,
    text: String,
    wait: Long = defaultWaitingTime
) {
    val field = wait(Until.findObject(By.desc(fieldDescription)), wait)
    field.click()
    waitForIdle(wait) //wait for keyboard
    text.forEach { wait(Until.findObject(By.text(it.toString())), 50).click() }
}

/**
 * Scroll Vertically a RecycleView, ListVIew or ScrollView until find a text.
 * @param text that want to be find
 * @param maximumScrolls how many times will scroll until give up (Default: 5)
 * @param scrollXStartPosition Start X position of the scroll (Default: 500)
 * @param scrollYStartPosition Start Y position of the scroll (Default: 1500)
 * @param scrollPixels How many pixels will be scrolled (Default: 300)
 */
fun UiDevice.scrollUntilFindText(
    text: String,
    maximumScrolls: Int = 5,
    scrollXStartPosition: Int = 500,
    scrollYStartPosition: Int = 1500,
    scrollPixels: Int = 300
): UiObject2? {
    var scrollAttempts = 0
    while (!textExists(text, defaultWaitingTime)) {
        swipe(
            scrollXStartPosition,
            scrollYStartPosition,
            scrollXStartPosition,
            scrollYStartPosition - scrollPixels,
            15
        )
        waitForIdle(defaultWaitingTime)
        scrollAttempts++
        if (scrollAttempts >= maximumScrolls) {
            println("Couldn't find $text")
            return null
        }
    }
    return byText(text, defaultWaitingTime)
}

/**
 * Scroll Vertically a RecycleView, ListVIew or ScrollView until find a text.
 * This is useful to search for Images or EditTexts
 * @param text that want to be find
 * @param maximumScrolls how many times will scroll until give up (Default: 5)
 * @param scrollXStartPosition Start X position of the scroll (Default: 500)
 * @param scrollYStartPosition Start Y position of the scroll (Default: 1500)
 * @param scrollPixels How many pixels will be scrolled (Default: 300)
 */
fun UiDevice.scrollUntilFindDescription(
    text: String,
    maximumScrolls: Int = 5,
    scrollXStartPosition: Int = 500,
    scrollYStartPosition: Int = 1500,
    scrollPixels: Int = 300
): UiObject2? {
    var scrollAttempts = 0
    while (!descriptionExist(text, defaultWaitingTime)) {
        swipe(
            scrollXStartPosition,
            scrollYStartPosition,
            scrollXStartPosition,
            scrollYStartPosition - scrollPixels,
            15
        )
        waitForIdle(defaultWaitingTime)
        scrollAttempts++
        if (scrollAttempts >= maximumScrolls) {
            println("Couldn't find $text")
            return null
        }
    }
    return byDescription(text, defaultWaitingTime)
}

/**
 * Check if a text is visible on screen.
 * This method also wait for it for some milliseconds
 * @param text the text that you want to search in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.assertTextExist(
    text: String,
    wait: Long = defaultWaitingTime
) = try {
    assertTrue("$text should be visible", textExists(text, wait))
} catch (uiNotFound: Exception) {
    uiNotFound.printStackTrace()
}

/**
 * Check if a text is visible on screen.
 * This method also wait for it for some milliseconds
 * @param text the text that you want to search in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.assertTextExist(
    text: Pattern,
    wait: Long = defaultWaitingTime
) = try {
    assertTrue("$text should be visible", textExists(text, wait))
} catch (uiNotFound: Exception) {
    uiNotFound.printStackTrace()
}

/**
 *
 */
fun waitTest(wait: Long = defaultWaitingTime) = Thread.sleep(wait)


