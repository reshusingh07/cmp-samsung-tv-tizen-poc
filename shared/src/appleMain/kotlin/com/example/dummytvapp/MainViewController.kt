package com.example.dummytvapp

import androidx.compose.ui.window.ComposeUIViewController
import com.example.dummytvapp.ui.App
import platform.UIKit.UIViewController

/**
 * Called from Swift as `MainViewControllerKt.MainViewController()` by both
 * `iosApp/iosApp/ContentView.swift` and `tvosApp/tvosApp/ContentView.swift`.
 *
 * That Swift-visible name comes directly from this file's name
 * (`MainViewController.kt` -> `MainViewControllerKt`), the same convention
 * JetBrains' Kotlin Multiplatform wizard uses.
 *
 * This lives in `appleMain` rather than `iosMain` because it is byte-for-byte
 * the same on tvOS: `ComposeUIViewController` exists for both UIKit platforms
 * (on tvOS it is provided by the `dev.sajidali.*` Compose fork -- see
 * docs/CMP_TVOS_GUIDE.md), and the returned `UIViewController` is hosted the
 * same way on both.
 */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
