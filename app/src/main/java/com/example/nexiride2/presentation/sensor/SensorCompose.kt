package com.example.nexiride2.presentation.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
fun ShakeToRefresh(
    enabled: Boolean,
    onShake: () -> Unit
) {
    val context = LocalContext.current
    val latestOnShake by rememberUpdatedState(onShake)
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose { }
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return@DisposableEffect onDispose { }
        val detector = ShakeDetector { latestOnShake() }
        sm.registerListener(detector, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(detector) }
    }
}

/**
 * Invokes [onCoveredChange] whenever the device's proximity sensor flips
 * between "covered" (near face / inside a pocket) and uncovered.
 *
 * Two robustness fixes over a naïve reading:
 *   • Many phones ship a binary proximity sensor that reports 0 (near) and
 *     a device-specific max (far), while others report a continuous cm
 *     distance. We treat anything closer than 5 cm AND strictly below the
 *     reported max as "covered" — that catches both hardware styles.
 *   • Sensors sometimes jitter on a borderline reading (e.g. finger hovering
 *     a couple of cm away). Debouncing by 200 ms stops the masked UI from
 *     flickering on and off.
 */
@Composable
fun ProximityCoveredEffect(onCoveredChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val latest by rememberUpdatedState(onCoveredChange)
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY) ?: return@DisposableEffect onDispose { }
        val handler = Handler(Looper.getMainLooper())
        var lastReported: Boolean? = null
        var pending: Runnable? = null

        fun publish(near: Boolean) {
            pending?.let(handler::removeCallbacks)
            pending = Runnable {
                if (lastReported != near) {
                    lastReported = near
                    latest(near)
                }
            }.also { handler.postDelayed(it, 200L) }
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val max = event.sensor.maximumRange
                val v = event.values.firstOrNull() ?: return
                // Covered = strictly below the sensor's max (handles binary
                // sensors) AND within 5 cm (handles continuous sensors that
                // report e.g. "8 cm" as the far reading on top of 0/8 binary).
                val near = v < max && v < 5f
                publish(near)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        // SENSOR_DELAY_UI is faster than NORMAL so the mask appears before
        // the user has time to read a glance of the QR through fabric.
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            pending?.let(handler::removeCallbacks)
            sm.unregisterListener(listener)
            latest(false)
        }
    }
}
