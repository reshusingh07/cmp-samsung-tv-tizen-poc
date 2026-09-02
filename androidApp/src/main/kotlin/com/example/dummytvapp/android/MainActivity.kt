package com.example.dummytvapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.dummytvapp.ui.App

/**
 * The entire Android app is this one class. Everything it shows -- the 25
 * rows, the focus/selection behavior, the key handling -- comes from
 * `com.example.dummytvapp.ui.App()` in the `shared` module. This is the
 * concrete proof that the UI is actually shared, not reimplemented per
 * platform (per the brief's requirement 7 for iOS, mirrored here).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}
