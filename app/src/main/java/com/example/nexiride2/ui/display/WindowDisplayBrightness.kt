package com.example.nexiride2.ui.display

import android.view.Window
import android.view.WindowManager
import com.example.nexiride2.data.preferences.DisplayBrightnessState

/**
 * Sets this window's brightness.
 *
 * - Manual mode: pins the screen at [DisplayBrightnessState.manualLevel].
 * - Auto mode without an ambient reading: falls back to the system default
 *   (device auto-brightness + the OS slider).
 * - Auto mode with an [ambientLevel] supplied by our light sensor: uses that
 *   reading directly so the app adapts even if the OS auto-brightness is off.
 */
fun Window.applyDisplayBrightness(
    state: DisplayBrightnessState,
    ambientLevel: Float? = null
) {
    val lp = attributes
    lp.screenBrightness = when {
        !state.isAuto -> state.manualLevel.coerceIn(0.15f, 1f)
        ambientLevel != null -> ambientLevel.coerceIn(0.15f, 1f)
        else -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }
    attributes = lp
}
