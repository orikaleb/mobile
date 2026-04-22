package com.example.nexiride2.ui.display

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.ln
import kotlin.math.max

/**
 * Subscribes to [Sensor.TYPE_LIGHT] while [enabled] is true. The returned state
 * holds a brightness fraction in `[0.15f, 1f]` (null if no sensor / disabled).
 *
 * Mapping uses a log curve so the app stays comfortable across pitch-dark rooms
 * (≈0 lux) up to direct sunlight (≈10 000 lux).
 */
@Composable
fun rememberAmbientBrightnessLevel(enabled: Boolean): MutableState<Float?> {
    val context = LocalContext.current
    val level = remember { mutableStateOf<Float?>(null) }
    DisposableEffect(enabled) {
        if (!enabled) {
            level.value = null
            return@DisposableEffect onDispose { }
        }
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (sm == null || sensor == null) {
            level.value = null
            return@DisposableEffect onDispose { }
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val lux = event?.values?.firstOrNull() ?: return
                // log10(lux+1) / log10(10_001) → smooths out the huge dynamic range.
                val normalized = (ln(max(lux, 0f) + 1f) / ln(10_001f)).coerceIn(0f, 1f)
                // Keep a readable floor (0.20) so the screen never turns pitch black.
                level.value = (0.20f + normalized * 0.80f).coerceIn(0.20f, 1f)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose {
            sm.unregisterListener(listener)
            level.value = null
        }
    }
    return level
}
