package com.example.dummytvapp.platform

import androidx.compose.runtime.Composable

/**
 * No-op on Web/Wasm: `Key.Back`/`Key.Escape` from the keyboard already
 * arrive through the common `Modifier.onKeyEvent` handling in `App.kt`. The
 * browser's own Back button navigates the whole page/history stack, which
 * is out of scope for this single-screen POC (there is nowhere to navigate
 * "back" to). The one real Tizen-specific wrinkle -- its remote's
 * proprietary Back keycode -- is handled in `PlatformInputBridge.wasmJs.kt`
 * instead, independent of this function.
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Intentionally empty.
}
