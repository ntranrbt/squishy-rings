package com.thevinesh.squishyrings

import kotlin.test.Test
import kotlin.test.assertTrue

class RingSimulationTest {
    private val sim = RingSimulation.create(width = 1080f, height = 1920f, seed = 42)

    @Test
    fun rings_stay_in_bounds_over_10_seconds() {
        repeat(2400) { sim.step(1f / 240f, Vec2(0f, 1f)) }
        sim.rings.forEach { r ->
            assertTrue(r.x in 0f..1080f, "x ${r.x} out of bounds")
            assertTrue(r.y in 0f..1920f, "y ${r.y} out of bounds")
        }
    }

    @Test
    fun rings_settle_lower_under_gravity() {
        val start = sim.rings.map { it.y }.average().toFloat()
        repeat(1200) { sim.step(1f / 240f, Vec2(0f, 1f)) }
        val end = sim.rings.map { it.y }.average().toFloat()
        assertTrue(end > start + 50f, "avg y $start -> $end")
    }

    @Test
    fun zero_gravity_no_divergence() {
        repeat(2400) { sim.step(1f / 240f, Vec2(0f, 0f)) }
        sim.rings.forEach { r ->
            assertTrue(
                r.vx in -200f..200f && r.vy in -200f..200f,
                "diverged: ${r.vx},${r.vy}",
            )
        }
    }

    @Test
    fun squish_kicks_rings_away_from_center() {
        val s = RingSimulation.create(width = 1080f, height = 1920f, seed = 7)
        s.rings.forEach { it.apply { x = 540f; y = 1200f; vx = 0f; vy = 0f } }
        s.step(1f / 240f, Vec2(0f, 0f), Impulse(cx = 540f, cy = 1600f, strength = 520f, falloff = 240f))
        val up = s.rings.count { it.vy < 0f }
        assertTrue(up > 0, "expected rings to be kicked upward, got $up")
    }

    @Test
    fun overlapping_rings_separate() {
        val s = RingSimulation.create(
            width = 1080f,
            height = 1920f,
            seed = 3,
            tuning = Tuning(ringCount = 2),
        )
        val a = s.rings.first()
        val b = s.rings[1]
        a.apply { x = 500f; y = 500f; vx = 0f; vy = 0f }
        b.apply { x = a.x + a.radius + b.radius - 5f; y = 500f; vx = 0f; vy = 0f }
        val before = b.x - a.x
        s.step(1f / 240f, Vec2(0f, 0f))
        assertTrue(b.x - a.x >= before - 1f, "overlap not resolved: $before -> ${b.x - a.x}")
    }
}