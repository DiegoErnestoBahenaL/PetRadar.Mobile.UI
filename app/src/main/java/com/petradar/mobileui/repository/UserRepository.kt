package com.petradar.mobileui.repository

import com.petradar.mobileui.api.RetrofitClient
import com.petradar.mobileui.api.models.UpdateProfileRequest
import com.petradar.mobileui.api.models.UserProfile
import okhttp3.MultipartBody
import retrofit2.Response

class UserRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun getUserById(userId: Long): Response<UserProfile> =
        apiService.getUserById(userId)

    suspend fun updateUser(userId: Long, request: UpdateProfileRequest): Response<Unit> =
        apiService.updateUser(userId, request)

    suspend fun getAllUsers(): Response<List<UserProfile>> =
        apiService.getAllUsers()

    /**
     * Uploads a profile picture for the given user.
     * Endpoint: PUT /api/Users/{id}/profilepicture
     *
     * @param userId The user ID.
     * @param filePart Multipart image part with name "file".
     */
    suspend fun uploadProfilePicture(userId: Long, filePart: MultipartBody.Part): Response<Unit> =
        apiService.uploadProfilePicture(userId, filePart)
}
