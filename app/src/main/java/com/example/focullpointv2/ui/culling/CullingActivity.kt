package com.example.focullpointv2.ui.culling

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.focullpointv2.ui.theme.FoCullPointTheme
import java.io.File

class CullingActivity : ComponentActivity() {

    private val sourceDir: File by lazy {
        File(intent.getStringExtra(CullingExtras.SOURCE_PATH).orEmpty())
    }
    private val favoriteDir: File by lazy {
        File(intent.getStringExtra(CullingExtras.FAVORITE_PATH).orEmpty())
    }
    private val rejectDir: File by lazy {
        File(intent.getStringExtra(CullingExtras.REJECT_PATH).orEmpty())
    }

    private val viewModel: CullingViewModel by viewModels {
        CullingViewModel.Factory(application, sourceDir, favoriteDir, rejectDir)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (sourceDir.path.isEmpty()) {
            Toast.makeText(this, "Missing folder selection.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val state by viewModel.uiState.collectAsState()
            val keyBindings by viewModel.keyBindings.collectAsState()

            FoCullPointTheme(themeMode = themeMode) {
                CullingScreen(
                    state = state,
                    keyBindings = keyBindings,
                    onAction = { viewModel.onAction(it) },
                    onUndo = { viewModel.undo() },
                    onResolveConflict = { viewModel.resolveConflict(it) },
                    onDismissConflict = { viewModel.dismissConflict() },
                    onClearError = { viewModel.clearError() },
                    onReturnToSetup = { finish() }
                )
            }
        }
    }
}
