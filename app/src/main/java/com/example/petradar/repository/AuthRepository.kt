package com.example.petradar.repository

import com.example.petradar.api.RetrofitClient
import com.example.petradar.api.models.LoginRequest
import com.example.petradar.api.models.LoginResponse
import com.example.petradar.api.models.RegisterRequest
import retrofit2.Response

/**
 * Repository para manejar la autenticación
 */
class AuthRepository {

    private val apiService = RetrofitClient.apiService

    /**
     * Iniciar sesión con username y password
     * Endpoint: POST /api/gate/Login
     */
    suspend fun login(username: String, password: String): Response<LoginResponse> {
        val request = LoginRequest(username, password)
        return apiService.login(request)
    }

    /**
     * Registrar nuevo usuario
     * Endpoint: POST /api/Users
     * Nota: La API no devuelve token directamente, hay que hacer login después
     */
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
            role = "User"
        )
        return apiService.createUser(request)
    }
}


