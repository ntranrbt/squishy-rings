package com.thevinesh.squishyrings

import androidx.compose.ui.graphics.Color

/**
 * Design tokens for the Squishy Rings toy chamber.
 *
 * Flat cartoon look: water painted as hard-edged bands rather than a smooth
 * gradient, every solid shape carries an [Outline] stroke, and highlights are
 * single opaque gloss marks. Realism belongs in the simulation, not the art.
 */
object ToyColors {
    // Water, painted top to bottom as flat bands
    val WaterTop = Color(0xFF00F0FF)
    val WaterMid = Color(0xFF00B4FF)
    val WaterBottom = Color(0xFF0078FF)
    val LightShaft = Color(0x12FFFFFF)
    val Bubble = Color(0xCCFFFFFF)
    val BubbleGlint = Color(0xFFFFFFFF)

    /** Cartoon ink. Every ring, peg and dome is drawn over a slightly fatter stroke of this. */
    val Outline = Color(0xFF141414)

    // Chamber (chunky toy shell)
    val ChamberRim = Color(0xFFE0F2F1)
    val ChamberGloss = Color(0xE6FFFFFF)

    // Squishy dome, outer ridge inward
    val SquishOuter = Color(0xFF7B1FA2)
    val SquishMid = Color(0xFF9C27B0)
    val SquishInner = Color(0xFFBA68C8)
    val SquishCore = Color(0xFF4A148C)
    val Gloss = Color(0xF2FFFFFF)

    // Ring colors (the toy's five, cartoon-saturated)
    val ringColors = listOf(
        Color(0xFFFF9800), // orange
        Color(0xFFE91E63), // pink/magenta
        Color(0xFF9C27B0), // purple
        Color(0xFF00BCD4), // turquoise
        Color(0xFFCDDC39), // lime
    )
}
