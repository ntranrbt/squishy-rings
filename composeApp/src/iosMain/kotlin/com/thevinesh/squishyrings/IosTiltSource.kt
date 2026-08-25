package com.thevinesh.squishyrings

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import platform.darwin.dispatch_get_global_queue
import platform.darwin.DISPATCH_QUEUE_PRIORITY_BACKGROUND

/**
 * Device tilt via Core Motion (no Info.plist key required for CMMotionManager).
 *
 * `deviceMotion.gravity` is in device coordinates (x right, y up, z out of the
 * screen, magnitude ~1g). Screen space has y down, so the mapping is (g.x, -g.y).
 *
 * Updates run into a background queue; [latest] (called once per frame) reads
 * the most recent sample.
 */
@OptIn(ExperimentalForeignApi::class)
class IosTiltSource : TiltSource {
    private val manager = CMMotionManager()
    private var queue: NSOperationQueue? = null

    private var latestX: Float = 0f
    private var latestY: Float = 0f

    override fun latest(): Vec2 = Vec2(latestX, latestY)

    override fun start() {
        if (!manager.isDeviceMotionAvailable()) return
        val q = NSOperationQueue()
        q.name = "com.thevinesh.squishyrings.tilt"
        queue = q
        manager.deviceMotionUpdateInterval = 1.0 / 30.0
        manager.startDeviceMotionUpdatesToQueue(q) { motion, _ ->
            if (motion != null) {
                val g = motion.gravity.useContents { Pair(this.x, this.y) }
                latestX = g.first.toFloat()
                latestY = -g.second.toFloat()
            }
        }
    }

    override fun stop() {
        manager.stopDeviceMotionUpdates()
        queue = null
    }
}
