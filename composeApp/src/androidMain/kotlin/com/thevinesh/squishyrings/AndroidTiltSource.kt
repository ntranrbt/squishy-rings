package com.thevinesh.squishyrings

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper

/**
 * Device tilt via the rotation vector sensor (no permission required).
 *
 * The rotation vector gives the device orientation R (device to world, NED: x north,
 * y east, z down). Device-space gravity = R^T . (0, 0, 1). For a phone held in
 * portrait facing the user: screen right = +gx, screen down = +gy, so the
 * screen-space gravity vector is simply (gx, gy).
 */
class AndroidTiltSource(
    context: Context,
) : TiltSource, SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val rotationMatrix = FloatArray(9)

    private var last: Vec2 = Vec2(0f, 0f)

    override fun latest(): Vec2 = last

    override fun start() {
        if (sensor == null) return
        sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_GAME,
            mainHandler,
        )
    }

    override fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        // device-space gravity = R^T . (0,0,1) -> first column of R
        val gx = rotationMatrix[0]
        val gy = rotationMatrix[1]
        last = Vec2(gx, gy)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}