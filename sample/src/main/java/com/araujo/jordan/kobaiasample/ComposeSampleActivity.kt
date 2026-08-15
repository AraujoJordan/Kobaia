package com.araujo.jordan.kobaiasample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp

/**
 * The Compose counterpart of [KobaiaTestActivity]: one screen of the widgets a test finds awkward
 * — a tagged button, a `TextField`, and a long `LazyColumn` to scroll through.
 *
 * The one thing a Compose app has to do for [Kobaia.findTag] and friends is the `semantics` block
 * below: it publishes every `Modifier.testTag` to the accessibility tree as a resource id, which
 * is what UIAutomator — and therefore Kobaia — can see. Text and content descriptions need no
 * opt-in, they are already there.
 */
class ComposeSampleActivity : ComponentActivity() {

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ComposeSampleScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { testTagsAsResourceId = true }
                )
            }
        }
    }
}

@Composable
private fun ComposeSampleScreen(modifier: Modifier = Modifier) {
    var greeting by remember { mutableStateOf("Tap the button") }
    var typed by remember { mutableStateOf("") }

    Column(modifier.padding(16.dp)) {
        Text(text = greeting, modifier = Modifier.testTag("greeting"))

        Button(
            onClick = { greeting = "Kobaia clicked me!" },
            modifier = Modifier.testTag("greetButton")
        ) {
            Text("CLICK ME!")
        }

        TextField(
            value = typed,
            onValueChange = { typed = it },
            modifier = Modifier.testTag("nameField")
        )

        Text(text = "You typed: $typed", modifier = Modifier.testTag("typedBack"))

        LazyColumn {
            items((1..40).toList()) { item ->
                Text(text = "Item #$item", modifier = Modifier.testTag("item$item"))
            }
        }
    }
}
