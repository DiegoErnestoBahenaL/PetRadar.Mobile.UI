package com.example.petradar.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Clase helper para manejar el almacenamiento de datos de autenticación
 */
object AuthManager {

    private const val PREFS_NAME = "PetRadarPrefs"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_name"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Guardar el token de autenticación y refresh token
     */
    fun saveAuthToken(context: Context, token: String?, refreshToken: String? = null) {
        getPrefs(context).edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            apply()
        }
    }

    /**
     * Obtener el token de autenticación
     */
    fun getAuthToken(context: Context): String? {
        return getPrefs(context).getString(KEY_AUTH_TOKEN, null)
    }

    /**
     * Guardar información del usuario
     */
    fun saveUserInfo(context: Context, userId: Long, email: String, name: String) {
        getPrefs(context).edit().apply {
            putLong(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            apply()
        }
    }

    /**
     * Obtener ID del usuario como Long
     */
    fun getUserId(context: Context): Long? {
        val id = getPrefs(context).getLong(KEY_USER_ID, -1L)
        return if (id <= 0L) null else id
    }

    /**
     * Obtener email del usuario
     */
    fun getUserEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_EMAIL, null)
    }

    /**
     * Obtener nombre del usuario
     */
    fun getUserName(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_NAME, null)
    }

    /**
     * Verificar si el usuario está autenticado (token presente)
     */
    fun isAuthenticated(context: Context): Boolean {
        return !getAuthToken(context).isNullOrEmpty()
    }

    /**
     * Cerrar sesión y limpiar datos
     */
    fun logout(context: Context) {
        getPrefs(context).edit { clear() }
    }
}



