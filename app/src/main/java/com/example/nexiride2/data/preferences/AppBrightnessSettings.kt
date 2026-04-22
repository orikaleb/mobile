package com.example.nexiride2.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DisplayBrightnessState(
    val isAuto: Boolean = true,
    val manualLevel: Float = 0.55f
)

private val KEY_AUTO = booleanPreferencesKey("screen_brightness_auto")
private val KEY_MANUAL = floatPreferencesKey("screen_brightness_level")

@Singleton
class AppBrightnessSettings @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val dataStore = context.appDisplayDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val state = dataStore.data
        .map { p ->
            DisplayBrightnessState(
                isAuto = p[KEY_AUTO] ?: true,
                manualLevel = (p[KEY_MANUAL] ?: 0.55f).coerceIn(0.15f, 1f)
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, DisplayBrightnessState())

    fun setAuto(value: Boolean) {
        scope.launch { dataStore.edit { it[KEY_AUTO] = value } }
    }

    fun setManualLevel(value: Float) {
        val v = value.coerceIn(0.15f, 1f)
        scope.launch { dataStore.edit { it[KEY_MANUAL] = v } }
    }
}
