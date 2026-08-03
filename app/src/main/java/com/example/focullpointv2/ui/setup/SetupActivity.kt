package com.example.focullpointv2.ui.setup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.focullpointv2.ui.culling.CullingActivity
import com.example.focullpointv2.ui.culling.CullingExtras
import com.example.focullpointv2.ui.settings.SettingsActivity
import com.example.focullpointv2.ui.theme.FoCullPointTheme
import com.example.focullpointv2.util.StorageAccess

class SetupActivity : ComponentActivity() {

    private val viewModel: SetupViewModel by viewModels()

    private val sourcePicker = registerFolderPicker { viewModel.setSource(it) }
    private val favoritePicker = registerFolderPicker { viewModel.setFavorite(it) }
    private val rejectPicker = registerFolderPicker { viewModel.setReject(it) }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.refreshPermission()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val state by viewModel.uiState.collectAsState()

            FoCullPointTheme(themeMode = themeMode) {
                SetupScreen(
                    state = state,
                    themeMode = themeMode,
                    onToggleTheme = { viewModel.toggleTheme() },
                    onOpenSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    onSetMode = { viewModel.setMode(it) },
                    onPickSource = { launchPicker(sourcePicker) },
                    onPickFavorite = { launchPicker(favoritePicker) },
                    onPickReject = { launchPicker(rejectPicker) },
                    onGrantPermission = {
                        permissionLauncher.launch(StorageAccess.requestAccessIntent(this))
                    },
                    onStartCulling = ::startCulling
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermission()
    }

    private fun startCulling() {
        val args = viewModel.buildStartArgs() ?: return
        val intent = Intent(this, CullingActivity::class.java).apply {
            putExtra(CullingExtras.SOURCE_PATH, args.sourcePath)
            putExtra(CullingExtras.FAVORITE_PATH, args.favoritePath)
            putExtra(CullingExtras.REJECT_PATH, args.rejectPath)
        }
        startActivity(intent)
    }

    private fun launchPicker(
        launcher: androidx.activity.result.ActivityResultLauncher<Uri?>
    ) {
        launcher.launch(null)
    }

    private fun registerFolderPicker(onResolved: (java.io.File) -> Unit) =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            val dir = StorageAccess.resolveTreeUriToFile(uri)
            if (dir != null && dir.isDirectory) {
                onResolved(dir)
            } else {
                Toast.makeText(
                    this,
                    "Couldn't resolve that folder. Please pick a folder on device storage.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
}
