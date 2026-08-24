package com.thevinesh.squishyrings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = Unit
            },
        )

        setContent {
            val hapticFeedback = remember(applicationContext) { AndroidHapticFeedback(applicationContext) }
            val tilt = remember { AndroidTiltSource(applicationContext) }
            App(
                hapticFeedback = hapticFeedback,
                platformTilt = tilt,
            )
        }
    }
}