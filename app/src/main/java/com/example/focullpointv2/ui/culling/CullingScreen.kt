package com.example.focullpointv2.ui.culling

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.focullpointv2.model.ConflictStrategy
import com.example.focullpointv2.model.CullAction
import com.example.focullpointv2.model.KeyBindings
import com.example.focullpointv2.ui.theme.FavoriteGreen
import com.example.focullpointv2.ui.theme.RejectRed
import com.example.focullpointv2.ui.theme.SkipYellow

@Composable
fun CullingScreen(
    state: CullingUiState,
    keyBindings: KeyBindings,
    onAction: (CullAction) -> Unit,
    onUndo: () -> Unit,
    onResolveConflict: (ConflictStrategy) -> Unit,
    onDismissConflict: () -> Unit,
    onClearError: () -> Unit,
    onReturnToSetup: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && state.pendingConflict == null) {
                    val keyCode = event.nativeKeyEvent.keyCode
                    if (keyBindings.isUndo(keyCode)) {
                        if (state.canUndo) onUndo()
                        return@onPreviewKeyEvent true
                    }
                    val action = keyBindings.actionFor(keyCode)
                    if (action != null) {
                        onAction(action)
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
    ) {
        TopBar(state = state, onBack = onReturnToSetup)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.loading -> CircularProgressIndicator()
                state.isEmpty -> EmptyState()
                state.currentItem != null -> {
                    SwipeableCard(
                        item = state.currentItem!!,
                        onAction = onAction,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Preload the next photo so it appears instantly after a swipe.
            state.nextItem?.let { PreloadImage(it) }

            // Undo button in the bottom-right corner.
            if (state.canUndo && !state.isComplete) {
                FloatingActionButton(
                    onClick = onUndo,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo last action"
                    )
                }
            }
        }

        SwipeLegend()
    }

    state.pendingConflict?.let { conflict ->
        val names = conflict.conflictingFiles.joinToString(", ") { it.name }
        AlertDialog(
            onDismissRequest = onDismissConflict,
            title = { Text("File already exists") },
            text = {
                Text("A file already exists at the destination: $names. What would you like to do?")
            },
            confirmButton = {
                TextButton(onClick = { onResolveConflict(ConflictStrategy.RENAME) }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { onResolveConflict(ConflictStrategy.SKIP) }) {
                        Text("Skip")
                    }
                    TextButton(onClick = { onResolveConflict(ConflictStrategy.REPLACE) }) {
                        Text("Replace")
                    }
                }
            }
        )
    }

    state.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onClearError,
            title = { Text("Error") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onClearError) { Text("OK") }
            }
        )
    }

    if (state.isComplete && !state.isEmpty) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("All done!") },
            text = { Text("You've culled every photo. Return to the setup screen?") },
            confirmButton = {
                TextButton(onClick = onReturnToSetup) { Text("Return to setup") }
            }
        )
    }
}

@Composable
private fun TopBar(state: CullingUiState, onBack: () -> Unit) {
    var showBackConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = !state.isComplete) { showBackConfirm = true }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            if (state.isComplete) onBack() else showBackConfirm = true
        }) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = if (state.totalCount == 0) "" else
                "${(state.processedCount + 1).coerceAtMost(state.totalCount)} / ${state.totalCount}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.width(48.dp))
    }

    if (showBackConfirm) {
        AlertDialog(
            onDismissRequest = { showBackConfirm = false },
            title = { Text("Leave culling?") },
            text = { Text("Return to the setup screen? Your progress on remaining photos will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showBackConfirm = false
                    onBack()
                }) { Text("Leave") }
            },
            dismissButton = {
                TextButton(onClick = { showBackConfirm = false }) { Text("Stay") }
            }
        )
    }
}

@Composable
private fun SwipeLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem("← Reject", RejectRed)
        LegendItem("↑ Skip", SkipYellow)
        LegendItem("Favorite →", FavoriteGreen)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Text(text = label, color = color, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun EmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No photos found",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "This folder has no supported photos to cull.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
