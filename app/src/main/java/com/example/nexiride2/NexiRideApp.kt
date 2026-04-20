package com.example.nexiride2

import android.app.Application
import com.example.nexiride2.BuildConfig
import com.example.nexiride2.data.supabase.SupabaseSeed
import com.example.nexiride2.data.supabase.SupabaseSeedEntryPoint
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
        if (BuildConfig.DEBUG && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            val entry = EntryPointAccessors.fromApplication(this, SupabaseSeedEntryPoint::class.java)
            applicationScope.launch(Dispatchers.IO) {
                runCatching {
                    SupabaseSeed.seedIfEmpty(entry.supabaseApi(), entry.gson())
                }
            }
        }
    }
}
