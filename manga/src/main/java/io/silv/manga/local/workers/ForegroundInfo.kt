package io.silv.manga.local.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import io.silv.manga.R

/**
 * Foreground information for sync on lower API levels when sync workers are being
 * run with a foreground service
 */
fun Context.createForegroundInfo(
    notificationId: Int,
    channelId: String,
) = ForegroundInfo(
    notificationId,
    createWorkNotification(channelId),
)

/**
 * Notification displayed on lower API levels when sync workers are being
 * run with a foreground service
 */
private fun Context.createWorkNotification(channelId: String): Notification {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "background work",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "completing background work."
        }
        // Register the channel with the system
        val notificationManager: NotificationManager? =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        notificationManager?.createNotificationChannel(channel)
    }

    return NotificationCompat.Builder(
        this,
        channelId,
    )
        .setSmallIcon(
           R.drawable.amadeuslogo
        )
        .setContentTitle("completing background work.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
}