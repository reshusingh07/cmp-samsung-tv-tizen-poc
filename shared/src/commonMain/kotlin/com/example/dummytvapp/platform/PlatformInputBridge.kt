package com.example.dummytvapp.platform

import androidx.compose.runtime.Composable

/**
 * Extension point for input that cannot be captured through Compose's common
 * `Modifier.onKeyEvent` at all, on some platform.
 *
 * Concretely, this exists for one specific, verified case: Samsung's Tizen TV
 * remote sends its "Return" (back) button as a raw DOM `keydown` event with
 * `keyCode == 10009` (confirmed against Samsung's own "Remote Control"
 * developer documentation -- see README -> "Verified toolchain versions").
 * That is a Tizen-proprietary code that a desktop browser never produces, so
 * we cannot be sure the Compose Multiplatform web target's built-in
 * DOM-keyCode -> `androidx.compose.ui.input.key.Key` translation table maps
 * it to anything meaningful (we have not been able to confirm this either
 * way without a real Tizen device/emulator -- see the Final Verification
 * checklist in README.md). The wasmJs `actual` implementation below
 * defensively listens for it directly and forwards it as a back-press,
 * independent of whatever Compose's own key mapping does.
 *
 * Android and iOS have no equivalent proprietary input channel, so their
 * `actual` implementations are no-ops.
 *
 * @param onBack invoked when a platform-specific "back" signal not already
 *   covered by common `onKeyEvent` handling is observed.
 */
@Composable
expect fun InstallPlatformInputBridge(onBack: () -> Unit)
