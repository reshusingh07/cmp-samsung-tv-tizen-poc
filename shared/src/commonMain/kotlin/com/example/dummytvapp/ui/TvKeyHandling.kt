package com.example.dummytvapp.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.example.dummytvapp.viewmodel.HomeViewModel

/**
 * Swallows D-pad input while the selection overlay is up, and dismisses the
 * overlay on BACK/OK.
 *
 * This is the only key handling the app itself does. Everything else -- D-pad
 * movement between rows and cards, key-repeat throttling, ENTER/OK -- belongs to
 * `RokuLazyColumn`, which installs its own `onPreviewKeyEvent` (see
 * `RokuColumnKeyHandler`).
 *
 * It must be `onPreviewKeyEvent` on an ancestor of the column: preview handlers
 * run root-downwards, so returning `true` here stops the event before the
 * column's own handler ever sees it. That is what keeps the grid from scrolling
 * behind a modal overlay -- the same guard the old `HomeViewModel.move()` did
 * with `if (selected != null) return`, moved to where the keys now arrive.
 *
 * When no overlay is showing this returns `false` for *every* key, including
 * BACK. That is deliberate: an unconsumed BACK is what lets tvOS suspend the app
 * from the root screen (see [HomeViewModel.back]).
 */
internal fun Modifier.overlayKeyGate(viewModel: HomeViewModel): Modifier =
    onPreviewKeyEvent { event ->
        if (viewModel.selected == null) return@onPreviewKeyEvent false

        if (event.type == KeyEventType.KeyDown) {
            when (event.key) {
                Key.Back, Key.Escape, Key.Enter, Key.NumPadEnter, Key.DirectionCenter ->
                    viewModel.back()

                else -> Unit
            }
        }
        // Consume everything else too, so the grid underneath stays put.
        true
    }
