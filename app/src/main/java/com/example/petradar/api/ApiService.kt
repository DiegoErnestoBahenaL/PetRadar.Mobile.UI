package com.example.petradar.api

import com.example.petradar.api.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ========== Endpoints de Autenticación ==========

    /**
     * Login - POST /api/gate/Login
     */
    @POST("api/gate/Login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /**
     * Refresh Token - POST /api/gate/Login/refresh
     */
    @POST("api/gate/Login/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<LoginResponse>

    // ========== Endpoints de Usuarios ==========

    /**
     * Crear Usuario (Registro) - POST /api/Users
     */
    @POST("api/Users")
    suspend fun createUser(@Body request: RegisterRequest): Response<Unit>

    /**
     * Obtener todos los usuarios - GET /api/Users
     */
    @GET("api/Users")
    suspend fun getAllUsers(): Response<List<UserProfile>>

    /**
     * Obtener usuario por ID - GET /api/Users/{id}
     */
    @GET("api/Users/{id}")
    suspend fun getUserById(@Path("id") userId: Long): Response<UserProfile>

    /**
     * Actualizar usuario - PUT /api/Users/{id}
     */
    @PUT("api/Users/{id}")
    suspend fun updateUser(
        @Path("id") userId: Long,
        @Body request: UpdateProfileRequest
    ): Response<Unit>

    /**
     * Eliminar usuario - DELETE /api/Users/{id}
     */
    @DELETE("api/Users/{id}")
    suspend fun deleteUser(@Path("id") userId: Long): Response<Unit>

    // ========== Endpoints de Mascotas ==========

    /**
     * Obtener mascotas del usuario actual - GET /api/UserPets
     */
    @GET("api/UserPets")
    suspend fun getUserPets(): Response<List<UserPetViewModel>>

    /**
     * Obtener mascotas por userId - GET /api/UserPets/user/{userId}
     */
    @GET("api/UserPets/user/{userId}")
    suspend fun getUserPetsByUserId(@Path("userId") userId: Long): Response<List<UserPetViewModel>>

    /**
     * Obtener mascota por ID - GET /api/UserPets/{id}
     */
    @GET("api/UserPets/{id}")
    suspend fun getUserPetById(@Path("id") petId: Long): Response<UserPetViewModel>

    /**
     * Crear mascota - POST /api/UserPets
     */
    @POST("api/UserPets")
    suspend fun createUserPet(@Body request: UserPetCreateModel): Response<Unit>

    /**
     * Actualizar mascota - PUT /api/UserPets/{id}
     */
    @PUT("api/UserPets/{id}")
    suspend fun updateUserPet(
        @Path("id") petId: Long,
        @Body request: UserPetUpdateModel
    ): Response<Unit>

    /**
     * Eliminar mascota - DELETE /api/UserPets/{id}
     */
    @DELETE("api/UserPets/{id}")
    suspend fun deleteUserPet(@Path("id") petId: Long): Response<Unit>
}

// ========== Modelos de Mascotas ==========

data class UserPetViewModel(
    val id: Long,
    val userId: Long,
    val name: String?,
    val species: String?,
    val breed: String?,
    val color: String?,
    val sex: String?,
    val size: String?,
    val birthDate: String?,
    val approximateAge: Double?,
    val weight: Double?,
    val description: String?,
    val photoURL: String?,
    val additionalPhotosURL: String?,
    val isNeutered: Boolean?,
    val allergies: String?,
    val medicalNotes: String?
)

data class UserPetCreateModel(
    val userId: Long,
    val name: String,
    val species: String, // "Dog" or "Cat"
    val breed: String? = null,
    val color: String? = null,
    val sex: String? = null, // "Male", "Female", "Unknown"
    val size: String? = null, // "Small", "Medium", "Large"
    val birthDate: String? = null,
    val approximateAge: Double? = null,
    val weight: Double? = null,
    val description: String? = null,
    val isNeutered: Boolean? = null,
    val allergies: String? = null,
    val medicalNotes: String? = null
)

data class UserPetUpdateModel(
    val name: String? = null,
    val species: String? = null,
    val breed: String? = null,
    val color: String? = null,
    val sex: String? = null,
    val size: String? = null,
    val birthDate: String? = null,
    val approximateAge: Double? = null,
    val weight: Double? = null,
    val description: String? = null,
    val isNeutered: Boolean? = null,
    val allergies: String? = null,
    val medicalNotes: String? = null
)


