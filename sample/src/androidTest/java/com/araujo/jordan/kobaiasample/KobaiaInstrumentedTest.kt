package com.araujo.jordan.kobaiasample

import androidx.test.espresso.IdlingPolicies
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.ActivityTestRule
import com.araujo.jordan.kobaia.*

import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.TimeUnit

/**
 * Simple Kobaia test example
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@LargeTest
@RunWith(AndroidJUnit4ClassRunner::class)
class KobaiaInstrumentedTest {

    @get:Rule
    val activityRule = ActivityTestRule(KobaiaTestActivity::class.java)

    @Before
    fun registerIdlingResource() {
        IdlingPolicies.setMasterPolicyTimeout(3, TimeUnit.SECONDS)
        IdlingPolicies.setIdlingResourceTimeout(1, TimeUnit.SECONDS)
    }


    @Test
    fun testApp() {
        uiDevice().apply {
            textClick("CLICK ME!")
            descriptionClick("fluffy")
            textClick("YOU CAN CLICK ME!", 15000)
            slowingTypeNumberInKeyboard("editField", "133.37")
            pressBack()
            pressHome()
            waitTest(15000)
            textClick("Kobaia")
            scrollUntilFindText("SCROLL TO CLICK ME!")
            assertTextExist("SCROLL TO CLICK ME!")
        }
    }
}
