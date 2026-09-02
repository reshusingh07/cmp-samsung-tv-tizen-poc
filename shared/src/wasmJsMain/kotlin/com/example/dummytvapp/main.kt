package com.example.dummytvapp

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.example.dummytvapp.ui.App
import kotlinx.browser.document

/**
 * Entry point for the wasmJs (browser) target. `ComposeViewport` renders
 * into the given DOM element (here, the whole `<body>`), sizing itself to
 * fill it -- this is what later runs, unmodified, inside the Tizen Web App
 * wrapper too (see tizen-app/README-TIZEN.md).
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        App()
    }
}
