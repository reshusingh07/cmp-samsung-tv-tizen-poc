package com.example.dummytvapp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Always `true` on iOS and tvOS: `FocusRequester.requestFocus()` already
 * establishes real input focus end-to-end, so there is no gap to report.
 *
 * On tvOS specifically, the Compose fork starts its scene in
 * `InputMode.Keyboard` (there is no touch surface), which is what lets the
 * root `Modifier.focusable()` in `App.kt` accept focus on a cold launch before
 * any Siri Remote press has arrived.
 */
@Composable
actual fun rememberPlatformHasInputFocus(): State<Boolean> = remember { mutableStateOf(true) }
