package com.example.focullpointv2.model

/** The action a user takes while culling a photo. */
enum class CullAction {
    FAVORITE,
    REJECT,
    SKIP
}

/** UI theme preference. Default is true-black dark for OLED screens. */
enum class ThemeMode {
    DARK,
    LIGHT
}

/** How to resolve a destination file-name conflict during a move. */
enum class ConflictStrategy {
    SKIP,
    REPLACE,
    RENAME
}
