package com.petradar.mobileui.repository

import com.petradar.mobileui.api.RetrofitClient
import com.petradar.mobileui.api.models.LoginRequest
import com.petradar.mobileui.api.models.LoginResponse
import com.petradar.mobileui.api.models.RecoverPasswordRequest
import com.petradar.mobileui.api.models.RefreshTokenRequest
import com.petradar.mobileui.api.models.RegisterRequest
import retrofit2.Response

/**
 * Repository that centralises all authentication operations with the API.
 *
 * Follows the Repository pattern: acts as the intermediary between ViewModels
 * and the remote data source ([RetrofitClient.apiService]).
 * ViewModels have no knowledge of API details; they only call methods on this repository.
 *
 * All functions are `suspend` and must be called from a coroutine.
 */
class AuthRepository {

    /** Reference to the Retrofit service obtained from the [RetrofitClient] singleton. */
    private val apiService = RetrofitClient.apiService

    /**
     * Signs in using the provided credentials.
     * Endpoint: POST /api/gate/Login
     *
     * In PetRadar the `username` field in the API corresponds to the user's email.
     *
     * @param username User's email.
     * @param password User's password.
     * @return [Response] with [LoginResponse] (token + refreshToken) on success,
     *         or with code 401 if the credentials are invalid.
     */
    suspend fun login(username: String, password: String): Response<LoginResponse> {
        val request = LoginRequest(username, password)
        return apiService.login(request)
    }

    /**
     * Renews the JWT using the saved refresh token.
     * Endpoint: POST /api/gate/Login/refresh
     *
     * Call this when [com.petradar.mobileui.utils.AuthManager.isAuthenticated] returns false
     * but a refresh token is still available, to avoid forcing the user to re-enter credentials.
     *
     * @param token The refresh token previously stored in [com.petradar.mobileui.utils.AuthManager].
     * @return [Response] with a new [LoginResponse] (new JWT + new refresh token) on success,
     *         or a 4xx/5xx code if the refresh token is invalid or expired.
     */
    suspend fun refreshToken(token: String): Response<LoginResponse> {
        return apiService.refreshToken(RefreshTokenRequest(token))
    }

    /**
     * Registers a new user in the system.
     * Endpoint: POST /api/Users
     *
     * Important note: In the QA environment this endpoint may require admin authentication.
     * If it returns 401, registration is not possible without an administrator
     * creating the account manually.
     *
     * Registration does not return a token; after a successful registration (201 Created)
     * the flow continues with an automatic login in [com.petradar.mobileui.RegisterActivity].
     *
     * @param name        User first name (required).
     * @param lastName    Last name (optional).
     * @param email       Unique email (required).
     * @param password    Password (required).
     * @param phoneNumber Contact phone number (optional).
     * @return [Response] with Unit; HTTP 201 on success.
     */
    /**
     * Sends a password-recovery email via POST /api/gate/Login/recoverpassword.
     *
     * @param email Email of the account whose password must be reset.
     * @return [Response] with Unit; HTTP 200 on success, 404 if the email is unknown.
     */
    suspend fun recoverPassword(email: String): Response<Unit> =
        apiService.recoverPassword(RecoverPasswordRequest(email))

    suspend fun register(
        name: String,
        lastName: String?,
        email: String,
        password: String,
        phoneNumber: String? = null
    ): Response<Unit> {
        val request = RegisterRequest(
            email = email,
            password = password,
            name = name,
            lastName = lastName,
            phoneNumber = phoneNumber,
            role = "User" // Users registering through the app are always assigned the "User" role.
        )
        return apiService.createUser(request)
    }
}
