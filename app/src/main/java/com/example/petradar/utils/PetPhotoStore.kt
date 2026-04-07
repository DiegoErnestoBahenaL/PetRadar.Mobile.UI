package com.example.petradar.utils

import android.content.Context
import androidx.core.content.edit
import androidx.core.net.toUri
import java.io.File

/**
 * Local store for pet photo URIs.
 *
 * The PetRadar API does not expose an endpoint for uploading pet photos,
 * so photos chosen by the user are copied into the app's internal storage
 * and the resulting file URI is saved in [android.content.SharedPreferences].
 *
 * It is an `object` (singleton) to ensure centralised access from anywhere in the app.
 *
 * Known limitation: if the user uninstalls and reinstalls the app, or clears
 * app storage, locally saved photos are lost.
 */
object PetPhotoStore {

    /** Name of the SharedPreferences file where URIs are stored. */
    private const val PREFS = "pet_photos"
    private const val PHOTO_DIR = "pet_photos"

    private fun getPhotoFile(context: Context, petId: Long): File =
        File(File(context.filesDir, PHOTO_DIR), "pet_$petId.jpg")

    private fun persistPhotoCopy(context: Context, petId: Long, uriString: String): String {
        val uri = runCatching { uriString.toUri() }.getOrNull() ?: return uriString
        val scheme = uri.scheme?.lowercase()

        if (scheme != "content" && scheme != "file") {
            return uriString
        }

        val targetFile = getPhotoFile(context, petId)
        if (scheme == "file" && targetFile.absolutePath == uri.path) {
            return targetFile.toUri().toString()
        }
        targetFile.parentFile?.mkdirs()

        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return uriString

            targetFile.toUri().toString()
        }.getOrElse { uriString }
    }

    private fun deletePhotoCopy(storedUri: String?) {
        val uri = storedUri?.toUri() ?: return
        if (uri.scheme != "file") return

        val filePath = uri.path ?: return
        runCatching { File(filePath).delete() }
    }

    /**
     * Saves the URI of a pet's photo.
     *
     * @param context   App or Activity context.
     * @param petId     Pet ID; used as the key in SharedPreferences.
     * @param uriString Content URI of the image (e.g. "content://media/...").
     */
    fun save(context: Context, petId: Long, uriString: String) {
        val persistedUri = persistPhotoCopy(context, petId, uriString)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(petId.toString(), persistedUri) }
    }

    /**
     * Retrieves the previously saved URI for a pet's photo.
     *
     * @param context App or Activity context.
     * @param petId   Pet ID.
     * @return The URI as a String, or null if no photo has been saved for this pet.
     */
    fun get(context: Context, petId: Long): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(petId.toString(), null) ?: return null

        val normalized = persistPhotoCopy(context, petId, stored)
        if (normalized != stored) {
            prefs.edit { putString(petId.toString(), normalized) }
        }

        return normalized
    }

    /**
     * Deletes the photo URI associated with a pet.
     * Should be called when a pet is deleted to avoid orphaned data.
     *
     * @param context App or Activity context.
     * @param petId   ID of the pet whose photo should be removed.
     */
    fun delete(context: Context, petId: Long) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        deletePhotoCopy(prefs.getString(petId.toString(), null))
        prefs
            .edit { remove(petId.toString()) }
    }
}
