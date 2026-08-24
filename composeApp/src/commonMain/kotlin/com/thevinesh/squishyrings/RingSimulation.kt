package com.thevinesh.squishyrings

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Deterministic 2D "rings floating in fluid" simulation.
 *
 * Pure Kotlin: no time source, no platform calls. Tests drive [step] with an explicit
 * [dt] and a seeded [create] so results are reproducible on every platform.
 *
 * Feel model (tuned for "syrup", not "water"):
 * - gravity from a screen-space tilt vector (gyro), scaled down for buoyancy
 * - exponential drag (fluid resistance)
 * - a cheap pseudo-curl drift field so rings stay alive when idle
 * - one-shot radial impulses for the squishy button
 * - soft circle-circle collisions + rounded-rect walls
 */
class RingSimulation private constructor(
    val width: Float,
    val height: Float,
    val rings: MutableList<Ring>,
    private val rng: Random,
    private val tuning: Tuning,
) {
    private var time: Float = 0f

    fun resize(width: Float, height: Float) {
        rings.forEach { r ->
            r.x = r.x.coerceIn(r.radius, (width - r.radius).coerceAtLeast(r.radius))
            r.y = r.y.coerceIn(r.radius, (height - r.radius).coerceAtLeast(r.radius))
        }
    }

    /** Advance the sim by [dt] seconds under screen-space [gravity]; optional one-shot [impulse]. */
    fun step(dt: Float, gravity: Vec2, impulse: Impulse? = null) {
        val sdt = dt / tuning.substeps
        repeat(tuning.substeps) { i ->
            stepOnce(sdt, gravity, if (i == 0) impulse else null)
        }
        time += dt
    }

    private fun stepOnce(dt: Float, gravity: Vec2, impulse: Impulse?) {
        for (r in rings) {
            var fx = gravity.x * tuning.gravityScale
            var fy = gravity.y * tuning.gravityScale
            // pseudo-curl drift field: cheap, organic, no noise library
            fx += (sin(r.y * 0.011f + time * 0.7f) + cos(r.x * 0.013f - time * 0.5f)) * tuning.driftScale
            fy += (cos(r.x * 0.011f - time * 0.6f) + sin(r.y * 0.013f + time * 0.4f)) * tuning.driftScale
            r.vx += fx * dt
            r.vy += fy * dt
            val drag = exp(-tuning.drag * dt)
            r.vx *= drag
            r.vy *= drag
            r.x += r.vx * dt
            r.y += r.vy * dt
            r.angle += r.spin * dt
            r.spin *= exp(-1.5f * dt)
        }
        if (impulse != null) applyImpulse(impulse)
        resolveRingCollisions()
        resolveWalls()
    }

    private fun applyImpulse(imp: Impulse) {
        for (r in rings) {
            val dx = r.x - imp.cx
            val dy = r.y - imp.cy
            val dist = hypot(dx, dy).coerceAtLeast(1f)
            val k = imp.strength * exp(-dist / imp.falloff)
            r.vx += dx / dist * k
            r.vy += dy / dist * k
            r.spin += (rng.nextFloat() - 0.5f) * 2f * tuning.spinKick
        }
    }

    private fun resolveRingCollisions() {
        for (i in rings.indices) {
            for (j in i + 1 until rings.size) {
                val a = rings[i]
                val b = rings[j]
                val dx = b.x - a.x
                val dy = b.y - a.y
                val dist = hypot(dx, dy).coerceAtLeast(0.001f)
                val minDist = a.radius + b.radius
                if (dist < minDist) {
                    val nx = dx / dist
                    val ny = dy / dist
                    val push = (minDist - dist) / 2f
                    a.x -= nx * push
                    a.y -= ny * push
                    b.x += nx * push
                    b.y += ny * push
                    val relVn = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny
                    if (relVn < 0f) {
                        val jImp = -(1f + tuning.restitution) * relVn / 2f
                        a.vx -= nx * jImp
                        a.vy -= ny * jImp
                        b.vx += nx * jImp
                        b.vy += ny * jImp
                        val relVt = (b.vx - a.vx) * -ny + (b.vy - a.vy) * nx
                        a.spin += relVt * 0.004f
                        b.spin -= relVt * 0.004f
                    }
                }
            }
        }
    }

    private fun resolveWalls() {
        val inset = tuning.wallInset
        val cornerR = tuning.cornerRadius
        for (ring in rings) {
            // flat edges
            if (ring.x < inset + ring.radius) {
                ring.x = inset + ring.radius
                if (ring.vx < 0f) ring.vx = -ring.vx * tuning.wallDamping
            }
            if (ring.x > width - inset - ring.radius) {
                ring.x = width - inset - ring.radius
                if (ring.vx > 0f) ring.vx = -ring.vx * tuning.wallDamping
            }
            if (ring.y < inset + ring.radius) {
                ring.y = inset + ring.radius
                if (ring.vy < 0f) ring.vy = -ring.vy * tuning.wallDamping
            }
            if (ring.y > height - inset - ring.radius) {
                ring.y = height - inset - ring.radius
                if (ring.vy > 0f) ring.vy = -ring.vy * tuning.wallDamping
            }
            // rounded corners: arc constraint only inside the corner region
            val corner = when {
                ring.x < inset + cornerR && ring.y < inset + cornerR ->
                    Vec2(inset + cornerR, inset + cornerR)

                ring.x > width - inset - cornerR && ring.y < inset + cornerR ->
                    Vec2(width - inset - cornerR, inset + cornerR)

                ring.x < inset + cornerR && ring.y > height - inset - cornerR ->
                    Vec2(inset + cornerR, height - inset - cornerR)

                ring.x > width - inset - cornerR && ring.y > height - inset - cornerR ->
                    Vec2(width - inset - cornerR, height - inset - cornerR)

                else -> null
            }
            if (corner != null) {
                val dx = ring.x - corner.x
                val dy = ring.y - corner.y
                val dist = hypot(dx, dy)
                if (dist > cornerR && dist > 0.001f) {
                    val nx = dx / dist
                    val ny = dy / dist
                    ring.x = corner.x + nx * cornerR
                    ring.y = corner.y + ny * cornerR
                    val vDotN = ring.vx * nx + ring.vy * ny
                    if (vDotN > 0f) {
                        ring.vx -= (1f + tuning.wallDamping) * vDotN * nx
                        ring.vy -= (1f + tuning.wallDamping) * vDotN * ny
                    }
                }
            }
        }
    }

    companion object {
        fun create(
            width: Float,
            height: Float,
            seed: Long = 42,
            tuning: Tuning = Tuning.Default,
        ): RingSimulation {
            val rng = Random(seed)
            val rings = (0 until tuning.ringCount).map { i ->
                Ring(
                    x = width * (0.15f + 0.7f * rng.nextFloat()),
                    y = height * (0.15f + 0.7f * rng.nextFloat()),
                    angle = rng.nextFloat() * 2f * PI,
                    radius = tuning.minRingRadius +
                        (tuning.maxRingRadius - tuning.minRingRadius) * rng.nextFloat(),
                    colorIndex = i % ToyColors.ringColors.size,
                )
            }.toMutableList()
            return RingSimulation(width, height, rings, rng, tuning)
        }

        private const val PI = 3.14159265f
    }
}