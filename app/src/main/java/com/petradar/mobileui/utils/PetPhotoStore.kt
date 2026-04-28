package com.petradar.mobileui.utils

import android.content.Context
import androidx.core.content.edit

/**
 * Store para URIs de fotos de mascotas en SharedPreferences.
 *
 * Las fotos se suben a la API vía PUT /api/UserPets/{id}/mainpicture y se sirven
 * desde GET /api/UserPets/{id}/mainpicture. No se almacena ninguna copia local
 * para evitar conflictos con los datos de la API.
 */
object PetPhotoStore {

    private const val PREFS = "pet_photos"

    fun save(context: Context, petId: Long, uriString: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(petId.toString(), uriString) }
    }

    fun get(context: Context, petId: Long): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(petId.toString(), null)

    fun delete(context: Context, petId: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { remove(petId.toString()) }
    }
}
