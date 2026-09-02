package com.example.dummytvapp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.window
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

/**
 * Samsung's Tizen TV remote sends its "Return" (back) button as a raw DOM
 * `keydown` event with `keyCode == 10009`. This is confirmed directly
 * against Samsung's own "Remote Control" developer documentation
 * (developer.samsung.com/smarttv/develop/guides/user-interaction/remote-control.html,
 * checked 2026-08-31 -- see README -> "Verified toolchain versions"), which
 * lists exactly:
 *
 *   ArrowLeft=37, ArrowUp=38, ArrowRight=39, ArrowDown=40, Enter=13, Back=10009
 *
 * The arrow keys and Enter match standard DOM keyCodes that desktop browsers
 * also produce, so Compose Multiplatform's web target already handles them
 * correctly via the common `Key.DirectionUp` etc. mapping used in
 * `TvKeyHandling.kt`. `10009`, however, is Tizen-proprietary: no desktop
 * browser ever sends it, so there was no way to confirm from documentation
 * alone whether Compose's built-in DOM-keyCode -> `Key` translation table
 * forwards it as `Key.Back`. Rather than assume either way, this listens for
 * it directly at the `window` level, independent of Compose's own key
 * handling. This is the one piece of genuinely Tizen-specific input code in
 * the whole project.
 *
 * IMPORTANT - this has NOT been verified end-to-end on a real Tizen device
 * or emulator (see README's Final Verification checklist): what is verified
 * is only that Samsung's docs say the keycode is 10009, and that
 * `kotlinx.browser.window` + `org.w3c.dom.events.KeyboardEvent` are the
 * correct, standard Kotlin/Wasm browser bindings for listening to it.
 */
private const val TIZEN_REMOTE_BACK_KEYCODE = 10009

@Composable
actual fun InstallPlatformInputBridge(onBack: () -> Unit) {
    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = { event ->
            val keyboardEvent = event as? KeyboardEvent
            @Suppress("DEPRECATION") // keyCode is deprecated DOM API, but it's exactly what Tizen's own docs specify.
            if (keyboardEvent != null && keyboardEvent.keyCode == TIZEN_REMOTE_BACK_KEYCODE) {
                onBack()
            }
        }
        window.addEventListener("keydown", listener)
        onDispose { window.removeEventListener("keydown", listener) }
    }
}
