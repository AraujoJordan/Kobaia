package com.araujo.jordan.latest

import androidx.test.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

fun uiDevice() = UiDevice.getInstance(getInstrumentation())

/**
 * Get UIObject2 device by Text that appear on screen.
 * This method also wait for it for some milliseconds
 * @param text the text that you want to search in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.byText(
    text: String,
    wait: Long = 200
) = wait(Until.findObject(By.text(text)), wait)

/**
 * Get UIObject2 device by Pattern that appear on screen.
 * This method also wait for it for some milliseconds
 * @param pattern the Pattern that you want to search in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.byText(
    pattern: Pattern,
    wait: Long = 200
) = wait(Until.findObject(By.text(pattern)), wait)

/**
 * Check if Text is visible on screen.
 * This method also wait for it for some milliseconds
 * @param text the text that you want to search in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.textExists(
    text: String,
    wait: Long = 200
) = try {
    wait(Until.findObjects(By.text(text)), wait).size > 0
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
    wait: Long = 200
) = try {
    wait(Until.findObjects(By.text(pattern)), wait).size > 0
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
    wait: Long = 200
) = try {
    wait(Until.findObjects(By.textContains(text)), wait).size > 0
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
    wait: Long = 200
) = try {
    wait(Until.findObjects(By.desc(text)), wait).size > 0
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
    wait: Long = 200
) = try {
    wait(Until.findObjects(By.desc(pattern)), wait).size > 0
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
    wait: Long = 200
) = wait(Until.findObject(By.desc(pattern)), wait)

/**
 * Get UIObject2 device by Description that appear on screen. This is useful to search for Images or EditTexts
 * This method also wait for it for some milliseconds
 * @param text the description that you want to search in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.byDescription(
    text: String,
    wait: Long = 200
) = wait(Until.findObject(By.desc(text)), wait)


/**
 * Click in an UIObject2 by text. This method won't fail your test if this object is not clicked
 * This method also wait for it for some milliseconds
 * @param text the text that you want to be clicked in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.textClick(
    text: String,
    wait: Long = 200
) {
    val obj = wait(Until.findObject(By.text(text)), wait)
    try {
        obj.click()
    } catch (uiNotFound: Exception) {
        uiNotFound.printStackTrace()
    }
}

/**
 * Click in an UIObject2 by pattern. This method won't fail your test if this object is not clicked
 * This method also wait for it for some milliseconds
 * @param pattern the text pattern that you want to be clicked in your screen
 * @param wait how long you want to wait for it (Default is 200 milliseconds)
 */
fun UiDevice.textClick(
    pattern: Pattern,
    wait: Long = 200
) {
    val obj = wait(Until.findObject(By.text(pattern)), wait)
    try {
        obj.click()
    } catch (uiNotFound: Exception) {
        uiNotFound.printStackTrace()
    }
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
    wait: Long = 200
) {
    val obj = wait(Until.findObject(By.desc(text)), wait)
    try {
        obj.click()
    } catch (uiNotFound: Exception) {
        uiNotFound.printStackTrace()
    }
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
    wait: Long = 200
) {
    val obj = wait(Until.findObject(By.desc(pattern)), wait)
    try {
        obj.click()
    } catch (uiNotFound: Exception) {
        uiNotFound.printStackTrace()
    }
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
    wait: Long = 200
) {
    val field = wait(Until.findObject(By.desc(fieldDescription)), wait)
    field.click()
    waitForIdle(1000) //wait for keyboard
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
    while (!textExists(text, 25L)) {
        swipe(
            scrollXStartPosition,
            scrollYStartPosition,
            scrollXStartPosition,
            scrollYStartPosition + scrollPixels,
            15
        )
        waitForIdle(600)
        scrollAttempts++
        if (scrollAttempts >= maximumScrolls) {
            println("Couldn't find $text")
            return null
        }
    }
    return byText(text, 500)
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
    scrollYStartPosition: Int = 1500
): UiObject2? {
    var scrollAttempts = 0
    while (!descriptionExist(text, 25L)) {
        swipe(scrollXStartPosition, scrollYStartPosition, 500, 800, 15)
        waitForIdle(600)
        scrollAttempts++
        if (scrollAttempts >= maximumScrolls) {
            println("Couldn't find $text")
            return null
        }
    }
    return byDescription(text, 500)
}