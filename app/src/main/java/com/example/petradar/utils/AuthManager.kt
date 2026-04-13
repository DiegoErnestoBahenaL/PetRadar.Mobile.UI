package com.example.petradar.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Local session manager for PetRadar.
 *
 * Safely stores and retrieves authentication and user data in
 * [SharedPreferences] under the file name `PetRadarPrefs`.
 *
 * It is an `object` (singleton) to guarantee a single instance throughout the app.
 *
 * Data it manages:
 *  - JWT authentication token (required in the Authorization header of every API request).
 *  - Refresh token (used to renew the JWT when it expires, without asking for the password again).
 *  - ID of the authenticated user (Long).
 *  - User email.
 *  - User name.
 *
 * Security note: SharedPreferences in MODE_PRIVATE is only accessible by the same app.
 * For production with sensitive data, EncryptedSharedPreferences from Jetpack Security
 * is recommended.
 */
object AuthManager {

    private const val PREFS_NAME          = "PetRadarPrefs"
    private const val SECURE_PREFS_NAME   = "PetRadarSecurePrefs"

    // Keys for accessing values in SharedPreferences
    private const val KEY_AUTH_TOKEN    = "auth_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_ID       = "user_id"
    private const val KEY_USER_EMAIL    = "user_email"
    private const val KEY_USER_NAME     = "user_name"
    private const val KEY_PROFILE_PHOTO = "profile_photo_url"

    // Keys for encrypted credential storage
    private const val KEY_SAVED_EMAIL    = "saved_email"
    private const val KEY_SAVED_PASSWORD = "saved_password"

    /** Returns the SharedPreferences instance for the app. */
    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns an [EncryptedSharedPreferences] instance backed by Android Keystore.
     * Used to securely store user credentials for silent re-authentication.
     * Returns null if the secure storage cannot be initialized (e.g., device doesn't support it).
     */
    private fun getSecurePrefs(context: Context): SharedPreferences? = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("AuthManager", "Could not open EncryptedSharedPreferences", e)
        null
    }

    /**
     * Saves the JWT and the refresh token after a successful login.
     *
     * @param context      App or Activity context.
     * @param token        Short-lived JWT (included in every API request header).
     * @param refreshToken Long-lived token to renew the JWT (may be null).
     */
    fun saveAuthToken(context: Context, token: String?, refreshToken: String? = null) {
        getPrefs(context).edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            apply()
        }
    }

    /**
     * Returns the currently saved JWT.
     *
     * @return The token as a String, or null if there is no active session.
     */
    fun getAuthToken(context: Context): String? =
        getPrefs(context).getString(KEY_AUTH_TOKEN, null)

    /** Returns the currently saved refresh token. */
    fun getRefreshToken(context: Context): String? =
        getPrefs(context).getString(KEY_REFRESH_TOKEN, null)

    /**
     * Saves basic user information after identifying the user via the API.
     * Called after login to associate the token with the user's profile.
     *
     * @param context App or Activity context.
     * @param userId  Numeric user ID in the API database.
     * @param email   User email (also used as the username in the API).
     * @param name    Full user name to display in the UI.
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
     * Returns the ID of the authenticated user.
     *
     * @return The ID as Long, or null if there is no session or the ID is invalid (≤ 0).
     */
    fun getUserId(context: Context): Long? {
        val id = getPrefs(context).getLong(KEY_USER_ID, -1L)
        return if (id <= 0L) null else id
    }

    /**
     * Returns the email of the authenticated user.
     *
     * @return Email as String, or null if there is no active session.
     */
    fun getUserEmail(context: Context): String? =
        getPrefs(context).getString(KEY_USER_EMAIL, null)

    /**
     * Returns the name of the authenticated user.
     *
     * @return Name as String, or null if there is no active session.
     */
    fun getUserName(context: Context): String? =
        getPrefs(context).getString(KEY_USER_NAME, null)

    /**
     * Saves the profile photo URL returned by the API.
     * Call this after login or after a successful photo upload.
     */
    fun saveProfilePhotoURL(context: Context, url: String?) {
        getPrefs(context).edit { putString(KEY_PROFILE_PHOTO, url) }
    }

    /**
     * Builds the direct API URL for the authenticated user's profile picture.
     * Endpoint: GET /api/Users/{id}/profilepicture
     * Returns null if no userId is stored.
     */
    fun getProfilePictureUrl(context: Context): String? {
        val userId = getUserId(context) ?: return null
        return "${com.example.petradar.api.RetrofitClient.BASE_URL}api/Users/$userId/profilepicture"
    }

    /**
     * Checks whether the user has an active session (a non-empty saved token).
     * Used in [com.example.petradar.LoginActivity] to skip the login screen.
     *
     * @return true if a valid token is saved; false otherwise.
     */
    fun isAuthenticated(context: Context): Boolean {
        val token = getAuthToken(context)
        return !token.isNullOrEmpty() && !JwtUtils.isTokenExpired(token)
    }

    /**
     * Updates the user's name and email stored in SharedPreferences.
     * Call this after a successful profile update so the Home screen reflects
     * the new name immediately without requiring a re-login.
     *
     * @param context  App or Activity context.
     * @param name     New display name (null = keep existing).
     * @param email    New email (null = keep existing).
     */
    fun updateUserInfo(context: Context, name: String? = null, email: String? = null) {
        getPrefs(context).edit {
            name?.let { putString(KEY_USER_NAME, it) }
            email?.let { putString(KEY_USER_EMAIL, it) }
        }
    }

    /**
     * Saves the user's credentials encrypted in the Android Keystore-backed store.
     * Call this after every successful login so silent re-authentication is possible
     * even when both the JWT and refresh token have expired.
     *
     * The data is protected by AES-256-GCM and is only readable by this app.
     *
     * @param context  App or Activity context.
     * @param email    User email.
     * @param password Plain-text password (encrypted at rest by EncryptedSharedPreferences).
     */
    fun saveCredentials(context: Context, email: String, password: String) {
        getSecurePrefs(context)?.edit {
            putString(KEY_SAVED_EMAIL, email)
            putString(KEY_SAVED_PASSWORD, password)
        }
    }

    /**
     * Returns the email saved by [saveCredentials], or null if none is stored.
     */
    fun getSavedEmail(context: Context): String? =
        getSecurePrefs(context)?.getString(KEY_SAVED_EMAIL, null)

    /**
     * Returns the password saved by [saveCredentials], or null if none is stored.
     */
    fun getSavedPassword(context: Context): String? =
        getSecurePrefs(context)?.getString(KEY_SAVED_PASSWORD, null)

    /**
     * Signs out the user by clearing all data stored in SharedPreferences.
     * After calling this method, [isAuthenticated] will return false.
     *
     * @param context App or Activity context.
     */
    fun logout(context: Context) {
        getPrefs(context).edit { clear() }
        getSecurePrefs(context)?.edit { clear() }
    }
}
