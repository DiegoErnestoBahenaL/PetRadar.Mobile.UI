package com.example.petradar.repository

import com.example.petradar.api.RetrofitClient
import com.example.petradar.api.models.UpdateProfileRequest
import com.example.petradar.api.models.UserProfile
import retrofit2.Response

class UserRepository {

    private val apiService = RetrofitClient.apiService

    /**
     * Obtener usuario por ID
     * Endpoint: GET /api/Users/{id}
     */
    suspend fun getUserById(userId: Long): Response<UserProfile> {
        return apiService.getUserById(userId)
    }

    /**
     * Actualizar usuario
     * Endpoint: PUT /api/Users/{id}
     */
    suspend fun updateUser(userId: Long, request: UpdateProfileRequest): Response<Unit> {
        return apiService.updateUser(userId, request)
    }

    /**
     * Obtener todos los usuarios
     * Endpoint: GET /api/Users
     */
    suspend fun getAllUsers(): Response<List<UserProfile>> {
        return apiService.getAllUsers()
    }
}



