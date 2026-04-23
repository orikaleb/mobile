package com.example.nexiride2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nexiride2.data.preferences.AppBrightnessSettings
import com.example.nexiride2.notifications.FcmTokenManager
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

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    // Re-register the FCM token whenever the user grants the runtime
    // notification permission — without it pushes would be dropped silently.
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fcmTokenManager.registerCurrentDevice()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
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

    /**
     * Android 13 introduced [Manifest.permission.POST_NOTIFICATIONS] as a
     * runtime permission. Without it the system drops every push we would
     * otherwise show, so we prompt on the very first launch. On older
     * releases the permission is granted at install time and there's nothing
     * to ask for.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val already = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (already) {
            fcmTokenManager.registerCurrentDevice()
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
