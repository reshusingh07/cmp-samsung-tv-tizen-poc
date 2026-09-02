package com.example.dummytvapp

import androidx.compose.ui.window.ComposeUIViewController
import com.example.dummytvapp.ui.App
import platform.UIKit.UIViewController

/**
 * Called from Swift as `Main_iosKt.MainViewController()`.
 *
 * That Swift-visible name comes directly from this file's name
 * (`main.ios.kt` -> `Main_iosKt`), the same convention JetBrains' own
 * `compose-multiplatform-template` uses -- see `iosApp/iosApp/ContentView.swift`,
 * which is copied close to verbatim from that template in this project.
 */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
