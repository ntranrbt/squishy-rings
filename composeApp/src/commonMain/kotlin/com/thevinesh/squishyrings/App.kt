package com.thevinesh.squishyrings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview

val LocalHapticFeedback = compositionLocalOf<HapticFeedback> { NoOpHapticFeedback }

@Composable
fun App(
    hapticFeedback: HapticFeedback = NoOpHapticFeedback,
    platformTilt: TiltSource? = null,
) {
    CompositionLocalProvider(LocalHapticFeedback provides hapticFeedback) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ToyColors.WaterDeep),
        ) {
            ToyScreen(tilt = platformTilt, haptics = hapticFeedback)
        }
    }
}

@Preview
@Composable
private fun AppPreview() {
    App()
}