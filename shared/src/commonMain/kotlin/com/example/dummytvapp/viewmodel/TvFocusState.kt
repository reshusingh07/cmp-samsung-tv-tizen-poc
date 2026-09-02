package com.example.dummytvapp.viewmodel

/**
 * Where the "TV cursor" currently sits on the home screen.
 *
 * This is intentionally a plain, framework-free data class. We do NOT rely on
 * Compose's built-in 2D focus traversal (`Modifier.focusable()` +
 * `FocusManager.moveFocus(...)`) to move between a [LazyColumn] of
 * independently-scrolling [LazyRow]s, because Compose's default focus search
 * has no concept of "the grid of rows has different lengths per row" and its
 * behavior differs across Android/iOS/Web in exactly the situations a TV UI
 * cares about most (see README -> "Why not Modifier.focusable()?"). Instead,
 * [rowIndex]/[columnIndex] are moved explicitly by [HomeViewModel], and each
 * card simply reads whether its own (row, column) matches this state.
 */
data class TvFocusState(
    val rowIndex: Int = 0,
    val columnIndex: Int = 0,
)

/** The four directional moves a TV remote (or arrow keys) can make. */
enum class TvDirection {
    Up, Down, Left, Right
}
