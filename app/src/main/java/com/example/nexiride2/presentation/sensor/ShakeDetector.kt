package com.example.nexiride2.presentation.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

internal class ShakeDetector(
    private val onShake: () -> Unit
) : SensorEventListener {

    private var lastShakeAt = 0L
    private var lastMag = SensorManager.GRAVITY_EARTH

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val mag = sqrt(x * x + y * y + z * z)
        val jerk = kotlin.math.abs(mag - lastMag)
        lastMag = mag
        if (jerk > JERK_THRESHOLD) {
            val now = System.currentTimeMillis()
            if (now - lastShakeAt > COOLDOWN_MS) {
                lastShakeAt = now
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private companion object {
        const val JERK_THRESHOLD = 11f
        const val COOLDOWN_MS = 1_200L
    }
}
