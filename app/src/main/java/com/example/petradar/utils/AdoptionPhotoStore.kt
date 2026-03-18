package com.example.petradar.utils

import android.content.Context
import androidx.core.content.edit

/**
 * Local store for adoption animal photo URIs.
 *
 * Works identically to [PetPhotoStore] but uses a separate SharedPreferences
 * file (`adoption_photos`) to avoid key collisions between user pets and
 * adoption animals.
 *
 * The stored URI is the content URI on the device (content://...) that
 * points to the image selected from the gallery or taken with the camera.
 */
object AdoptionPhotoStore {

    /** Name of the SharedPreferences file where URIs are stored. */
    private const val PREFS = "adoption_photos"

    /**
     * Saves the URI of an adoption animal's photo.
     *
     * @param context   App or Activity context.
     * @param animalId  Adoption animal ID; used as the key in SharedPreferences.
     * @param uriString Content URI of the image.
     */
    fun save(context: Context, animalId: Long, uriString: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(animalId.toString(), uriString) }
    }

    /**
     * Retrieves the previously saved URI for an adoption animal's photo.
     *
     * @param context  App or Activity context.
     * @param animalId Adoption animal ID.
     * @return The URI as a String, or null if no photo has been saved.
     */
    fun get(context: Context, animalId: Long): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(animalId.toString(), null)

    /**
     * Deletes the photo URI associated with an adoption animal.
     *
     * @param context  App or Activity context.
     * @param animalId ID of the animal whose photo should be removed.
     */
    fun delete(context: Context, animalId: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { remove(animalId.toString()) }
    }
}

