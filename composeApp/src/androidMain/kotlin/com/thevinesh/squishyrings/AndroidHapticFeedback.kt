package com.thevinesh.squishyrings

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

internal class AndroidHapticFeedback(
    context: Context,
) : HapticFeedback {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    override fun squish() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        // solid "thock" with a short settle
        val effect = VibrationEffect.createWaveform(
            longArrayOf(0L, 30L, 20L),
            intArrayOf(160, 80),
            intArrayOf(-1, -1),
        )
        v.vibrate(effect)
    }

    override fun pop() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        v.vibrate(VibrationEffect.createOneShot(15, 90))
    }
}