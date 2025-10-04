package com.keak.petemotions

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

// Keep a single instance of the ComposeUIViewController to prevent
// Koin scope issues when SwiftUI recreates the view
private var _mainViewController: UIViewController? = null

fun MainViewController(): UIViewController {
    if (_mainViewController == null) {
        _mainViewController = ComposeUIViewController {
            App()
        }
    }
    return _mainViewController!!
}