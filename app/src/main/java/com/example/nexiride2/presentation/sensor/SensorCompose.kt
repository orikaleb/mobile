package com.example.nexiride2.presentation.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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

@Composable
fun ProximityCoveredEffect(onCoveredChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val latest by rememberUpdatedState(onCoveredChange)
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY) ?: return@DisposableEffect onDispose { }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val max = event.sensor.maximumRange
                val v = event.values[0]
                val near = if (max <= 0f) v < 1f else v < max
                latest(near)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose {
            sm.unregisterListener(listener)
            latest(false)
        }
    }
}
