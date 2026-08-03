package com.example.focullpointv2.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.focullpointv2.ui.theme.FoCullPointTheme

class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val bindings by viewModel.keyBindings.collectAsState()
            val capturing by viewModel.capturing.collectAsState()
            val message by viewModel.message.collectAsState()

            FoCullPointTheme(themeMode = themeMode) {
                SettingsScreen(
                    bindings = bindings,
                    capturing = capturing,
                    message = message,
                    onBack = { finish() },
                    onStartCapture = { viewModel.startCapture(it) },
                    onCancelCapture = { viewModel.cancelCapture() },
                    onKeyCaptured = { viewModel.onKeyCaptured(it) },
                    onClearMessage = { viewModel.clearMessage() }
                )
            }
        }
    }
}
