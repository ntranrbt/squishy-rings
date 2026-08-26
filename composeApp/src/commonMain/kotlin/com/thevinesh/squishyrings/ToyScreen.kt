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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

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
    // flat cartoon bands, hard edges
    val topBand = height * 0.30f
    val midBand = height * 0.58f
    drawRect(color = ToyColors.WaterTop, size = Size(width, topBand))
    drawRect(
        color = ToyColors.WaterMid,
        topLeft = Offset(0f, topBand),
        size = Size(width, midBand - topBand),
    )
    drawRect(
        color = ToyColors.WaterBottom,
        topLeft = Offset(0f, midBand),
        size = Size(width, height - midBand),
    )
    // a few slanted light shafts, angled so they never read as vertical banding
    for (i in 0 until 3) {
        val x = width * (0.18f + 0.3f * i)
        drawLine(
            color = ToyColors.LightShaft,
            start = Offset(x, -height * 0.1f),
            end = Offset(x + width * 0.35f, height * 1.1f),
            strokeWidth = width * 0.11f,
        )
    }
    // slow rising bubbles (cosmetic, driven by frame count)
    for (i in 0 until 8) {
        val bx = width * (0.05f + 0.125f * i)
        val speed = 0.3f + (i % 3) * 0.2f
        val span = height + 60f
        val progress = (frame * speed + i * 137f) % span
        val by = height + 30f - progress
        val r = 5f + (i % 3) * 3f
        drawCircle(color = ToyColors.Bubble, center = Offset(bx, by), radius = r)
        drawCircle(
            color = ToyColors.BubbleGlint,
            center = Offset(bx - r * 0.3f, by - r * 0.35f),
            radius = r * 0.28f,
        )
    }
}

private fun DrawScope.drawRing(r: Ring) {
    val center = Offset(r.x, r.y)
    val band = r.radius * 0.42f
    // ink pass first: a fatter stroke of the same circle reads as an outline on both edges
    drawCircle(
        color = ToyColors.Outline,
        center = center,
        radius = r.radius,
        style = Stroke(width = band + r.radius * 0.24f),
    )
    drawCircle(
        color = ToyColors.ringColors[r.colorIndex],
        center = center,
        radius = r.radius,
        style = Stroke(width = band),
    )
    // one gloss dot orbiting the band, so spin stays readable
    drawCircle(
        color = ToyColors.Gloss,
        center = Offset(r.x + cos(r.angle) * r.radius, r.y + sin(r.angle) * r.radius),
        radius = band * 0.3f,
    )
}

private fun DrawScope.drawChamberFrame(width: Float, height: Float, density: Float) {
    val corner = 48f * density
    val rim = 6f * density
    // chunky toy shell
    drawRoundRect(
        color = ToyColors.ChamberRim,
        topLeft = Offset(rim / 2f, rim / 2f),
        size = Size(width - rim, height - rim),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = rim),
    )
    // two gloss slashes across the top-left corner
    val arcBox = corner * 1.7f
    drawArc(
        color = ToyColors.ChamberGloss,
        startAngle = 185f,
        sweepAngle = 50f,
        useCenter = false,
        topLeft = Offset(corner * 0.5f, corner * 0.5f),
        size = Size(arcBox, arcBox),
        style = Stroke(width = 4f * density, cap = StrokeCap.Round),
    )
    drawArc(
        color = ToyColors.ChamberGloss,
        startAngle = 195f,
        sweepAngle = 22f,
        useCenter = false,
        topLeft = Offset(corner * 0.9f, corner * 0.9f),
        size = Size(arcBox * 0.75f, arcBox * 0.75f),
        style = Stroke(width = 3f * density, cap = StrokeCap.Round),
    )
}

@Preview
@Composable
private fun ToyScreenPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ToyColors.WaterBottom),
    ) {
        ToyScreen()
    }
}