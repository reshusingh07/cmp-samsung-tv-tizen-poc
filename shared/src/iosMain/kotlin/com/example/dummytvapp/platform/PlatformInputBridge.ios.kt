package com.example.dummytvapp.platform

import androidx.compose.runtime.Composable

/** No-op on iOS: there is no equivalent of Tizen's proprietary keyCode 10009. */
@Composable
actual fun InstallPlatformInputBridge(onBack: () -> Unit) {
    // Intentionally empty.
}
