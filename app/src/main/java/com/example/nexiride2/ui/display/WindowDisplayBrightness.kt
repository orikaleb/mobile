package com.example.nexiride2.ui.display

import android.view.Window
import android.view.WindowManager
import com.example.nexiride2.data.preferences.DisplayBrightnessState

/**
 * Sets this window’s brightness: [DisplayBrightnessState.isAuto] uses the
 * system default (device auto-brightness and global slider), otherwise a
 * fixed level for this app only.
 */
fun Window.applyDisplayBrightness(state: DisplayBrightnessState) {
    val lp = attributes
    lp.screenBrightness = if (state.isAuto) {
        WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    } else {
        state.manualLevel.coerceIn(0.15f, 1f)
    }
    attributes = lp
}
