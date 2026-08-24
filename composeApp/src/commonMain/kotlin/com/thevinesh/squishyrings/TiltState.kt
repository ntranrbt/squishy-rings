package com.thevinesh.squishyrings

import kotlin.math.exp

/**
 * Low-pass filtered screen-space gravity from the device tilt.
 * `current` is the vector the sim consumes: (0, 1) = portrait upright (rings sink down),
 * (0, 0) = flat face-up (free drift).
 */
class TiltState(private val tau: Float = 0.15f) {
    private var sx = 0f
    private var sy = 0f

    var current: Vec2 = Vec2(0f, 0f)
        private set

    /** Feed raw device tilt at any rate; internal exponential smoothing absorbs jitter. */
    fun update(raw: Vec2, dt: Float) {
        val a = 1f - exp(-dt / tau)
        sx += (raw.x - sx) * a
        sy += (raw.y - sy) * a
        current = Vec2(sx, sy)
    }
}

/**
 * Platform tilt source (gyro/accelerometer). Pushes screen-space gravity vectors
 * into [sink]; [latest] returns the most recent raw vector for the frame loop.
 * No permission is required on Android (ROTATION_VECTOR) or iOS (CMMotionManager).
 */
expect class PlatformTilt(val sink: (Vec2) -> Unit) {
    fun start()
    fun stop()
    fun latest(): Vec2
}