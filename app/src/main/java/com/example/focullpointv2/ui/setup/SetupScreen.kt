package com.example.focullpointv2.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.focullpointv2.model.ThemeMode
import java.io.File

@Composable
fun SetupScreen(
    state: SetupUiState,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onSetMode: (SetupMode) -> Unit,
    onPickSource: () -> Unit,
    onPickFavorite: () -> Unit,
    onPickReject: () -> Unit,
    onGrantPermission: () -> Unit,
    onStartCulling: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FoCull Point",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggleTheme) {
                Icon(
                    imageVector = if (themeMode == ThemeMode.DARK) Icons.Filled.LightMode
                    else Icons.Filled.DarkMode,
                    contentDescription = "Toggle theme",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Filled.Keyboard,
                    contentDescription = "Keyboard shortcuts",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        if (!state.permissionGranted) {
            PermissionCard(onGrantPermission)
            Spacer(Modifier.height(20.dp))
        }

        // Mode selector
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = state.mode == SetupMode.QUICK,
                onClick = { onSetMode(SetupMode.QUICK) },
                label = { Text("Quick Setup") }
            )
            FilterChip(
                selected = state.mode == SetupMode.ADVANCED,
                onClick = { onSetMode(SetupMode.ADVANCED) },
                label = { Text("Advanced Setup") }
            )
        }

        Spacer(Modifier.height(20.dp))

        when (state.mode) {
            SetupMode.QUICK -> {
                FolderRow(
                    label = "Source folder",
                    dir = state.sourceDir,
                    onPick = onPickSource
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "\"Favorites\" and \"Rejects\" folders will be created inside " +
                        "the source folder (reused if they already exist).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SetupMode.ADVANCED -> {
                FolderRow(
                    label = "Source folder",
                    dir = state.sourceDir,
                    onPick = onPickSource
                )
                Spacer(Modifier.height(12.dp))
                FolderRow(
                    label = "Favorite folder",
                    dir = state.favoriteDir,
                    onPick = onPickFavorite
                )
                Spacer(Modifier.height(12.dp))
                FolderRow(
                    label = "Reject folder",
                    dir = state.rejectDir,
                    onPick = onPickReject
                )
            }
        }

        state.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onStartCulling,
            enabled = state.permissionGranted && state.sourceDir != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Culling")
        }
    }
}

@Composable
private fun PermissionCard(onGrantPermission: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Storage access required",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "FoCull Point needs All-Files Access to read and move your photos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onGrantPermission) {
                Text("Grant access")
            }
        }
    }
}

@Composable
private fun FolderRow(
    label: String,
    dir: File?,
    onPick: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick = onPick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null)
            Spacer(Modifier.height(0.dp))
            Text(
                text = dir?.absolutePath ?: "Tap to select…",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f, fill = false)
            )
        }
    }
}
