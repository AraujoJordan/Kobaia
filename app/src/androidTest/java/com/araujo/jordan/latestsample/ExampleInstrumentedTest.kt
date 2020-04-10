package com.araujo.jordan.latestsample

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.araujo.jordan.latest.uiDevice

import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        uiDevice().apply {
            pressHome()
            openNotification()
            pressHome()
        }
    }
}
