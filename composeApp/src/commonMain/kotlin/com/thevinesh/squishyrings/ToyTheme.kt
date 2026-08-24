package com.thevinesh.squishyrings

import androidx.compose.ui.graphics.Color

/**
 * Design tokens for the Squishy Rings toy chamber.
 * Palette mirrors the physical toy: bright ring colors over a teal underwater scene,
 * purple squishy dome.
 */
object ToyColors {
    // Water
    val WaterDeep = Color(0xFF0B3B5C)
    val WaterMid = Color(0xFF12618C)
    val WaterLight = Color(0xFF1E88B5)
    val LightShaft = Color(0x14FFFFFF)
    val Bubble = Color(0x29FFFFFF)

    // Chamber (clear plastic case)
    val ChamberEdge = Color(0x40FFFFFF)
    val ChamberGloss = Color(0x29FFFFFF)

    // Squishy button
    val SquishPurple = Color(0xFF9C4DCC)
    val SquishPurpleDeep = Color(0xFF6A2C96)
    val Gloss = Color(0x66FFFFFF)
    val GlossTransparent = Color(0x00FFFFFF)

    // Ring colors (the toy's five)
    val ringColors = listOf(
        Color(0xFFF97316), // orange
        Color(0xFFEC4899), // pink/magenta
        Color(0xFFA78BFA), // lavender
        Color(0xFF2DD4BF), // teal
        Color(0xFFA3E635), // lime green
    )
}