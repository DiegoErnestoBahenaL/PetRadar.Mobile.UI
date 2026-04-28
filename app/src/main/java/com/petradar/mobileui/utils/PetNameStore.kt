package com.petradar.mobileui.utils

import android.content.Context
import androidx.core.content.edit

/**
 * Simple key-value store that persists the display name of a pet locally
 * so that notification receivers can look it up without a network call.
 */
object PetNameStore {

    private const val PREFS = "pet_names"

    /** Saves (or updates) the name for [petId]. */
    fun save(context: Context, petId: Long, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(petId.toString(), name) }
    }

    /** Returns the stored name for [petId], or null if not found. */
    fun get(context: Context, petId: Long): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(petId.toString(), null)

    /** Removes the entry for [petId] (e.g. when the pet is deleted). */
    fun remove(context: Context, petId: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { remove(petId.toString()) }
    }
}

