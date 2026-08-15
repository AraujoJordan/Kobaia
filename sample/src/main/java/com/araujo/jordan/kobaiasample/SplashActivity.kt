package com.araujo.jordan.kobaiasample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay

/**
 * The first screen of the sample: a splash that takes its time, so the test has something to
 * wait for without being told to.
 */
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KobaiaSampleTheme {
                SplashScreen(
                    onFinished = {
                        startActivity(Intent(this, WelcomeActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000)
        loading = true
        delay(1000)
        onFinished()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("splashTitle")
        )
        if (loading) {
            Text(text = "Loading...", modifier = Modifier.testTag("splashDescription"))
        }
    }
}
