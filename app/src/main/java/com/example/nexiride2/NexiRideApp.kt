package com.example.nexiride2

import android.app.Application
import com.example.nexiride2.BuildConfig
import com.example.nexiride2.data.firebase.FirebaseSeedEntryPoint
import com.example.nexiride2.data.firebase.FirestoreSeed
import com.example.nexiride2.notifications.NotificationChannels
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class NexiRideApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Register the system notification channel exactly once per install
        // so FCM pushes land in the right bucket and respect user controls.
        NotificationChannels.ensureCreated(this)
        if (BuildConfig.DEBUG) {
            val entry = EntryPointAccessors.fromApplication(this, FirebaseSeedEntryPoint::class.java)
            applicationScope.launch(Dispatchers.IO) {
                runCatching {
                    FirestoreSeed.seedIfEmpty(entry.firestore(), entry.gson())
                }
            }
        }
    }
}
