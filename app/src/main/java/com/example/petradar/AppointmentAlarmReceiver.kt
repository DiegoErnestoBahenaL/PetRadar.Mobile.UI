package com.example.petradar

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.petradar.utils.NotificationHelper

/**
 * [BroadcastReceiver] that is triggered by [android.app.AlarmManager] when an appointment reminder fires.
 *
 * Reads the notification title, body and ID from the Intent extras set by
 * [NotificationHelper.scheduleReminders] and posts a high-priority notification
 * on the [NotificationHelper.CHANNEL_ID] channel.
 */
class AppointmentAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(NotificationHelper.EXTRA_TITLE)
            ?: "Recordatorio de cita"
        val body = intent.getStringExtra(NotificationHelper.EXTRA_BODY)
            ?: "Tienes una cita veterinaria próximamente."
        val notifId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIF_ID, 0)

        // Tap action: open AppointmentsActivity
        val tapIntent = Intent(context, AppointmentsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context, notifId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(notifId, notification)
    }
}

