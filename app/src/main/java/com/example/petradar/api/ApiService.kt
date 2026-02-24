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

    // ========== Endpoints de Citas Veterinarias ==========

    /** GET /api/VeterinaryAppointments/user/{userId} */
    @GET("api/VeterinaryAppointments/user/{userId}")
    suspend fun getAppointmentsByUserId(@Path("userId") userId: Long): Response<List<VeterinaryAppointmentViewModel>>

    /** GET /api/VeterinaryAppointments/pet/{petId} */
    @GET("api/VeterinaryAppointments/pet/{petId}")
    suspend fun getAppointmentsByPetId(@Path("petId") petId: Long): Response<List<VeterinaryAppointmentViewModel>>

    /** GET /api/VeterinaryAppointments/{id} */
    @GET("api/VeterinaryAppointments/{id}")
    suspend fun getAppointmentById(@Path("id") id: Long): Response<VeterinaryAppointmentViewModel>

    /** POST /api/VeterinaryAppointments */
    @POST("api/VeterinaryAppointments")
    suspend fun createAppointment(@Body request: VeterinaryAppointmentCreateModel): Response<Unit>

    /** PUT /api/VeterinaryAppointments/{id} */
    @PUT("api/VeterinaryAppointments/{id}")
    suspend fun updateAppointment(
        @Path("id") id: Long,
        @Body request: VeterinaryAppointmentUpdateModel
    ): Response<Unit>

    /** DELETE /api/VeterinaryAppointments/{id} */
    @DELETE("api/VeterinaryAppointments/{id}")
    suspend fun deleteAppointment(@Path("id") id: Long): Response<Unit>
}

// ========== Modelos de Citas Veterinarias ==========

data class VeterinaryAppointmentViewModel(
    val id: Long,
    val petId: Long,
    val veterinaryName: String?,
    val appointmentType: String?,   // Checkup | Vaccination | Surgery | Grooming | Consultation | Other
    val appointmentStatus: String?, // Scheduled | Cancelled
    val appointmentDate: String,    // ISO-8601
    val durationInMinutes: Int?,
    val reasonForVisit: String?,
    val notes: String?,
    val diagnosis: String?,
    val treatment: String?,
    val prescriptions: String?,
    val cost: Double?,
    val addressText: String?,
    val reminderSent: Boolean
)

data class VeterinaryAppointmentCreateModel(
    val petId: Long,
    val veterinaryName: String? = null,
    val appointmentType: String,   // required enum
    val appointmentStatus: String, // required enum
    val appointmentDate: String,   // ISO-8601 datetime
    val durationInMinutes: Int? = null,
    val reasonForVisit: String,
    val notes: String? = null,
    val diagnosis: String? = null,
    val treatment: String? = null,
    val prescriptions: String? = null,
    val cost: Double? = null,
    val addressText: String? = null
)

data class VeterinaryAppointmentUpdateModel(
    val veterinaryName: String? = null,
    val appointmentType: String? = null,
    val appointmentStatus: String? = null,
    val appointmentDate: String? = null,
    val durationInMinutes: Int? = null,
    val reasonForVisit: String? = null,
    val notes: String? = null,
    val diagnosis: String? = null,
    val treatment: String? = null,
    val prescriptions: String? = null,
    val cost: Double? = null,
    val addressText: String? = null
)

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


