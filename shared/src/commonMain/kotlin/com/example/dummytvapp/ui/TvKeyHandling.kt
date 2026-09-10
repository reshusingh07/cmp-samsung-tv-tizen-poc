package com.example.dummytvapp.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.example.dummytvapp.viewmodel.HomeViewModel
import com.example.dummytvapp.viewmodel.TvDirection

/**
 * Maps a Compose [KeyEvent] to an action on [HomeViewModel].
 *
 * `androidx.compose.ui.input.key.Key`/`KeyEvent`/`onKeyEvent` are common
 * Compose Multiplatform UI APIs -- they are implemented on Android (hardware
 * D-pad / keyboard), iOS (hardware keyboard), tvOS (the Siri Remote: the
 * Compose tvOS fork maps its D-pad to `Key.Direction*`, Select to
 * `Key.DirectionCenter` and Menu to `Key.Back` -- see
 * docs/CMP_TVOS_GUIDE.md), and Web/Wasm (browser `KeyboardEvent`s). That is
 * what lets this single function drive every platform's arrow-key/Enter/Back
 * handling with zero platform-specific code -- see README -> "Why not
 * Modifier.focusable()?" for what still does need a platform split
 * (auto-scrolling + the Tizen-specific Back keycode).
 *
 * On tvOS the `false` returned by `back()` when nothing is selected matters:
 * the fork treats an unconsumed Menu press as "let the system handle it", so
 * Menu on the root screen sends the app to the Apple TV Home screen, exactly
 * as Apple's HIG expects.
 *
 * @return `true` if the event was handled (so the caller should consume it).
 */
internal fun HomeViewModel.handleKeyEvent(event: KeyEvent): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    return when (event.key) {
        Key.DirectionLeft -> { move(TvDirection.Left); true }
        Key.DirectionRight -> { move(TvDirection.Right); true }
        Key.DirectionUp -> { move(TvDirection.Up); true }
        Key.DirectionDown -> { move(TvDirection.Down); true }
        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> { activate(); true }
        Key.Back, Key.Escape -> back()
        else -> false
    }
}
