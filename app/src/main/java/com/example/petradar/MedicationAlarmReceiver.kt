package com.example.petradar

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.petradar.utils.MedicationScheduler
import com.example.petradar.utils.NotificationHelper
import com.example.petradar.utils.PetNameStore

/**
 * Fires when a medication alarm triggers.
 *
 * Posts a notification and immediately re-schedules the next alarm
 * based on the reminder's frequency so the cycle continues automatically.
 */
class MedicationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val petId       = intent.getLongExtra(MedicationScheduler.EXTRA_PET_ID, -1L)
        val reminderId  = intent.getLongExtra(MedicationScheduler.EXTRA_REMINDER_ID, -1L)
        val medicine    = intent.getStringExtra(MedicationScheduler.EXTRA_MEDICINE) ?: "Medicamento"
        val notes       = intent.getStringExtra(MedicationScheduler.EXTRA_NOTES) ?: ""
        val freqType    = intent.getStringExtra(MedicationScheduler.EXTRA_FREQ_TYPE) ?: "days"
        val freqValue   = intent.getIntExtra(MedicationScheduler.EXTRA_FREQ_VALUE, 1)

        if (petId < 0 || reminderId < 0) return

        // Resolve pet name: prefer the intent extra, then the local store, then a generic fallback
        val petNameFromIntent = intent.getStringExtra(MedicationScheduler.EXTRA_PET_NAME)
            ?.takeIf { it.isNotBlank() }
        val petName = petNameFromIntent
            ?: PetNameStore.get(context, petId)
            ?: "Tu mascota"

        val freqLabel = when (freqType) {
            "hours" -> "cada $freqValue h"
            else    -> if (freqValue == 1) "cada día" else "cada $freqValue días"
        }
        val body = buildString {
            append("Hora de dar $medicine a $petName")
            if (notes.isNotBlank()) append(" · $notes")
            append(" ($freqLabel)")
        }

        // Tap → open PetsActivity
        val tapIntent = Intent(context, PetsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPi = PendingIntent.getActivity(
            context, reminderId.toInt(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifId = (petId * 10_000 + reminderId).toInt()
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("💊 $petName – $medicine")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPi)
            .build()

        context.getSystemService(NotificationManager::class.java).notify(notifId, notification)

        // Re-schedule the NEXT alarm in the series
        MedicationScheduler.scheduleNext(
            context, petId, "", reminderId, freqType, freqValue
        )
    }
}

