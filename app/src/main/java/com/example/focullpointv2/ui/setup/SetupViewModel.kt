package com.example.focullpointv2.ui.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focullpointv2.data.SettingsRepository
import com.example.focullpointv2.model.ThemeMode
import com.example.focullpointv2.util.StorageAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class SetupMode { QUICK, ADVANCED }

/** Validated selection ready to hand to the culling screen. */
data class StartCullingArgs(
    val sourcePath: String,
    val favoritePath: String,
    val rejectPath: String
)

data class SetupUiState(
    val mode: SetupMode = SetupMode.QUICK,
    val sourceDir: File? = null,
    val favoriteDir: File? = null,
    val rejectDir: File? = null,
    val permissionGranted: Boolean = false,
    val error: String? = null
)

class SetupViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    val themeMode: StateFlow<ThemeMode> = repo.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.DARK)

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun setMode(mode: SetupMode) {
        _uiState.value = _uiState.value.copy(mode = mode, error = null)
    }

    fun setSource(dir: File) {
        _uiState.value = _uiState.value.copy(sourceDir = dir, error = null)
    }

    fun setFavorite(dir: File) {
        _uiState.value = _uiState.value.copy(favoriteDir = dir, error = null)
    }

    fun setReject(dir: File) {
        _uiState.value = _uiState.value.copy(rejectDir = dir, error = null)
    }

    fun toggleTheme() {
        viewModelScope.launch { repo.toggleTheme() }
    }

    fun refreshPermission() {
        _uiState.value = _uiState.value.copy(
            permissionGranted = StorageAccess.hasAllFilesAccess(getApplication())
        )
    }

    /**
     * Validates the current selection, creating the Favorites/Rejects folders for
     * Quick Setup. Returns null and sets an error when the selection is invalid.
     */
    fun buildStartArgs(): StartCullingArgs? {
        val state = _uiState.value
        val source = state.sourceDir
        if (source == null || !source.isDirectory) {
            _uiState.value = state.copy(error = "Please select a valid source folder.")
            return null
        }

        val favorite: File
        val reject: File
        when (state.mode) {
            SetupMode.QUICK -> {
                favorite = File(source, "Favorites")
                reject = File(source, "Rejects")
                if (!favorite.exists()) favorite.mkdirs()
                if (!reject.exists()) reject.mkdirs()
            }
            SetupMode.ADVANCED -> {
                val fav = state.favoriteDir
                val rej = state.rejectDir
                if (fav == null || rej == null) {
                    _uiState.value =
                        state.copy(error = "Please select favorite and reject folders.")
                    return null
                }
                favorite = fav
                reject = rej
                if (!favorite.exists()) favorite.mkdirs()
                if (!reject.exists()) reject.mkdirs()
            }
        }

        return StartCullingArgs(
            sourcePath = source.absolutePath,
            favoritePath = favorite.absolutePath,
            rejectPath = reject.absolutePath
        )
    }
}
