package com.petradar.mobileui.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.petradar.mobileui.MedicationAlarmReceiver
import java.util.Calendar

/**
 * Schedules and cancels exact repeating alarms for medication reminders.
 *
 * Because [AlarmManager.setRepeating] is inexact on API 19+, we use
 * [AlarmManager.setExactAndAllowWhileIdle] and re-schedule the next alarm
 * from inside [MedicationAlarmReceiver] after each firing.
 */
object MedicationScheduler {

    /** Extra keys sent to [MedicationAlarmReceiver]. */
    const val EXTRA_PET_ID       = "med_pet_id"
    const val EXTRA_REMINDER_ID  = "med_reminder_id"
    const val EXTRA_PET_NAME     = "med_pet_name"
    const val EXTRA_MEDICINE     = "med_medicine"
    const val EXTRA_NOTES        = "med_notes"
    const val EXTRA_FREQ_TYPE    = "med_freq_type"
    const val EXTRA_FREQ_VALUE   = "med_freq_value"

    /**
     * Schedules the first (or next) alarm for [reminder].
     *
     * @param petId    Owner pet ID — used to build a unique request code.
     * @param petName  Shown in the notification.
     */
    fun schedule(context: Context, petId: Long, petName: String, reminder: MedicationReminder) {
        if (!reminder.isActive) return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pi = buildPendingIntent(context, petId, reminder) ?: return

        // First trigger: today at the configured hour:minute.
        // If that time has already passed today, schedule for tomorrow.
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi
            )
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }

    /**
     * Cancels the alarm for a specific reminder.
     */
    fun cancel(context: Context, petId: Long, reminder: MedicationReminder) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pi = buildPendingIntent(context, petId, reminder,
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        pi?.let { alarmManager.cancel(it) }
    }

    /**
     * Cancels all alarms for every reminder of [petId].
     */
    fun cancelAll(context: Context, petId: Long) {
        MedicationStore.getAll(context, petId).forEach { cancel(context, petId, it) }
    }

    /**
     * Reschedules the NEXT alarm after one has fired.
     * Called from [MedicationAlarmReceiver].
     */
    fun scheduleNext(
        context: Context,
        petId: Long,
        petName: String,
        reminderId: Long,
        frequencyType: String,
        frequencyValue: Int
    ) {
        val reminder = MedicationStore.getAll(context, petId)
            .find { it.id == reminderId } ?: return
        if (!reminder.isActive) return

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pi = buildPendingIntent(context, petId, reminder) ?: return

        val nextMillis = System.currentTimeMillis() + when (frequencyType) {
            "hours" -> frequencyValue * 60L * 60L * 1000L
            else    -> frequencyValue * 24L * 60L * 60L * 1000L   // days
        }
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMillis, pi)
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextMillis, pi)
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun buildPendingIntent(
        context: Context,
        petId: Long,
        reminder: MedicationReminder,
        flags: Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    ): PendingIntent? {
        // Request code: combine petId and reminderId for uniqueness
        val requestCode = ((petId and 0xFFFF) shl 16 or (reminder.id and 0xFFFF)).toInt()
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra(EXTRA_PET_ID, petId)
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_PET_NAME, "")        // filled at runtime from store
            putExtra(EXTRA_MEDICINE, reminder.medicineName)
            putExtra(EXTRA_NOTES, reminder.notes)
            putExtra(EXTRA_FREQ_TYPE, reminder.frequencyType)
            putExtra(EXTRA_FREQ_VALUE, reminder.frequencyValue)
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}

