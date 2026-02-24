package com.example.petradar.utils

import android.content.Context

/**
 * Stores pet photo URIs locally (SharedPreferences) keyed by petId.
 * Used because the API's UserPetUpdateModel does not expose a photo upload field.
 */
object PetPhotoStore {

    private const val PREFS = "pet_photos"

    fun save(context: Context, petId: Long, uriString: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(petId.toString(), uriString).apply()
    }

    fun get(context: Context, petId: Long): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(petId.toString(), null)

    fun delete(context: Context, petId: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(petId.toString()).apply()
    }
}

