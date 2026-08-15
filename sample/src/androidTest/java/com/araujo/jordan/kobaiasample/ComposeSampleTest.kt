package com.araujo.jordan.kobaiasample

import android.view.KeyEvent
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.Direction
import com.araujo.jordan.kobaia.Kobaia
import com.araujo.jordan.kobaia.Kobaia.Companion.typeIntoTag
import com.araujo.jordan.kobaia.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The sample's Compose login flow, driven end to end by Compose testTags.
 * Demonstrates finders, assertions, clicks, and typing targeting Modifier.testTag selectors.
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class ComposeSampleTest {

    @Test
    fun testApp() = launch<SplashActivity> {
        assertTagVisible("splashTitle")
        assertVisible("Kobaia")
        clickTag("welcomeSkipButton")

        assertTagVisible("welcomeGetStartedButton")
        clickTag("welcomeGetStartedButton")

        assertTagVisible("landingLoginButton")
        clickTag("landingLoginButton")

        assertTagVisible("loginTitle")
        typeIntoTag("right_email@kobaia.com", tag = "emailField")
        typeIntoTag("12345678", tag = "passwordField")

        clickTag("loginButton")
        assertTagVisible("loggedInText")
        assertVisible("Welcome to Kobaia!")
    }

    /**
     * The widgets of [ComposeSampleActivity], exercised through their testTags: a checkbox set to
     * a known state rather than toggled, buttons whose clickability is asserted, and a button
     * double clicked.
     */
    @Test
    fun testWidgets() = launch<ComposeSampleActivity> {
        assertTagClickable("greetButton")
        // a disabled Compose Button keeps its OnClick semantics, so the tree still calls it
        // clickable — the plain Text is what is genuinely not
        assertTagNotClickable("typedBack")

        checkTag("acceptCheckbox")
        assertTagChecked("acceptCheckbox")
        // checking an already checked box must leave it checked — that is the whole point
        checkTag("acceptCheckbox")
        assertTagChecked("acceptCheckbox")
        uncheckTag("acceptCheckbox")
        assertTagUnchecked("acceptCheckbox")

        doubleClickTag("greetButton")
        assertVisible("Kobaia clicked me!")
    }

    /**
     * The raw swipe turns the lazy list by hand: two swipes up move the first item off the top,
     * and swiping down brings it back. A swipe lifts the finger while it is still moving, so the
     * list flings an unpredictable distance — the way back is a bounded number of tries, not an
     * exact count. [Kobaia.scrollTo] stays the way to *find* an item.
     */
    @Test
    fun swipesTheListByHand() = launch<ComposeSampleActivity> {
        assertTagVisible("item1")
        swipe(Direction.UP)
        swipe(Direction.UP)
        assertTagNotVisible("item1")

        var backAtTheTop = false
        repeat(6) {
            swipe(Direction.DOWN)
            if (Kobaia.isTagVisible("item1", Kobaia.QUICK_WAITING_TIME)) {
                backAtTheTop = true
                return@repeat
            }
        }
        assertTrue("swiping back down should reach the first item again", backAtTheTop)
    }

    /**
     * Pinches and the device helpers. There is nothing on this screen that zooms, so the pinch
     * asserts only that the gesture was delivered — the return is `false` on a miss instead.
     */
    @Test
    fun testGesturesAndDevice() = launch<ComposeSampleActivity> {
        assertTrue("the pinch should be delivered", pinchOutTag("greetButton"))
        assertTrue("the pinch should be delivered", pinchInTag("greetButton"))

        wakeUp()
        assertTrue("pressing a key should work", pressKey(KeyEvent.KEYCODE_ENTER))

        val screenshotFile = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "kobaia-screenshot-test.png"
        )
        assertTrue("the screenshot should be taken", screenshot(screenshotFile))
        assertTrue("the screenshot should be on disk", screenshotFile.exists())
        screenshotFile.delete()

        copyToClipboard("kobaia")
        assertEquals("kobaia", clipboardText())
        copyToClipboard("")
        assertEquals("", clipboardText())
    }
}
