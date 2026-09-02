package com.example.dummytvapp.platform

import androidx.compose.runtime.Composable

/**
 * No-op on iOS: there is no system-level back button/gesture channel the
 * way Android has one. `Key.Escape` from a hardware keyboard, or (per the
 * Compose Multiplatform 1.11 release notes) the tvOS Siri Remote's Menu
 * button, already arrive through the common `Modifier.onKeyEvent` handling
 * in `App.kt` -- this hook simply has nothing extra to do here.
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Intentionally empty.
}
