package com.thevinesh.squishyrings

/** 2D vector used by the physics sim and tilt input. */
data class Vec2(val x: Float, val y: Float) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)
    val length: Float get() = kotlin.math.sqrt(x * x + y * y)
}

/** One floating ring inside the chamber. Mutable fields are owned by [RingSimulation] only. */
class Ring(
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var angle: Float = 0f,
    var spin: Float = 0f,
    val radius: Float,
    val colorIndex: Int,
)

/** One-shot radial kick from a squish press: [strength] px/s at the center, exponential falloff over [falloff] px. */
data class Impulse(val cx: Float, val cy: Float, val strength: Float, val falloff: Float)

/** All feel constants in one place — tune in one spot, previewable in tests. */
data class Tuning(
    val ringCount: Int = 22,
    val minRingRadius: Float = 22f,
    val maxRingRadius: Float = 34f,
    val gravityScale: Float = 140f, // px/s^2 at unit gravity; near-buoyant rings sink slowly
    val drag: Float = 2.0f,         // 1/s; the "fluid"
    val driftScale: Float = 35f,    // idle organic drift
    val substeps: Int = 2,
    val restitution: Float = 0.35f,
    val wallDamping: Float = 0.4f,
    val squishStrength: Float = 1100f, // px/s kick at impulse center
    val squishFalloff: Float = 480f,   // px
    val popStrength: Float = 480f,
    val spinKick: Float = 5.0f,
    /** Mix of radial spray vs screen-up (0 = purely radial, 1 = purely upward). */
    val impulseUpBias: Float = 0.55f,
    val wallInset: Float = 24f,
    val cornerRadius: Float = 56f,
) {
    companion object {
        val Default = Tuning()
    }
}