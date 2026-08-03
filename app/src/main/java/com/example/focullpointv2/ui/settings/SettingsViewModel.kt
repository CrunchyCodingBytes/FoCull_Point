package com.example.focullpointv2.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focullpointv2.data.SettingsRepository
import com.example.focullpointv2.model.KeyBindings
import com.example.focullpointv2.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A remappable shortcut slot shown in the settings screen. */
enum class BindTarget(val label: String) {
    FAVORITE("Favorite"),
    SKIP("Skip"),
    REJECT("Reject"),
    UNDO("Undo")
}

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    val themeMode: StateFlow<ThemeMode> = repo.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.DARK)

    val keyBindings: StateFlow<KeyBindings> = repo.keyBindings
        .stateIn(viewModelScope, SharingStarted.Eagerly, KeyBindings())

    /** The target currently waiting for a key press, or null when idle. */
    private val _capturing = MutableStateFlow<BindTarget?>(null)
    val capturing: StateFlow<BindTarget?> = _capturing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun startCapture(target: BindTarget) {
        _capturing.value = target
    }

    fun cancelCapture() {
        _capturing.value = null
    }

    fun clearMessage() {
        _message.value = null
    }

    /**
     * Assigns [keyCode] to the target currently being captured. Rejects the key if
     * it is already bound to a different target (conflict validation).
     */
    fun onKeyCaptured(keyCode: Int) {
        val target = _capturing.value ?: return
        val current = keyBindings.value

        val conflictTarget = current.targetFor(keyCode)
        if (conflictTarget != null && conflictTarget != target) {
            _message.value = "That key is already used for ${conflictTarget.label}."
            _capturing.value = null
            return
        }

        val updated = when (target) {
            BindTarget.FAVORITE -> current.copy(favorite = keyCode)
            BindTarget.SKIP -> current.copy(skip = keyCode)
            BindTarget.REJECT -> current.copy(reject = keyCode)
            BindTarget.UNDO -> current.copy(undo = keyCode)
        }
        _capturing.value = null
        viewModelScope.launch { repo.setKeyBindings(updated) }
    }

    private fun KeyBindings.targetFor(keyCode: Int): BindTarget? = when (keyCode) {
        favorite -> BindTarget.FAVORITE
        skip -> BindTarget.SKIP
        reject -> BindTarget.REJECT
        undo -> BindTarget.UNDO
        else -> null
    }
}
