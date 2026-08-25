package com.thevinesh.squishyrings

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController() = MainViewController(screenshotScenarioName = null)

fun MainViewController(screenshotScenarioName: String?): UIViewController {
    val hapticFeedback = IosHapticFeedback()
    val tilt = IosTiltSource()

    return ComposeUIViewController {
        App(
            hapticFeedback = hapticFeedback,
            platformTilt = tilt,
        )
    }
}
