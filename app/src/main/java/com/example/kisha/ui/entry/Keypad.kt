package com.example.kisha.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.kisha.R
import com.example.kisha.ui.TestTags

private val rows = listOf(
    listOf(Key.Digit(1), Key.Digit(2), Key.Digit(3)),
    listOf(Key.Digit(4), Key.Digit(5), Key.Digit(6)),
    listOf(Key.Digit(7), Key.Digit(8), Key.Digit(9)),
    listOf(Key.Dot, Key.Digit(0), Key.Backspace),
)

/** EX-1 keypad. Keys are at least 64 dp tall (DP-6) and shape-morph when pressed (UI-7). */
@Composable
fun Keypad(onKey: (Key) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key -> KeypadKey(key, onKey, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun KeypadKey(key: Key, onKey: (Key) -> Unit, modifier: Modifier) {
    val tag = when (key) {
        is Key.Digit -> TestTags.key(key.value)
        Key.Dot -> TestTags.KEY_DOT
        Key.Backspace -> TestTags.KEY_BACKSPACE
    }
    FilledTonalButton(
        onClick = { onKey(key) },
        shapes = ButtonDefaults.shapes(),
        modifier = modifier.heightIn(min = 64.dp).testTag(tag),
    ) {
        when (key) {
            is Key.Digit -> Text("${key.value}", style = MaterialTheme.typography.headlineMedium)
            Key.Dot -> {
                val label = stringResource(R.string.keypad_dot)
                Text(".", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { contentDescription = label })
            }
            Key.Backspace -> Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = stringResource(R.string.keypad_backspace))
        }
    }
}
