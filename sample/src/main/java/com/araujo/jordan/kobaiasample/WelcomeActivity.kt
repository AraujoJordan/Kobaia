package com.araujo.jordan.kobaiasample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The tutorial: SKIP jumps to the last page, GET STARTED moves on to the landing screen.
 */
class WelcomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KobaiaSampleTheme {
                WelcomeScreen(
                    onGetStarted = {
                        startActivity(Intent(this, LandingActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
private fun WelcomeScreen(onGetStarted: () -> Unit) {
    var skipped by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (skipped) "Tutorial Page 3" else "Tutorial Page 1",
            modifier = Modifier.testTag("welcomeTutorialText")
        )

        if (skipped) {
            Button(
                onClick = { scope.launch { delay(500); onGetStarted() } },
                modifier = Modifier.testTag("welcomeGetStartedButton")
            ) {
                Text("GET STARTED")
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { skipped = true },
                    modifier = Modifier.testTag("welcomeSkipButton")
                ) {
                    Text("SKIP")
                }
                Button(
                    onClick = { skipped = true },
                    modifier = Modifier.testTag("welcomeNextButton")
                ) {
                    Text("NEXT")
                }
            }
        }
    }
}
