package com.example.nexiride2.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.nexiride2.MainActivity
import com.example.nexiride2.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlin.random.Random

/**
 * Receives FCM pushes from the Cloud Function that fans out our Firestore
 * `notifications` collection. Two responsibilities:
 *   1. Persist the freshly-minted device token so the server can target this
 *      device. See [FcmTokenManager.saveToken].
 *   2. Render a system-tray notification on the user's device when a push
 *      arrives. Works for both "notification" and "data" FCM payload shapes.
 */
@AndroidEntryPoint
class NexiRideMessagingService : FirebaseMessagingService() {

    @javax.inject.Inject lateinit var tokenManager: FcmTokenManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Best-effort: if the user is signed in we write it now, otherwise
        // [FcmTokenManager] will also be called on next sign-in.
        tokenManager.saveTokenAsync(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        NotificationChannels.ensureCreated(this)

        // FCM can deliver either a typed "notification" payload (handled by
        // the system when the app is backgrounded) or a "data" payload which
        // always hits this callback. Prefer whichever is present.
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            Random.nextInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationChannels.ALERTS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .build()

        // Android 13+ requires runtime permission; skip silently if we don't
        // have it (MainActivity requests it at first launch).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        NotificationManagerCompat.from(this).notify(Random.nextInt(), notification)
    }
}
