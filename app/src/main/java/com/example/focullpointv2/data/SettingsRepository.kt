package com.example.focullpointv2.data

import android.content.Context
import android.view.KeyEvent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.focullpointv2.model.KeyBindings
import com.example.focullpointv2.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focull_settings")

/**
 * Persists user settings (theme mode + keyboard bindings) via Jetpack DataStore.
 * Defaults: dark true-black theme and arrow-key bindings.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_FAVORITE = intPreferencesKey("key_favorite")
        val KEY_SKIP = intPreferencesKey("key_skip")
        val KEY_REJECT = intPreferencesKey("key_reject")
        val KEY_UNDO = intPreferencesKey("key_undo")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.THEME_MODE]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            else -> ThemeMode.DARK
        }
    }

    val keyBindings: Flow<KeyBindings> = context.dataStore.data.map { prefs ->
        KeyBindings(
            favorite = prefs[Keys.KEY_FAVORITE] ?: KeyEvent.KEYCODE_DPAD_RIGHT,
            skip = prefs[Keys.KEY_SKIP] ?: KeyEvent.KEYCODE_DPAD_UP,
            reject = prefs[Keys.KEY_REJECT] ?: KeyEvent.KEYCODE_DPAD_LEFT,
            undo = prefs[Keys.KEY_UNDO] ?: KeyEvent.KEYCODE_DPAD_DOWN
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun toggleTheme() {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.THEME_MODE]
            prefs[Keys.THEME_MODE] =
                if (current == ThemeMode.LIGHT.name) ThemeMode.DARK.name else ThemeMode.LIGHT.name
        }
    }

    suspend fun setKeyBindings(bindings: KeyBindings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.KEY_FAVORITE] = bindings.favorite
            prefs[Keys.KEY_SKIP] = bindings.skip
            prefs[Keys.KEY_REJECT] = bindings.reject
            prefs[Keys.KEY_UNDO] = bindings.undo
        }
    }
}
