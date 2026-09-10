package com.example.dummytvapp.platform

import androidx.compose.runtime.Composable

/**
 * No-op on iOS and tvOS: neither has a system-level back button/gesture
 * channel the way Android has one.
 *
 * - iOS: `Key.Escape` from a hardware keyboard already arrives through the
 *   common `Modifier.onKeyEvent` handling in `App.kt`.
 * - tvOS: the Siri Remote's Menu button is delivered by the Compose tvOS fork
 *   as a `Key.Back` key event (it maps `UIPressTypeMenu` to `Key.Menu` and
 *   then rewrites it to `Key.Back` before dispatch), so it too reaches the
 *   common `onKeyEvent` handling. When that handler returns `false` (nothing
 *   to dismiss), the fork forwards the unconsumed press up the UIKit responder
 *   chain, and tvOS suspends the app -- the standard Apple TV "Menu at the root
 *   screen goes Home" behaviour. See docs/CMP_TVOS_GUIDE.md.
 *
 * So this hook simply has nothing extra to do on either platform.
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Intentionally empty.
}
