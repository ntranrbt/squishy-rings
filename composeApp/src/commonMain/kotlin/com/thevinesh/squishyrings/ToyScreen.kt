package com.thevinesh.squishyrings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * The whole toy: fluid chamber with floating rings, squishy button, gyro tilt.
 *
 * Sim space is 1:1 with canvas pixels. The simulation is (re)created on the first
 * size change, stepped in the frame loop, and observed via [frame] so the Canvas
 * redraws every frame.
 */
@Composable
fun ToyScreen(
    tilt: TiltSource? = null,
    haptics: HapticFeedback = NoOpHapticFeedback,
) {
    val density = LocalDensity.current
    var sim by remember { mutableStateOf<RingSimulation?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val tiltState = remember { TiltState() }
    val frame = remember { mutableIntStateOf(0) }
    val pendingImpulse = remember { ArrayDeque<Impulse>() }
    val tuning = Tuning.Default
    val buttonPaddingDp = 24.dp
    val buttonSizeDp = 150.dp

    val buttonCenterSim = remember(canvasSize) {
        with(density) {
            Vec2(
                x = canvasSize.width / 2f,
                y = canvasSize.height - (buttonPaddingDp + buttonSizeDp / 2).toPx().toFloat(),
            )
        }
    }

    DisposableEffect(tilt) {
        tilt?.start()
        onDispose { tilt?.stop() }
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    canvasSize = it
                    sim = RingSimulation.create(it.width.toFloat(), it.height.toFloat())
                },
        ) {
            val f = frame.value
            val s = sim ?: return@Canvas
            drawWater(size.width, size.height, f)
            s.rings.forEach { ring -> drawRing(ring) }
            drawChamberFrame(size.width, size.height, density.density)
        }

        SquishyButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = buttonPaddingDp),
            onSquish = {
                haptics.squish()
                pendingImpulse.addLast(
                    Impulse(
                        cx = buttonCenterSim.x,
                        cy = buttonCenterSim.y,
                        strength = tuning.squishStrength,
                        falloff = tuning.squishFalloff,
                    ),
                )
            },
            onPop = {
                haptics.pop()
                pendingImpulse.addLast(
                    Impulse(
                        cx = buttonCenterSim.x,
                        cy = buttonCenterSim.y,
                        strength = tuning.popStrength,
                        falloff = tuning.squishFalloff,
                    ),
                )
            },
        )
    }

    LaunchedEffect(tilt) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { nanos ->
                val dt = if (lastNanos == 0L) {
                    1f / 60f
                } else {
                    ((nanos - lastNanos) / 1e9f).coerceAtMost(1f / 20f)
                }
                tilt?.let { tiltState.update(it.latest(), dt) }
                sim?.let { s ->
                    val impulse = pendingImpulse.removeFirstOrNull()
                    s.step(dt, tiltState.current, impulse)
                }
                frame.value++
                lastNanos = nanos
            }
        }
    }
}

private fun DrawScope.drawWater(width: Float, height: Float, frame: Int) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(ToyColors.WaterLight, ToyColors.WaterMid, ToyColors.WaterDeep),
        ),
    )
    // soft light shafts from above
    for (i in 0 until 5) {
        val x = width * (0.08f + 0.21f * i)
        drawRect(
            color = ToyColors.LightShaft,
            topLeft = Offset(x, 0f),
            size = Size(width * 0.07f, height),
        )
    }
    // slow rising bubbles (cosmetic, driven by frame count)
    for (i in 0 until 8) {
        val bx = width * (0.05f + 0.125f * i)
        val speed = 0.3f + (i % 3) * 0.2f
        val span = height + 60f
        val progress = (frame * speed + i * 137f) % span
        val by = height + 30f - progress
        drawCircle(
            color = ToyColors.Bubble,
            center = Offset(bx, by),
            radius = 3f + (i % 3) * 2f,
        )
    }
}

private fun DrawScope.drawRing(r: Ring) {
    val color = ToyColors.ringColors[r.colorIndex]
    drawCircle(
        color = color,
        center = Offset(r.x, r.y),
        radius = r.radius,
        style = Stroke(width = r.radius * 0.42f, cap = StrokeCap.Round),
    )
    // highlight arc that rotates with the ring, so spin is visible
    val deg = r.angle * 57.29578f
    drawArc(
        color = Color.White.copy(alpha = 0.4f),
        startAngle = deg,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = Offset(r.x - r.radius, r.y - r.radius),
        size = Size(r.radius * 2f, r.radius * 2f),
        style = Stroke(width = r.radius * 0.42f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawChamberFrame(width: Float, height: Float, density: Float) {
    val corner = 48f * density
    // clear plastic case edge
    drawRoundRect(
        color = ToyColors.ChamberEdge,
        topLeft = Offset(6f * density, 6f * density),
        size = Size(width - 12f * density, height - 12f * density),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = 2f * density),
    )
    // top-edge gloss
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(ToyColors.ChamberGloss, Color.Transparent),
            endY = height * 0.25f,
        ),
        topLeft = Offset(0f, 0f),
        size = Size(width, height * 0.25f),
        cornerRadius = CornerRadius(corner, corner),
    )
}

@Preview
@Composable
private fun ToyScreenPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ToyColors.WaterDeep),
    ) {
        ToyScreen()
    }
}