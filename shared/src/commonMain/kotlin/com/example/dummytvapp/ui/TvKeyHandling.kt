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
 * D-pad / keyboard), iOS (hardware keyboard and, per the Compose
 * Multiplatform 1.11 release notes, the tvOS Siri Remote), and Web/Wasm
 * (browser `KeyboardEvent`s). That is what lets this single function drive
 * every platform's arrow-key/Enter/Back handling with zero platform-specific
 * code -- see README -> "Why not Modifier.focusable()?" for what still does
 * need a platform split (auto-scrolling + the Tizen-specific Back keycode).
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
