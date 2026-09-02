package com.example.dummytvapp.platform

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Android is the one platform with a real OS-level back channel (the system
 * back gesture / button), delivered outside Compose's normal key-event
 * pipeline. `androidx.activity.compose.BackHandler` is the standard,
 * Android-only API for intercepting it -- this is exactly the kind of thing
 * that belongs in `androidMain`, not `commonMain`.
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
