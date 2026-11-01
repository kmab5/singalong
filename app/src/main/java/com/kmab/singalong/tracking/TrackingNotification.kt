package com.kmab.singalong.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.kmab.singalong.R
import com.kmab.singalong.app.MainActivity

object TrackingNotification {
    const val ID = 100
    private const val CHANNEL = "tracking"

    fun create(context: Context, tracking: Boolean): Notification {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Music tracking", NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        fun action(name: String, request: Int) = PendingIntent.getService(
            context, request, Intent(context, TrackingService::class.java).setAction(name),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (tracking) "Singalong is following your player" else "Singalong overlay preview")
            .setContentText("P0 build · lyrics lookup is not connected yet")
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, "show preview", action(TrackingService.ACTION_SHOW_OVERLAY, 1))
            .addAction(0, "stop", action(TrackingService.ACTION_STOP, 2))
            .build()
    }
}
