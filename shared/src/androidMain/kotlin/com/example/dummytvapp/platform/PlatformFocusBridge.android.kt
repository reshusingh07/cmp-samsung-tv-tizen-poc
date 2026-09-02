package com.example.dummytvapp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Always `true` on Android: `FocusRequester.requestFocus()` already
 * establishes real input focus end-to-end, so there is no gap to report.
 */
@Composable
actual fun rememberPlatformHasInputFocus(): State<Boolean> = remember { mutableStateOf(true) }
