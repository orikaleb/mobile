package com.example.nexiride2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nexiride2.data.preferences.AppBrightnessSettings
import com.example.nexiride2.presentation.navigation.NexiRideNavHost
import com.example.nexiride2.ui.display.applyDisplayBrightness
import com.example.nexiride2.ui.display.rememberAmbientBrightnessLevel
import com.example.nexiride2.ui.theme.NexiRideTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appBrightnessSettings: AppBrightnessSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val displayBrightness by appBrightnessSettings.state.collectAsStateWithLifecycle()
            // Only subscribe to the ambient light sensor while auto mode is on so
            // we don't drain battery when the user picked a fixed in-app level.
            val ambientLevel by rememberAmbientBrightnessLevel(enabled = displayBrightness.isAuto)
            SideEffect {
                window.applyDisplayBrightness(displayBrightness, ambientLevel)
            }
            NexiRideTheme {
                NexiRideNavHost()
            }
        }
    }
}
