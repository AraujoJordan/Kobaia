package com.araujo.jordan.kobaiasample

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize

/**
 * The theme of the sample, and the one piece of setup a Compose app owes a UIAutomator test.
 *
 * `testTagsAsResourceId` publishes every `Modifier.testTag` to the accessibility tree as a
 * resource id, which is what UIAutomator — and so Kobaia's `findTag`, `clickTag`, `scrollToTag`
 * — can see. Text and content descriptions are already there and need none of this.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun KobaiaSampleTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true }
        ) {
            content()
        }
    }
}
