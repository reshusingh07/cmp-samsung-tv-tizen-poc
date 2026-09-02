package com.example.dummytvapp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

/**
 * See the `expect` declaration for the full story. The real, complete
 * mechanism (confirmed by reading Compose Multiplatform 1.12.0's own source,
 * `ComposeWindow.web.kt`/`ComposeWindowInternal.web.kt`, after black-box
 * testing gave misleading results) has two independent layers:
 *
 * 1. DOM/browser focus: Compose attaches its `keydown`/`keyup` listeners
 *    directly to the `<canvas>` element `ComposeViewport` creates, which is
 *    nested inside a *shadow root* (`body > div > div(shadow host) -> canvas`).
 *    Real DOM keyboard events only reach that listener if the canvas itself
 *    has actual focus -- confirmed via `shadowRoot.activeElement`, since a
 *    non-delegating shadow host reports itself, not its focused descendant,
 *    to `document.activeElement`.
 * 2. Compose's own internal focus tree: even with the canvas genuinely
 *    focused, `onKeyEvent` in `App.kt` only fires once our own
 *    `FocusRequester.requestFocus()` has been called from a live
 *    composition (see `App.kt`'s `LaunchedEffect(hasInputFocus)`).
 *
 * Both layers are ordinary, always-available operations -- neither needs a
 * genuine user gesture. Calling `canvas.focus()` directly, reached through
 * the shadow root, satisfies layer 1 immediately on mount; reporting
 * `true` here immediately (instead of waiting for a click) drives layer 2
 * via `App.kt`'s existing effect. (An earlier version of this function
 * waited for a real click, based on black-box testing that seemed to show
 * DOM focus alone was insufficient -- that testing never actually
 * established layer 2 at the same time, so it wasn't a fair test.)
 *
 * The exact DOM shape is Compose-internal and could change in a future
 * version, so this fails soft: if the canvas can't be found, this falls
 * back to needing a real click/tap, exactly like before.
 */
@Composable
actual fun rememberPlatformHasInputFocus(): State<Boolean> {
    val hasFocus = remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val canvas = findComposeCanvas()
        if (canvas != null) {
            canvas.focus()
            hasFocus.value = true
        }

        // Fallback for if the DOM shape above ever changes: a real click/tap
        // still unlocks things the old way.
        val onFirstPointerInput: (Event) -> Unit = { hasFocus.value = true }
        window.addEventListener("pointerdown", onFirstPointerInput, true)
        window.addEventListener("touchstart", onFirstPointerInput, true)
        onDispose {
            window.removeEventListener("pointerdown", onFirstPointerInput, true)
            window.removeEventListener("touchstart", onFirstPointerInput, true)
        }
    }
    return hasFocus
}

/**
 * Walks `body > positioningContainer > shadowHost -shadow-> canvas`, the DOM
 * shape `ComposeViewport(document.body!!) { ... }` builds as of Compose
 * Multiplatform 1.12.0. Returns `null` (rather than throwing) if any step
 * doesn't match, so a future Compose version change degrades gracefully
 * instead of crashing the app.
 */
private fun findComposeCanvas(): HTMLElement? {
    val positioningContainer = document.body?.firstElementChild ?: return null
    val shadowHost = positioningContainer.firstElementChild ?: return null
    val shadowRoot = shadowHost.shadowRoot ?: return null
    return shadowRoot.querySelector("canvas") as? HTMLElement
}
