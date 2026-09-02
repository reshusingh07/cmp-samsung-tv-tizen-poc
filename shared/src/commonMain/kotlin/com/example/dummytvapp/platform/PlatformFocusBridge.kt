package com.example.dummytvapp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * Whether the platform has actually granted this app real keyboard/remote
 * input focus yet -- as opposed to Compose's own internal
 * `FocusRequester.requestFocus()`, which can report success without the
 * platform ever delivering a single key event.
 *
 * This exists for one specific, verified case: on the Web/Wasm target,
 * `FocusRequester.requestFocus()` (called from `App.kt`) only moves
 * Compose's own internal notion of focus. It does NOT make the browser
 * start routing real `KeyboardEvent`s to the Compose canvas -- that
 * separately requires the actual `<canvas>` element Compose renders into
 * to have real DOM focus. See `PlatformFocusBridge.wasmJs.kt` for exactly
 * how that canvas is found and focused (no click/tap needed -- an earlier
 * version of this function required one, based on black-box testing that
 * turned out to be misleading; see that file's doc comment for the full
 * story).
 *
 * Android and iOS have their own native focus systems where
 * `FocusRequester.requestFocus()` already works end-to-end with no such
 * gap, so their `actual` implementations report focus as already present.
 *
 * IMPORTANT (see docs/CMP_SAMSUNG_TV_GUIDE.md): this whole mechanism has
 * only been verified in desktop Chrome. Whether Tizen's embedded WebKit
 * runtime builds the same DOM shape has NOT been verified on a real device
 * or emulator.
 */
@Composable
expect fun rememberPlatformHasInputFocus(): State<Boolean>
