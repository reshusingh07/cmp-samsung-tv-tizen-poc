package com.example.dummytvapp.platform

import androidx.compose.runtime.Composable

/**
 * Hooks into whatever OS-level "back" channel a platform has, in addition to
 * the common `Key.Back` / `Key.Escape` handling already wired up in
 * [com.example.dummytvapp.ui.App] via `Modifier.onKeyEvent`.
 *
 * This genuinely needs an expect/actual split (per the brief's "create an
 * architecture where platform-specific input handling can be implemented
 * separately if necessary"):
 *
 * - **Android** has a system back gesture/button that is delivered outside
 *   the normal Compose key-event pipeline (`androidx.activity.compose.BackHandler`).
 * - **iOS** has no hardware/system back button at all.
 * - **Web/Wasm** has no OS-level back channel either (browser Back navigates
 *   the whole page, which is out of scope for a single-screen POC); Tizen's
 *   remote "Return" key is a DOM keyboard event, so it is already covered by
 *   the common `onKeyEvent` handling, not this function -- see
 *   [PlatformInputBridge] for the one Tizen-specific wrinkle that IS handled
 *   here-adjacent.
 *
 * @param enabled whether this handler should currently intercept back at all.
 * @param onBack invoked when the platform-specific back channel fires.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
