package com.example.focullpointv2.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.example.focullpointv2.model.KeyBindings
import com.example.focullpointv2.util.KeyNames

@Composable
fun SettingsScreen(
    bindings: KeyBindings,
    capturing: BindTarget?,
    message: String?,
    onBack: () -> Unit,
    onStartCapture: (BindTarget) -> Unit,
    onCancelCapture: () -> Unit,
    onKeyCaptured: (Int) -> Unit,
    onClearMessage: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(capturing) {
        if (capturing != null) {
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (capturing != null && event.type == KeyEventType.KeyUp) {
                    onKeyCaptured(event.nativeKeyEvent.keyCode)
                    true
                } else {
                    false
                }
            }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Keyboard Shortcuts",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(Modifier.height(20.dp))

        BindingRow("Favorite", bindings.favorite) { onStartCapture(BindTarget.FAVORITE) }
        Spacer(Modifier.height(12.dp))
        BindingRow("Skip", bindings.skip) { onStartCapture(BindTarget.SKIP) }
        Spacer(Modifier.height(12.dp))
        BindingRow("Reject", bindings.reject) { onStartCapture(BindTarget.REJECT) }
        Spacer(Modifier.height(12.dp))
        BindingRow("Undo", bindings.undo) { onStartCapture(BindTarget.UNDO) }
    }

    if (capturing != null) {
        AlertDialog(
            onDismissRequest = onCancelCapture,
            title = { Text("Press a key") },
            text = {
                Text("Press the key you want to use for ${capturing.label}.")
            },
            confirmButton = {
                TextButton(onClick = onCancelCapture) { Text("Cancel") }
            }
        )
    }

    if (message != null) {
        AlertDialog(
            onDismissRequest = onClearMessage,
            title = { Text("Key conflict") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onClearMessage) { Text("OK") }
            }
        )
    }
}

@Composable
private fun BindingRow(
    label: String,
    keyCode: Int,
    onRemap: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = KeyNames.label(keyCode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onRemap) {
                Text("Remap")
            }
        }
    }
}

