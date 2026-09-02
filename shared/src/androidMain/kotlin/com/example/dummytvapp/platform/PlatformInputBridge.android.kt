package com.example.dummytvapp.platform

import androidx.compose.runtime.Composable

/**
 * No-op on Android: D-pad/remote input on Android TV already arrives as
 * ordinary `androidx.compose.ui.input.key.Key.DirectionUp` etc. events
 * through the common `Modifier.onKeyEvent` handling in `App.kt`. There is no
 * Android equivalent of Tizen's proprietary keyCode 10009.
 */
@Composable
actual fun InstallPlatformInputBridge(onBack: () -> Unit) {
    // Intentionally empty.
}
