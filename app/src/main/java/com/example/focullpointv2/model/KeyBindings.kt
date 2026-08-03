package com.example.focullpointv2.model

import android.view.KeyEvent

/**
 * Remappable physical-keyboard shortcuts for the cull actions plus Undo.
 * Defaults follow the arrow keys: right = Favorite, up = Skip, left = Reject,
 * down = Undo.
 */
data class KeyBindings(
    val favorite: Int = KeyEvent.KEYCODE_DPAD_RIGHT,
    val skip: Int = KeyEvent.KEYCODE_DPAD_UP,
    val reject: Int = KeyEvent.KEYCODE_DPAD_LEFT,
    val undo: Int = KeyEvent.KEYCODE_DPAD_DOWN
) {
    /** Returns the [CullAction] mapped to [keyCode], or null if none. */
    fun actionFor(keyCode: Int): CullAction? = when (keyCode) {
        favorite -> CullAction.FAVORITE
        skip -> CullAction.SKIP
        reject -> CullAction.REJECT
        else -> null
    }

    /** True when [keyCode] is bound to the Undo action. */
    fun isUndo(keyCode: Int): Boolean = keyCode == undo
}
