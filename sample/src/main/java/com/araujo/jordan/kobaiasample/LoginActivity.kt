package com.araujo.jordan.kobaiasample

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The login form. The fields carry a contentDescription as well as a testTag, so the sample
 * covers both ways of reaching a Compose field from a test.
 */
class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KobaiaSampleTheme { LoginScreen() }
        }
    }
}

@Composable
private fun LoginScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var loggedIn by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (loggedIn) {
            Text(text = "Welcome to Kobaia!", modifier = Modifier.testTag("loggedInText"))
            return@Column
        }

        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.testTag("loginTitle")
                )

                TextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Enter your email") },
                    modifier = Modifier
                        .testTag("emailField")
                        .semantics { contentDescription = "Enter your email" }
                )

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Enter your password") },
                    modifier = Modifier
                        .testTag("passwordField")
                        .semantics { contentDescription = "Enter your password" }
                )

                Button(
                    onClick = {
                        scope.launch {
                            loading = true
                            delay(2000)
                            loading = false
                            if (email == "right_email@kobaia.com" && password == "12345678") {
                                loggedIn = true
                            } else {
                                Toast.makeText(context, "Wrong credentials!", Toast.LENGTH_LONG)
                                    .show()
                            }
                        }
                    },
                    modifier = Modifier
                        .testTag("loginButton")
                        .semantics { contentDescription = "Login Button" }
                ) {
                    Text("ENTER")
                }
            }
        }

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.testTag("loginLoadingCircle"))
        }
    }
}
