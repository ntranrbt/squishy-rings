package com.thevinesh.squishyrings

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val hapticFeedback = IosHapticFeedback()
    val tilt = IosTiltState(sink = { /* consumed via latest() in the frame loop */ })

    return ComposeUIViewController {
        App(
            hapticFeedback = hapticFeedback,
            platformTilt = tilt,
        )
    }
}