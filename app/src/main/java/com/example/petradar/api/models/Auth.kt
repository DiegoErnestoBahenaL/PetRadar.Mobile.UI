package com.example.petradar.api.models

import com.google.gson.annotations.SerializedName

/**
 * Modelo para la petición de login (LoginModel en Swagger)
 */
data class LoginRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String
)

/**
 * Modelo para la respuesta de login (UserTokenViewModel en Swagger)
 */
data class LoginResponse(
    @SerializedName("token")
    val token: String?,

    @SerializedName("tokenValidTo")
    val tokenValidTo: String?,

    @SerializedName("refreshToken")
    val refreshToken: String?,

    @SerializedName("refreshTokenExpiryTime")
    val refreshTokenExpiryTime: String?
)

/**
 * Modelo para refrescar token (RefreshTokenFromUiModel en Swagger)
 */
data class RefreshTokenRequest(
    @SerializedName("refreshToken")
    val refreshToken: String
)

/**
 * Modelo para la petición de registro (UserCreateModel en Swagger)
 */
data class RegisterRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("lastName")
    val lastName: String? = null,

    @SerializedName("phoneNumber")
    val phoneNumber: String? = null,

    @SerializedName("organizationName")
    val organizationName: String? = null,

    @SerializedName("organizationAddress")
    val organizationAddress: String? = null,

    @SerializedName("organizationPhone")
    val organizationPhone: String? = null,

    @SerializedName("role")
    val role: String = "User"
)

/**
 * Modelo genérico para respuestas de error (ProblemDetails en Swagger)
 */
data class ApiError(
    @SerializedName("type")
    val type: String?,

    @SerializedName("title")
    val title: String?,

    @SerializedName("status")
    val status: Int?,

    @SerializedName("detail")
    val detail: String?,

    @SerializedName("instance")
    val instance: String?
)



