package com.thevinesh.squishyrings

import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

/**
 * Device tilt via Core Motion (no Info.plist key required for CMMotionManager).
 *
 * `deviceMotion.gravity` is in device coordinates (x right, y up, z out of the
 * screen, magnitude ~1g). Screen space has y down, so the mapping is (g.x, -g.y).
 *
 * Updates run into a background NSOperationQueue; [latest] (called once per
 * frame from the main-thread frame loop) reads the most recent sample.
 */
class IosTiltSource : TiltSource {
    private val manager = CMMotionManager()
    private var queue: NSOperationQueue? = null

    override fun latest(): Vec2 {
        val g = manager.deviceMotion?.gravity ?: return Vec2(0f, 0f)
        return Vec2(g.x.toFloat(), -g.y.toFloat())
    }

    override fun start() {
        if (!manager.isDeviceMotionAvailable) return
        val q = NSOperationQueue()
        q.name = "com.thevinesh.squishyrings.tilt"
        queue = q
        manager.deviceMotionUpdateInterval = 1.0 / 30.0
        manager.startDeviceMotionUpdatesToQueue(q, withInterval = 1.0 / 30.0)
    }

    override fun stop() {
        manager.stopDeviceMotionUpdates()
        queue = null
    }
}