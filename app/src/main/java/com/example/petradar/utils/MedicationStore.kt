package com.example.petradar.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.edit

/**
 * Local store for pet medication reminders.
 *
 * Persists a list of [MedicationReminder] objects per pet in SharedPreferences
 * as a JSON array. No server interaction — entirely local.
 */
object MedicationStore {

    private const val PREFS = "pet_medications"

    /** Returns all reminders for [petId]. */
    fun getAll(context: Context, petId: Long): List<MedicationReminder> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(petId.toString(), null) ?: return emptyList()
        return runCatching { parseList(json) }.getOrDefault(emptyList())
    }

    /** Replaces the full list of reminders for [petId]. */
    fun saveAll(context: Context, petId: Long, reminders: List<MedicationReminder>) {
        val json = serializeList(reminders)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(petId.toString(), json) }
    }

    /** Adds a single reminder to the list for [petId]. */
    fun add(context: Context, petId: Long, reminder: MedicationReminder) {
        val current = getAll(context, petId).toMutableList()
        current.add(reminder)
        saveAll(context, petId, current)
    }

    /** Updates (replaces by id) a reminder in the list for [petId]. */
    fun update(context: Context, petId: Long, reminder: MedicationReminder) {
        val current = getAll(context, petId).map { if (it.id == reminder.id) reminder else it }
        saveAll(context, petId, current)
    }

    /** Removes a reminder by id from the list for [petId]. */
    fun remove(context: Context, petId: Long, reminderId: Long) {
        val current = getAll(context, petId).filter { it.id != reminderId }
        saveAll(context, petId, current)
    }

    /** Removes ALL reminders for [petId] (e.g. when the pet is deleted). */
    fun deleteAll(context: Context, petId: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { remove(petId.toString()) }
    }

    // ── Serialization ──────────────────────────────────────────────────────────

    private fun serializeList(list: List<MedicationReminder>): String {
        val array = JSONArray()
        list.forEach { r ->
            array.put(JSONObject().apply {
                put("id", r.id)
                put("medicineName", r.medicineName)
                put("hour", r.hour)
                put("minute", r.minute)
                put("frequencyType", r.frequencyType)
                put("frequencyValue", r.frequencyValue)
                put("notes", r.notes)
                put("isActive", r.isActive)
            })
        }
        return array.toString()
    }

    private fun parseList(json: String): List<MedicationReminder> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            MedicationReminder(
                id            = o.getLong("id"),
                medicineName  = o.getString("medicineName"),
                hour          = o.getInt("hour"),
                minute        = o.getInt("minute"),
                frequencyType = o.getString("frequencyType"),
                frequencyValue = o.getInt("frequencyValue"),
                notes         = o.optString("notes", ""),
                isActive      = o.optBoolean("isActive", true)
            )
        }
    }
}

