package com.example.nexiride2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.nexiride2.presentation.navigation.NexiRideNavHost
import com.example.nexiride2.ui.theme.NexiRideTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexiRideTheme {
                NexiRideNavHost()
            }
        }
    }
}