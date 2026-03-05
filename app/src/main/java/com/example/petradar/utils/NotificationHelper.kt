package com.example.petradar.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.petradar.AppointmentAlarmReceiver
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Helper for creating the notification channel and scheduling appointment reminders
 * via [AlarmManager].
 *
 * Two alarms per appointment:
 *  1. **One hour before** — always scheduled.
 *  2. **One day before** — scheduled only when [remindDayBefore] is true.
 */
object NotificationHelper {

    const val CHANNEL_ID = "appointment_reminders"
    const val CHANNEL_NAME = "Recordatorios de citas"

    /** Extra keys forwarded to [AppointmentAlarmReceiver]. */
    const val EXTRA_TITLE = "extra_notif_title"
    const val EXTRA_BODY = "extra_notif_body"
    const val EXTRA_NOTIF_ID = "extra_notif_id"

    /** Request-code suffix so each PendingIntent is unique. */
    private const val RC_HOUR_OFFSET = 1_000_000   // appointmentId * 2 + this
    private const val RC_DAY_OFFSET = 2_000_000

    /**
     * Creates the notification channel. Safe to call multiple times (no-op if already exists).
     * Must be called before posting any notification (e.g. in Application.onCreate).
     */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Recordatorios automáticos de citas veterinarias"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    /**
     * Schedules one or two alarms for the given appointment.
     *
     * @param context          Application/Activity context.
     * @param appointmentId    Used to build unique request codes and notification IDs.
     * @param petName          Pet name shown in the notification body.
     * @param reason           Reason for visit shown in the notification body.
     * @param appointmentTime  Exact date-time of the appointment (device local time).
     * @param remindDayBefore  If true, also schedules a "day before" alarm.
     */
    fun scheduleReminders(
        context: Context,
        appointmentId: Long,
        petName: String,
        reason: String,
        appointmentTime: LocalDateTime,
        remindDayBefore: Boolean
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        // — 1 hour before —
        val oneHourBefore = appointmentTime.minusHours(1)
        val oneHourMillis = oneHourBefore.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (oneHourMillis > System.currentTimeMillis()) {
            scheduleAlarm(
                context, alarmManager,
                triggerAtMillis = oneHourMillis,
                requestCode = (appointmentId * 2 + RC_HOUR_OFFSET).toInt(),
                notifId = (appointmentId * 2).toInt(),
                title = "Cita en 1 hora 🐾",
                body = "Cita de $petName: $reason"
            )
        }

        // — 1 day before (same hour) —
        if (remindDayBefore) {
            val oneDayBefore = appointmentTime.minusDays(1)
            val oneDayMillis = oneDayBefore.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (oneDayMillis > System.currentTimeMillis()) {
                scheduleAlarm(
                    context, alarmManager,
                    triggerAtMillis = oneDayMillis,
                    requestCode = (appointmentId * 2 + RC_DAY_OFFSET).toInt(),
                    notifId = (appointmentId * 2 + 1).toInt(),
                    title = "Cita mañana 🐾",
                    body = "Recuerda: cita de $petName mañana – $reason"
                )
            }
        }
    }

    /**
     * Cancels all pending alarms for [appointmentId] (both hour-before and day-before).
     * Call this when an appointment is deleted or edited.
     */
    fun cancelReminders(context: Context, appointmentId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        listOf(
            (appointmentId * 2 + RC_HOUR_OFFSET).toInt(),
            (appointmentId * 2 + RC_DAY_OFFSET).toInt()
        ).forEach { rc ->
            val intent = Intent(context, AppointmentAlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context, rc, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pi?.let { alarmManager.cancel(it) }
        }
    }

    // ── private helpers ────────────────────────────────────────────────────────

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        requestCode: Int,
        notifId: Int,
        title: String,
        body: String
    ) {
        val intent = Intent(context, AppointmentAlarmReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use setExactAndAllowWhileIdle for reliable delivery even in Doze mode.
        // On Android 12+ this requires SCHEDULE_EXACT_ALARM or USE_EXACT_ALARM.
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            // Fallback: inexact alarm if exact alarm permission is denied.
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}

