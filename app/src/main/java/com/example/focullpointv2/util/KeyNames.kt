package com.example.focullpointv2.util

import android.view.KeyEvent

/** Converts an Android key code into a short, human-friendly label. */
object KeyNames {
    fun label(keyCode: Int): String = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_RIGHT -> "→ Right Arrow"
        KeyEvent.KEYCODE_DPAD_LEFT -> "← Left Arrow"
        KeyEvent.KEYCODE_DPAD_UP -> "↑ Up Arrow"
        KeyEvent.KEYCODE_DPAD_DOWN -> "↓ Down Arrow"
        KeyEvent.KEYCODE_SPACE -> "Space"
        KeyEvent.KEYCODE_ENTER -> "Enter"
        else -> {
            val raw = KeyEvent.keyCodeToString(keyCode)
            raw.removePrefix("KEYCODE_").replace('_', ' ')
        }
    }
}
