package com.example.petradar.api.models

import com.google.gson.annotations.SerializedName

/**
 * Modelo de usuario (UserViewModel en Swagger)
 */
data class UserProfile(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("email")
    val email: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("lastName")
    val lastName: String? = null,

    @SerializedName("phoneNumber")
    val phoneNumber: String? = null,

    @SerializedName("profilePhotoURL")
    val profilePhotoURL: String? = null,

    @SerializedName("role")
    val role: String? = null,

    @SerializedName("organizationName")
    val organizationName: String? = null,

    @SerializedName("organizationAddress")
    val organizationAddress: String? = null,

    @SerializedName("organizationPhone")
    val organizationPhone: String? = null
)

/**
 * Modelo para actualizar usuario (UserUpdateModel en Swagger)
 */
data class UpdateProfileRequest(
    @SerializedName("email")
    val email: String? = null,

    @SerializedName("password")
    val password: String? = null,

    @SerializedName("name")
    val name: String? = null,

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
    val role: String? = null
)



