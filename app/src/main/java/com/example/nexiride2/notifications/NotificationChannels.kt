package com.example.nexiride2.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Central registration point for all system notification channels used by the
 * app. Channels only need to be created once per install; re-creating is a
 * no-op, so it's safe to call on every app start.
 */
object NotificationChannels {
    /** Channel id used for admin broadcasts and trip alerts. Keep in sync with
     *  the `default_notification_channel_id` meta-data in the manifest. */
    const val ALERTS_ID = "nexiride_alerts"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(ALERTS_ID) != null) return
        val channel = NotificationChannel(
            ALERTS_ID,
            "Alerts & updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Admin broadcasts, booking confirmations, and live trip updates."
            enableLights(true)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }
}
