package com.example.dummytvapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dummytvapp.platform.InstallPlatformInputBridge
import com.example.dummytvapp.platform.PlatformBackHandler
import com.example.dummytvapp.platform.rememberPlatformHasInputFocus
import com.example.dummytvapp.viewmodel.HomeViewModel
import com.rokufocus.rememberRokuColumnState

/**
 * Entry point shared by all four targets:
 * - Android: called from `MainActivity.setContent { App() }`
 * - iOS and tvOS: called from `ComposeUIViewController { App() }` (see
 *   `MainViewController.kt` in `appleMain`)
 * - Web/Wasm: called from `ComposeViewport(document.body!!) { App() }`
 *
 * A dark, TV-style Material 3 theme is used throughout since this is meant
 * to be watched from a couch, not held in a hand.
 */
@Composable
fun App() {
    val viewModel = remember { HomeViewModel() }

    // The grid's navigation state. Hoisted to here, rather than left inside
    // HomeScreen, only so this function can hand it platform focus below.
    val columnState = rememberRokuColumnState()

    // Android system back gesture/button. Enabled only while there is an
    // overlay to dismiss, so at the root screen back still exits the app
    // instead of being silently swallowed.
    PlatformBackHandler(
        enabled = viewModel.selected != null,
        onBack = { viewModel.back() },
    )

    // Tizen remote's proprietary Back keycode (10009) - no-op on Android/iOS/tvOS.
    InstallPlatformInputBridge(onBack = { viewModel.back() })

    // See PlatformFocusBridge doc comment: on Web/Wasm, requesting focus below
    // is not enough by itself -- the actual <canvas> Compose renders into also
    // needs real DOM focus, which the wasmJs actual grabs on mount (no
    // click/tap needed). Always true immediately on Android/iOS/tvOS.
    val hasInputFocus by rememberPlatformHasInputFocus()

    // Hand platform focus to the grid once the platform has granted the app
    // real input focus. RokuLazyColumn is the focusable node now -- there is no
    // separate root focus target, which would only have competed with it.
    LaunchedEffect(hasInputFocus) {
        if (!hasInputFocus) return@LaunchedEffect
        // One frame, so the column's FocusRequester is attached before it is used.
        withFrameNanos { }
        runCatching { columnState.requestFocus() }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box {
                HomeScreen(
                    viewModel = viewModel,
                    columnState = columnState,
                    modifier = Modifier
                        .fillMaxSize()
                        // Must sit above the column: it previews keys away from
                        // the grid while the selection overlay is up.
                        .overlayKeyGate(viewModel),
                )
                if (!hasInputFocus) {
                    StartHint()
                }
            }
        }
    }
}

/**
 * Shown only while [rememberPlatformHasInputFocus] reports `false` -- i.e.
 * only ever on Web/Wasm, and only until the first click/tap. Without this,
 * a keyboard/remote-only user has no way to know the arrow keys will start
 * working the moment they click or tap anywhere.
 */
@Composable
private fun StartHint(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            text = "Click or tap anywhere once to enable arrow-key navigation",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 48.dp),
        )
    }
}
