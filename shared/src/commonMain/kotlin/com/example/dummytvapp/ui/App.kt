package com.example.dummytvapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import com.example.dummytvapp.platform.InstallPlatformInputBridge
import com.example.dummytvapp.platform.PlatformBackHandler
import com.example.dummytvapp.platform.rememberPlatformHasInputFocus
import com.example.dummytvapp.viewmodel.HomeViewModel

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
    val rootFocusRequester = remember { FocusRequester() }

    // Android system back gesture/button (see PlatformBackHandler doc comment
    // for exactly why this one platform needs its own hook).
    PlatformBackHandler(enabled = true, onBack = { viewModel.back() })

    // Tizen remote's proprietary Back keycode (10009) - no-op on Android/iOS.
    InstallPlatformInputBridge(onBack = { viewModel.back() })

    // See PlatformFocusBridge doc comment: on Web/Wasm, calling
    // requestFocus() below is not enough by itself -- the actual <canvas>
    // Compose renders into also needs real DOM focus, which the wasmJs
    // actual grabs on mount (no click/tap needed). Always true immediately
    // on Android/iOS, and in practice immediately on Web/Wasm too -- this
    // only stays `false` (showing the hint below) if that DOM lookup ever
    // fails, as a fallback.
    val hasInputFocus by rememberPlatformHasInputFocus()

    // The root key handler must request focus after the platform has granted
    // real input focus; on Web/Wasm requesting it before the canvas itself
    // has DOM focus would be too early.
    LaunchedEffect(hasInputFocus) {
        if (hasInputFocus) {
            rootFocusRequester.requestFocus()
        }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box {
                HomeScreen(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(rootFocusRequester)
                        .focusable()
                        .onKeyEvent { event -> viewModel.handleKeyEvent(event) },
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
