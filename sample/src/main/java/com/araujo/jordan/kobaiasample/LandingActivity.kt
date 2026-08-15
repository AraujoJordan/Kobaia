package com.araujo.jordan.kobaiasample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * Where the tutorial drops you: log in, or sign up and go nowhere.
 */
class LandingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KobaiaSampleTheme {
                LandingScreen(
                    onLogIn = { startActivity(Intent(this, LoginActivity::class.java)) }
                )
            }
        }
    }
}

@Composable
private fun LandingScreen(onLogIn: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.app_name), modifier = Modifier.testTag("landingTitle"))

        Button(onClick = onLogIn, modifier = Modifier.testTag("landingLoginButton")) {
            Text("LOG IN")
        }

        TextButton(onClick = { }, modifier = Modifier.testTag("landingSignUpButton")) {
            Text("SIGN UP")
        }
    }
}
