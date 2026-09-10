package com.example.dummytvapp.platform

import androidx.compose.runtime.Composable

/**
 * No-op on iOS and tvOS: there is no equivalent of Tizen's proprietary
 * keyCode 10009. Every Siri Remote button the app cares about (D-pad, Select,
 * Menu) already arrives as a regular Compose `KeyEvent` -- see
 * `PlatformBackHandler.apple.kt`.
 */
@Composable
actual fun InstallPlatformInputBridge(onBack: () -> Unit) {
    // Intentionally empty.
}
