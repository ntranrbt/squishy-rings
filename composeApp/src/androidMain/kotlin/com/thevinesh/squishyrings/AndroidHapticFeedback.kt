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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // solid "thock" with a short settle
            v.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 30L, 20L),
                    intArrayOf(-1, 160, 80),
                    -1,
                ),
            )
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(50)
        }
    }

    override fun pop() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(15, 90))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(15)
        }
    }
}