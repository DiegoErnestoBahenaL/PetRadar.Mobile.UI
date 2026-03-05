package com.example.petradar.utils

import android.content.Context
import androidx.core.content.edit

/**
 * Local store for pet photo URIs.
 *
 * The PetRadar API does not expose an endpoint for uploading pet photos
 * (the `photoURL` field in `UserPetViewModel` is read-only and cannot be
 * modified via `UserPetUpdateModel`). Therefore, photos chosen by the user
 * are saved locally in [android.content.SharedPreferences],
 * using the pet ID as the key.
 *
 * The stored URI is the content URI on the device (content://...) that
 * points to the image selected from the user's gallery or camera.
 *
 * It is an `object` (singleton) to ensure centralised access from anywhere in the app.
 *
 * Known limitation: if the user uninstalls and reinstalls the app, or clears
 * app storage, locally saved photos are lost.
 */
object PetPhotoStore {

    /** Name of the SharedPreferences file where URIs are stored. */
    private const val PREFS = "pet_photos"

    /**
     * Saves the URI of a pet's photo.
     *
     * @param context   App or Activity context.
     * @param petId     Pet ID; used as the key in SharedPreferences.
     * @param uriString Content URI of the image (e.g. "content://media/...").
     */
    fun save(context: Context, petId: Long, uriString: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(petId.toString(), uriString) }
    }

    /**
     * Retrieves the previously saved URI for a pet's photo.
     *
     * @param context App or Activity context.
     * @param petId   Pet ID.
     * @return The URI as a String, or null if no photo has been saved for this pet.
     */
    fun get(context: Context, petId: Long): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(petId.toString(), null)

    /**
     * Deletes the photo URI associated with a pet.
     * Should be called when a pet is deleted to avoid orphaned data.
     *
     * @param context App or Activity context.
     * @param petId   ID of the pet whose photo should be removed.
     */
    fun delete(context: Context, petId: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { remove(petId.toString()) }
    }
}
