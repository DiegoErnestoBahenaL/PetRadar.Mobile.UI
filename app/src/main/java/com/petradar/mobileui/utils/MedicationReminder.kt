package com.petradar.mobileui.utils

/**
 * Represents a single medication reminder stored locally for a pet.
 *
 * @param id            Unique ID for this reminder (used as alarm request code base).
 * @param medicineName  Name of the medicine (e.g. "Frontline", "Amoxicillin").
 * @param hour          Hour of day to send the notification (0–23).
 * @param minute        Minute of the hour to send the notification (0–59).
 * @param frequencyType How the repeat interval is measured: "hours" or "days".
 * @param frequencyValue How many hours or days between doses (e.g. 8 hours, 1 day).
 * @param notes         Optional extra notes (e.g. "give with food").
 * @param isActive      Whether this reminder is currently active.
 */
data class MedicationReminder(
    val id: Long = System.currentTimeMillis(),
    val medicineName: String,
    val hour: Int,
    val minute: Int,
    val frequencyType: String,   // "hours" or "days"
    val frequencyValue: Int,
    val notes: String = "",
    val isActive: Boolean = true
)

