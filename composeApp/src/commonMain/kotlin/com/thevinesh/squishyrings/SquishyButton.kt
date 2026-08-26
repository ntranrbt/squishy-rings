package com.thevinesh.squishyrings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The big purple squishy dome. Squashes fast on press, wobbles back on release.
 * [onSquish] fires on press-down, [onPop] on release (spring-back always pops).
 */
@Composable
fun SquishyButton(
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    onSquish: () -> Unit = {},
    onPop: () -> Unit = {},
) {
    var pressed by remember { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(pressed) {
        scale.animateTo(
            targetValue = if (pressed) 0.82f else 1f,
            animationSpec = if (pressed) {
                tween(70, easing = FastOutSlowInEasing)
            } else {
                spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)
            },
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = 1f + (1f - scale.value) * 0.35f
                scaleY = scale.value
            }
            .clip(CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    onSquish()
                    // hold until the finger lifts
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.all { !it.pressed }) break
                    }
                    pressed = false
                    onPop()
                }
            },
    ) {
        SquishyDome()
    }
}

@Composable
private fun SquishyDome() {
    Canvas(Modifier.fillMaxSize()) {
        val c = center
        val maxR = size.minDimension / 2f
        val ink = maxR * 0.07f
        // concentric ridges, each filled then inked, like a chunky rubber toy button
        val ridges = listOf(
            0.90f to ToyColors.SquishOuter,
            0.64f to ToyColors.SquishMid,
            0.36f to ToyColors.SquishInner,
            0.12f to ToyColors.SquishCore,
        )
        ridges.forEach { (scale, color) ->
            val r = maxR * scale
            drawCircle(color = color, radius = r)
            drawCircle(color = ToyColors.Outline, radius = r, style = Stroke(width = ink))
        }
        // gloss dot, top-left
        drawCircle(
            color = ToyColors.Gloss,
            center = c + Offset(-maxR * 0.38f, -maxR * 0.42f),
            radius = maxR * 0.15f,
        )
    }
}

@Preview
@Composable
private fun SquishyButtonPreview() {
    Box(
        modifier = Modifier
            .background(ToyColors.WaterMid)
            .size(240.dp),
    ) {
        SquishyButton(modifier = Modifier.align(Alignment.Center))
    }
}