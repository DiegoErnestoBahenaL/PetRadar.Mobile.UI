package com.example.petradar.api

import com.example.petradar.api.models.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface that declares all PetRadar API endpoints.
 *
 * Retrofit generates a concrete implementation of this interface at compile time.
 * Each `suspend` function can be called from a coroutine and returns [Response<T>],
 * allowing inspection of the HTTP code and body for both success and error scenarios.
 *
 * The base URL is configured in [RetrofitClient]. Paths here are relative to it.
 */
interface ApiService {

    // =========================================================================
    // Authentication  →  /api/gate/Login
    // =========================================================================

    /**
     * Signs in with username and password.
     * Endpoint: POST /api/gate/Login
     *
     * @param request User credentials ([LoginRequest]).
     * @return [LoginResponse] with the JWT and refresh token if credentials are valid.
     *         HTTP 401 if credentials are incorrect.
     */
    @POST("api/gate/Login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /**
     * Renews the session token using the refresh token.
     * Endpoint: POST /api/gate/Login/refresh
     *
     * Reserved for future use: when automatic JWT renewal on expiry is implemented,
     * instead of asking the user to sign in again.
     *
     * @param request Object with the current refresh token ([RefreshTokenRequest]).
     * @return New token/refresh token pair.
     */
    @POST("api/gate/Login/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<LoginResponse>

    // =========================================================================
    // Users  →  /api/Users
    // =========================================================================

    /**
     * Creates (registers) a new user in the system.
     * Endpoint: POST /api/Users
     * Note: In the QA environment this endpoint may require admin authentication.
     *
     * @param request New user data ([RegisterRequest]).
     * @return HTTP 201 Created with no body on success.
     */
    @POST("api/Users")
    suspend fun createUser(@Body request: RegisterRequest): Response<Unit>

    /**
     * Retrieves the full list of users (requires a role with permissions).
     * Endpoint: GET /api/Users
     * Used internally to look up the userId of the recently authenticated user by email.
     */
    @GET("api/Users")
    suspend fun getAllUsers(): Response<List<UserProfile>>

    /**
     * Retrieves a user's profile by their ID.
     * Endpoint: GET /api/Users/{id}
     *
     * @param userId ID of the user to query.
     * @return [UserProfile] with the user's data. HTTP 404 if not found.
     */
    @GET("api/Users/{id}")
    suspend fun getUserById(@Path("id") userId: Long): Response<UserProfile>

    /**
     * Updates an existing user's data.
     * Endpoint: PUT /api/Users/{id}
     *
     * @param userId ID of the user to update.
     * @param request Fields to modify ([UpdateProfileRequest]); null fields are ignored.
     * @return HTTP 204 No Content on success.
     */
    @PUT("api/Users/{id}")
    suspend fun updateUser(
        @Path("id") userId: Long,
        @Body request: UpdateProfileRequest
    ): Response<Unit>

    @PUT("api/Users/{id}/fcm-token")
    suspend fun updateFcmToken(
        @Path("id") userId: Long,
        @Body request: UpdateFcmTokenRequest
    ): Response<Unit>

    /**
     * Deletes a user by their ID.
     * Endpoint: DELETE /api/Users/{id}
     *
     * Reserved for future use: account deletion from profile settings.
     *
     * @param userId ID of the user to delete.
     * @return HTTP 204 No Content on success.
     */
    @DELETE("api/Users/{id}")
    suspend fun deleteUser(@Path("id") userId: Long): Response<Unit>

    /**
     * Uploads or replaces the profile picture of a user.
     * Endpoint: PUT /api/Users/{id}/profilepicture
     *
     * The image is sent as multipart/form-data with the field name "file".
     * Returns HTTP 201 on success.
     *
     * @param userId ID of the user whose picture to update.
     * @param file   Image file part (JPEG or PNG).
     */
    @Multipart
    @PUT("api/Users/{id}/profilepicture")
    suspend fun uploadProfilePicture(
        @Path("id") userId: Long,
        @Part file: MultipartBody.Part
    ): Response<Unit>

    // =========================================================================
    // Pets  →  /api/UserPets
    // =========================================================================

    /**
     * Retrieves the authenticated user's pets (without needing an explicit userId).
     * Endpoint: GET /api/UserPets
     *
     * Reserved for future use; currently [getUserPetsByUserId] is used because
     * the userId is already available in [com.example.petradar.utils.AuthManager].
     */
    @GET("api/UserPets")
    suspend fun getUserPets(): Response<List<UserPetViewModel>>

    /**
     * Retrieves the pets of a specific user by their ID.
     * Endpoint: GET /api/UserPets/user/{userId}
     *
     * @param userId ID of the pet owner.
     */
    @GET("api/UserPets/user/{userId}")
    suspend fun getUserPetsByUserId(@Path("userId") userId: Long): Response<List<UserPetViewModel>>

    /**
     * Retrieves the details of a pet by its ID.
     * Endpoint: GET /api/UserPets/{id}
     *
     * @param petId Pet ID. HTTP 404 if not found.
     */
    @GET("api/UserPets/{id}")
    suspend fun getUserPetById(@Path("id") petId: Long): Response<UserPetViewModel>

    /**
     * Creates a new pet associated with a user.
     * Endpoint: POST /api/UserPets
     *
     * @param request New pet data ([UserPetCreateModel]).
     * @return HTTP 201 Created with no body.
     */
    @POST("api/UserPets")
    suspend fun createUserPet(@Body request: UserPetCreateModel): Response<Unit>

    /**
     * Updates an existing pet's data.
     * Endpoint: PUT /api/UserPets/{id}
     *
     * @param petId ID of the pet to update.
     * @param request Fields to modify ([UserPetUpdateModel]); null fields are omitted.
     * @return HTTP 204 No Content on success.
     */
    @PUT("api/UserPets/{id}")
    suspend fun updateUserPet(
        @Path("id") petId: Long,
        @Body request: UserPetUpdateModel
    ): Response<Unit>

    /**
     * Deletes a pet by its ID.
     * Endpoint: DELETE /api/UserPets/{id}
     *
     * @param petId ID of the pet to delete.
     * @return HTTP 204 No Content on success.
     */
    @DELETE("api/UserPets/{id}")
    suspend fun deleteUserPet(@Path("id") petId: Long): Response<Unit>

    /**
     * Uploads or replaces the main picture of a pet.
     * Endpoint: PUT /api/UserPets/{id}/mainpicture
     *
     * The image is sent as multipart/form-data with the field name "file".
     */
    @Multipart
    @PUT("api/UserPets/{id}/mainpicture")
    suspend fun uploadPetMainPicture(
        @Path("id") petId: Long,
        @Part file: MultipartBody.Part
    ): Response<Unit>

    /**
     * Retrieves all additional photo names for a pet.
     * Endpoint: GET /api/UserPets/{id}/additionalphotos
     */
    @GET("api/UserPets/{id}/additionalphotos")
    suspend fun getUserPetAdditionalPhotos(@Path("id") id: Long): Response<List<String>>

    /**
     * Uploads one or more additional photos for a pet.
     * Endpoint: PUT /api/UserPets/{id}/additionalphotos
     */
    @Multipart
    @PUT("api/UserPets/{id}/additionalphotos")
    suspend fun uploadUserPetAdditionalPhotos(
        @Path("id") id: Long,
        @Part files: List<MultipartBody.Part>
    ): Response<Unit>

    /**
     * Deletes a specific additional photo of a pet.
     * Endpoint: DELETE /api/UserPets/{id}/additionalphotos/{photoName}
     */
    @DELETE("api/UserPets/{id}/additionalphotos/{photoName}")
    suspend fun deleteUserPetAdditionalPhoto(
        @Path("id") id: Long,
        @Path("photoName") photoName: String
    ): Response<Unit>

    // =========================================================================
    // Reports  ->  /api/Reports
    // =========================================================================

    /**
     * Creates a new report (e.g., lost pet report).
     * Endpoint: POST /api/Reports
     */
    @POST("api/Reports")
    suspend fun createReport(@Body request: ReportCreateModel): Response<Unit>

    /**
     * Uploads or replaces the main picture of a report.
     * Endpoint: PUT /api/Reports/{id}/mainpicture
     *
     * The image is sent as multipart/form-data with the field name "file".
     */
    @Multipart
    @PUT("api/Reports/{id}/mainpicture")
    suspend fun uploadReportMainPicture(
        @Path("id") reportId: Long,
        @Part file: MultipartBody.Part
    ): Response<Unit>

    /**
     * Retrieves all reports created by a specific user.
     * Endpoint: GET /api/Reports/user/{userId}
     */
    @GET("api/Reports/user/{userId}")
    suspend fun getReportsByUserId(@Path("userId") userId: Long): Response<List<ReportViewModel>>

    /**
     * Downloads a file from an arbitrary URL using the authenticated OkHttp client.
     * Used internally to copy pet photos to a report.
     */
    @GET
    suspend fun downloadFile(@Url url: String): Response<ResponseBody>

    /**
     * Retrieves all additional photo names for a report.
     * Endpoint: GET /api/Reports/{id}/additionalphotos
     */
    @GET("api/Reports/{id}/additionalphotos")
    suspend fun getReportAdditionalPhotos(@Path("id") id: Long): Response<List<String>>

    /**
     * Uploads one or more additional photos for a report.
     * Endpoint: PUT /api/Reports/{id}/additionalphotos
     */
    @Multipart
    @PUT("api/Reports/{id}/additionalphotos")
    suspend fun uploadReportAdditionalPhotos(
        @Path("id") id: Long,
        @Part files: List<MultipartBody.Part>
    ): Response<Unit>

    /**
     * Deletes a specific additional photo of a report.
     * Endpoint: DELETE /api/Reports/{id}/additionalphotos/{photoName}
     */
    @DELETE("api/Reports/{id}/additionalphotos/{photoName}")
    suspend fun deleteReportAdditionalPhoto(
        @Path("id") id: Long,
        @Path("photoName") photoName: String
    ): Response<Unit>

    /**
     * Retrieves the details of a report by its ID.
     * Endpoint: GET /api/Reports/{id}
     *
     * @param id Report ID. HTTP 404 if not found.
     */
    @GET("api/Reports/{id}")
    suspend fun getReportById(@Path("id") id: Long): Response<ReportViewModel>

    /**
     * Updates an existing report.
     * Endpoint: PUT /api/Reports/{id}
     *
     * @param id      Report ID to update.
     * @param request Fields to modify ([ReportUpdateModel]); null fields are omitted.
     * @return HTTP 204 No Content on success.
     */
    @PUT("api/Reports/{id}")
    suspend fun updateReport(
        @Path("id") id: Long,
        @Body request: ReportUpdateModel
    ): Response<Unit>

    /**
     * Deletes a report by its ID.
     * Endpoint: DELETE /api/Reports/{id}
     *
     * @param id Report ID to delete.
     * @return HTTP 204 No Content on success.
     */
    @DELETE("api/Reports/{id}")
    suspend fun deleteReport(@Path("id") id: Long): Response<Unit>

    // =========================================================================
    // Veterinary Appointments  →  /api/VeterinaryAppointments
    // =========================================================================

    /**
     * Retrieves all appointments for a user (grouped by their pets).
     * Endpoint: GET /api/VeterinaryAppointments/user/{userId}
     *
     * @param userId ID of the pet owner linked to the appointments.
     */
    @GET("api/VeterinaryAppointments/user/{userId}")
    suspend fun getAppointmentsByUserId(@Path("userId") userId: Long): Response<List<VeterinaryAppointmentViewModel>>

    /**
     * Retrieves the appointments for a specific pet.
     * Endpoint: GET /api/VeterinaryAppointments/pet/{petId}
     *
     * @param petId Pet ID.
     */
    @GET("api/VeterinaryAppointments/pet/{petId}")
    suspend fun getAppointmentsByPetId(@Path("petId") petId: Long): Response<List<VeterinaryAppointmentViewModel>>

    /**
     * Retrieves the details of a specific appointment.
     * Endpoint: GET /api/VeterinaryAppointments/{id}
     *
     * @param id Appointment ID. HTTP 404 if not found.
     */
    @GET("api/VeterinaryAppointments/{id}")
    suspend fun getAppointmentById(@Path("id") id: Long): Response<VeterinaryAppointmentViewModel>

    /**
     * Creates a new veterinary appointment.
     * Endpoint: POST /api/VeterinaryAppointments
     *
     * @param request New appointment data ([VeterinaryAppointmentCreateModel]).
     * @return HTTP 201 Created with no body.
     */
    @POST("api/VeterinaryAppointments")
    suspend fun createAppointment(@Body request: VeterinaryAppointmentCreateModel): Response<Unit>

    /**
     * Updates an existing appointment.
     * Endpoint: PUT /api/VeterinaryAppointments/{id}
     *
     * @param id ID of the appointment to update.
     * @param request Fields to modify ([VeterinaryAppointmentUpdateModel]).
     * @return HTTP 204 No Content on success.
     */
    @PUT("api/VeterinaryAppointments/{id}")
    suspend fun updateAppointment(
        @Path("id") id: Long,
        @Body request: VeterinaryAppointmentUpdateModel
    ): Response<Unit>

    /**
     * Deletes an appointment by its ID.
     * Endpoint: DELETE /api/VeterinaryAppointments/{id}
     *
     * @param id ID of the appointment to delete.
     * @return HTTP 204 No Content on success.
     */
    @DELETE("api/VeterinaryAppointments/{id}")
    suspend fun deleteAppointment(@Path("id") id: Long): Response<Unit>

    // =========================================================================
    // Adoption Animals  →  /api/AdoptionAnimals
    // =========================================================================

    /**
     * Retrieves all adoption animals.
     * Endpoint: GET /api/AdoptionAnimals
     *
     * @return List of [AdoptionAnimalViewModel]; may be empty.
     */
    @GET("api/AdoptionAnimals")
    suspend fun getAllAdoptionAnimals(): Response<List<AdoptionAnimalViewModel>>

    /**
     * Retrieves adoption animals for a specific shelter.
     * Endpoint: GET /api/AdoptionAnimals/shelter/{shelterId}
     *
     * @param shelterId Shelter ID.
     * @return List of [AdoptionAnimalViewModel] for that shelter.
     */
    @GET("api/AdoptionAnimals/shelter/{shelterId}")
    suspend fun getAdoptionAnimalsByShelterId(@Path("shelterId") shelterId: Long): Response<List<AdoptionAnimalViewModel>>

    /**
     * Retrieves the details of an adoption animal by its ID.
     * Endpoint: GET /api/AdoptionAnimals/{id}
     *
     * @param id Adoption animal ID. HTTP 404 if not found.
     */
    @GET("api/AdoptionAnimals/{id}")
    suspend fun getAdoptionAnimalById(@Path("id") id: Long): Response<AdoptionAnimalViewModel>

    /**
     * Creates a new adoption animal.
     * Endpoint: POST /api/AdoptionAnimals
     *
     * @param request New adoption animal data ([AdoptionAnimalCreateModel]).
     * @return HTTP 201 Created with no body.
     */
    @POST("api/AdoptionAnimals")
    suspend fun createAdoptionAnimal(@Body request: AdoptionAnimalCreateModel): Response<Unit>

    /**
     * Updates an existing adoption animal.
     * Endpoint: PUT /api/AdoptionAnimals/{id}
     *
     * @param id ID of the adoption animal to update.
     * @param request Fields to modify ([AdoptionAnimalUpdateModel]).
     * @return HTTP 204 No Content on success.
     */
    @PUT("api/AdoptionAnimals/{id}")
    suspend fun updateAdoptionAnimal(
        @Path("id") id: Long,
        @Body request: AdoptionAnimalUpdateModel
    ): Response<Unit>

    /**
     * Deletes an adoption animal by its ID.
     * Endpoint: DELETE /api/AdoptionAnimals/{id}
     *
     * @param id ID of the adoption animal to delete.
     * @return HTTP 204 No Content on success.
     */
    @DELETE("api/AdoptionAnimals/{id}")
    suspend fun deleteAdoptionAnimal(@Path("id") id: Long): Response<Unit>

    /**
     * Uploads or replaces the main picture of an adoption animal.
     * Endpoint: PUT /api/AdoptionAnimals/{id}/mainpicture
     *
     * @param id   Adoption animal ID.
     * @param file Image file as multipart part (field name "file").
     * @return HTTP 204 No Content on success.
     */
    @Multipart
    @PUT("api/AdoptionAnimals/{id}/mainpicture")
    suspend fun uploadAdoptionAnimalMainPicture(
        @Path("id") id: Long,
        @Part file: MultipartBody.Part
    ): Response<Unit>

    /**
     * Retrieves additional photo filenames for an adoption animal.
     * Endpoint: GET /api/AdoptionAnimals/{id}/additionalphotos
     *
     * @param id Adoption animal ID.
     * @return List of photo filenames (pass each to [com.example.petradar.utils.PetImageUrlResolver.adoptionAdditionalPhotoUrl]).
     */
    @GET("api/AdoptionAnimals/{id}/additionalphotos")
    suspend fun getAdoptionAnimalAdditionalPhotos(@Path("id") id: Long): Response<List<String>>

    /**
     * Uploads one or more additional photos for an adoption animal.
     * Endpoint: PUT /api/AdoptionAnimals/{id}/additionalphotos
     *
     * @param id    Adoption animal ID.
     * @param files List of image files as multipart parts (field name "files").
     * @return HTTP 204 No Content on success.
     */
    @Multipart
    @PUT("api/AdoptionAnimals/{id}/additionalphotos")
    suspend fun uploadAdoptionAnimalAdditionalPhotos(
        @Path("id") id: Long,
        @Part files: List<MultipartBody.Part>
    ): Response<Unit>

    /**
     * Deletes a specific additional photo of an adoption animal.
     * Endpoint: DELETE /api/AdoptionAnimals/{id}/additionalphotos/{photoName}
     *
     * @param id        Adoption animal ID.
     * @param photoName File name of the photo to delete.
     * @return HTTP 204 No Content on success.
     */
    @DELETE("api/AdoptionAnimals/{id}/additionalphotos/{photoName}")
    suspend fun deleteAdoptionAnimalAdditionalPhoto(
        @Path("id") id: Long,
        @Path("photoName") photoName: String
    ): Response<Unit>
}

// =============================================================================
// Data models for Veterinary Appointments
// =============================================================================

/**
 * Represents a veterinary appointment as returned by the API (VeterinaryAppointmentViewModel).
 *
 * @property id                Unique appointment ID in the database.
 * @property petId             ID of the pet the appointment belongs to.
 * @property veterinaryName    Name of the veterinarian or clinic (may be null).
 * @property appointmentType   Appointment type: Checkup | Vaccination | Surgery | Grooming | Consultation | Other.
 * @property appointmentStatus Status: Scheduled | Cancelled.
 * @property appointmentDate   Date and time in ISO-8601 format.
 * @property durationInMinutes Estimated duration in minutes (may be null).
 * @property reasonForVisit    Reason for the visit (required).
 * @property notes             Additional notes (may be null).
 * @property diagnosis         Diagnosis recorded after the appointment (may be null).
 * @property treatment         Prescribed treatment (may be null).
 * @property prescriptions     Medical prescriptions (may be null).
 * @property cost              Consultation cost (may be null).
 * @property addressText       Textual address of the clinic (may be null).
 * @property reminderSent      Indicates whether a reminder has already been sent to the user.
 */
data class VeterinaryAppointmentViewModel(
    val id: Long,
    val petId: Long,
    val veterinaryName: String?,
    val appointmentType: String?,
    val appointmentStatus: String?,
    val appointmentDate: String,    // ISO-8601 (e.g. "2026-03-05T10:00:00")
    val durationInMinutes: Int?,
    val reasonForVisit: String?,
    val notes: String?,
    val diagnosis: String?,
    val treatment: String?,
    val prescriptions: String?,
    val cost: Double?,
    val addressText: String?,
    val latitude: Double?,
    val longitude: Double?,
    val reminderSent: Boolean
)

/**
 * Payload for creating a new appointment (VeterinaryAppointmentCreateModel in Swagger).
 * Fields marked as required in Swagger cannot be null.
 */
data class VeterinaryAppointmentCreateModel(
    val petId: Long,                           // Pet ID (required)
    val veterinaryName: String? = null,        // Vet name (required by API validation)
    val appointmentType: String,               // Enum as String (required)
    val appointmentStatus: String,             // Enum as String (required)
    val appointmentDate: String,               // ISO-8601 datetime (required)
    val durationInMinutes: Int? = null,
    val reasonForVisit: String,                // Reason (required)
    val notes: String? = null,
    val diagnosis: String? = null,
    val treatment: String? = null,
    val prescriptions: String? = null,
    val cost: Double? = null,
    val addressText: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * Payload for updating an existing appointment (VeterinaryAppointmentUpdateModel in Swagger).
 * All fields are optional; only non-null values are sent to the API.
 */
data class VeterinaryAppointmentUpdateModel(
    val veterinaryName: String? = null,
    val appointmentType: String? = null,
    val appointmentStatus: String? = null,
    val appointmentDate: String? = null,       // ISO-8601 datetime
    val durationInMinutes: Int? = null,
    val reasonForVisit: String? = null,
    val notes: String? = null,
    val diagnosis: String? = null,
    val treatment: String? = null,
    val prescriptions: String? = null,
    val cost: Double? = null,
    val addressText: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

// =============================================================================
// Data models for Pets
// =============================================================================

/**
 * Represents a pet as returned by the API (UserPetViewModel in Swagger).
 *
 * @property id                Unique pet ID.
 * @property userId            Owner user ID.
 * @property name              Pet name.
 * @property species           Species: "Dog" or "Cat".
 * @property breed             Breed (may be null).
 * @property color             Coat colour (may be null).
 * @property sex               Sex: "Male" | "Female" | "Unknown".
 * @property size              Size: "Small" | "Medium" | "Large".
 * @property birthDate         Date of birth in ISO-8601 (may be null).
 * @property approximateAge    Approximate age in years if exact birth date is unknown.
 * @property weight            Weight in kg (may be null).
 * @property description       Free-text description (may be null).
 * @property photoURL          Main photo URL (may be null; local photo managed in PetPhotoStore).
 * @property additionalPhotosURL Additional photo URLs (may be null).
 * @property isNeutered        Indicates whether the pet is neutered/spayed.
 * @property allergies         Known allergies (may be null).
 * @property medicalNotes      Additional medical notes (may be null).
 */
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

/**
 * Payload for creating a new pet (UserPetCreateModel in Swagger).
 * [userId], [name] and [species] are the only fields required by the API.
 */
data class UserPetCreateModel(
    val userId: Long,
    val name: String,
    val species: String,               // "Dog" | "Cat"
    val breed: String? = null,
    val color: String? = null,
    val sex: String? = null,           // "Male" | "Female" | "Unknown"
    val size: String? = null,          // "Small" | "Medium" | "Large"
    val birthDate: String? = null,     // ISO-8601 datetime
    val approximateAge: Double? = null,
    val weight: Double? = null,
    val description: String? = null,
    val isNeutered: Boolean? = null,
    val allergies: String? = null,
    val medicalNotes: String? = null
)

/**
 * Payload for updating an existing pet (UserPetUpdateModel in Swagger).
 * All fields are optional; only non-null values are considered for the update.
 */
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

// =============================================================================
// Data models for Reports
// =============================================================================

/**
 * Payload for creating a report (ReportCreateModel in Swagger).
 *
 * For lost pet flow, set:
 * - reportType = "Lost"
 * - reportStatus = "Active"
 */
data class ReportCreateModel(
    val userId: Long,
    val userPetId: Long? = null,
    val species: String,
    val breed: String? = null,
    val color: String? = null,
    val sex: String? = null,
    val size: String? = null,
    val approximateAge: Double? = null,
    val weight: Double? = null,
    val description: String? = null,
    val isNeutered: Boolean? = null,
    val reportType: String,
    val reportStatus: String? = null,
    val hasCollar: Boolean? = null,
    val hasTag: Boolean? = null,
    val incidentDate: String? = null,
    val latitude: Double,
    val longitude: Double,
    val addressText: String? = null,
    val searchRadiusMeters: Int = 0,
    val useAlternateContact: Boolean = false,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val offersReward: Boolean = false,
    val rewardAmount: Double? = null
)

/** Represents a report returned by the API (ReportViewModel in Swagger). */
data class ReportViewModel(
    val id: Long,
    val userId: Long,
    val userPetId: Long?,
    val species: String?,
    val breed: String?,
    val color: String?,
    val sex: String?,
    val size: String?,
    val approximateAge: Double?,
    val weight: Double?,
    val description: String?,
    val photoURL: String?,
    val additionalPhotosURL: String?,
    val isNeutered: Boolean?,
    val reportType: String?,
    val reportStatus: String?,
    val hasCollar: Boolean?,
    val hasTag: Boolean?,
    val reportDate: String?,
    val incidentDate: String?,
    val latitude: Double?,
    val longitude: Double?,
    val addressText: String?,
    val searchRadiusMeters: Int?,
    val useAlternateContact: Boolean?,
    val contactName: String?,
    val contactPhone: String?,
    val contactEmail: String?,
    val offersReward: Boolean?,
    val rewardAmount: Double?,
    val views: Int?
)

/**
 * Payload for updating an existing report (ReportUpdateModel in Swagger).
 * All fields are optional; only non-null values are considered for the update.
 */
data class ReportUpdateModel(
    val species: String? = null,
    val breed: String? = null,
    val color: String? = null,
    val sex: String? = null,
    val size: String? = null,
    val approximateAge: Double? = null,
    val weight: Double? = null,
    val description: String? = null,
    val isNeutered: Boolean? = null,
    val reportType: String? = null,
    val reportStatus: String? = null,
    val hasCollar: Boolean? = null,
    val hasTag: Boolean? = null,
    val incidentDate: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val addressText: String? = null,
    val searchRadiusMeters: Int? = null,
    val useAlternateContact: Boolean? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val offersReward: Boolean? = null,
    val rewardAmount: Double? = null
)

// =============================================================================
// Data models for Adoption Animals
// =============================================================================

/**
 * Represents an adoption animal as returned by the API (AdoptionAnimalViewModel in Swagger).
 *
 * @property id                  Unique adoption animal ID in the database.
 * @property shelterId           ID of the shelter that owns this animal.
 * @property name                Animal name.
 * @property species             Species: "Dog" or "Cat".
 * @property breed               Breed (may be null).
 * @property color               Coat colour (may be null).
 * @property sex                 Sex: "Male" | "Female" | "Unknown".
 * @property size                Size: "Small" | "Medium" | "Large".
 * @property approximateAge      Approximate age in years (may be null).
 * @property weight              Weight in kg (may be null).
 * @property description         Free-text description (may be null).
 * @property photoURL            Main photo URL (may be null).
 * @property additionalPhotosURL Additional photo URLs (may be null).
 * @property isNeutered          Indicates whether the animal is neutered/spayed.
 * @property personality         Personality traits (may be null).
 * @property goodWithKids        Whether the animal is good with children.
 * @property goodWithDogs        Whether the animal is good with dogs.
 * @property goodWithCats        Whether the animal is good with cats.
 * @property isVaccinated        Whether the animal is vaccinated.
 * @property needsSpecialCare    Whether the animal needs special care.
 * @property specialCareDetails  Details of special care needed (may be null).
 * @property status              Adoption status: "Available" | "Adopted" | "Reserved".
 * @property adoptionDate        Date of adoption in ISO-8601 (may be null).
 * @property adopterId           ID of the adopter (may be null).
 * @property views               Number of views.
 */
data class AdoptionAnimalViewModel(
    val id: Long,
    val shelterId: Long,
    val name: String?,
    val species: String?,
    val breed: String?,
    val color: String?,
    val sex: String?,
    val size: String?,
    val approximateAge: Double?,
    val weight: Double?,
    val description: String?,
    val photoURL: String?,
    val additionalPhotosURL: String?,
    val isNeutered: Boolean?,
    val personality: String?,
    val goodWithKids: Boolean?,
    val goodWithDogs: Boolean?,
    val goodWithCats: Boolean?,
    val isVaccinated: Boolean?,
    val needsSpecialCare: Boolean?,
    val specialCareDetails: String?,
    val status: String?,
    val adoptionDate: String?,
    val adopterId: Long?,
    val views: Int
)

/**
 * Payload for creating a new adoption animal (AdoptionAnimalCreateModel in Swagger).
 * [shelterId], [name] and [species] are required by the API.
 */
data class AdoptionAnimalCreateModel(
    val shelterId: Long,
    val name: String,
    val species: String,                       // "Dog" | "Cat"
    val breed: String? = null,
    val color: String? = null,
    val sex: String? = null,                   // "Male" | "Female" | "Unknown"
    val size: String? = null,                  // "Small" | "Medium" | "Large"
    val approximateAge: Double? = null,
    val weight: Double? = null,
    val description: String? = null,
    val isNeutered: Boolean? = null,
    val personality: String? = null,
    val goodWithKids: Boolean? = null,
    val goodWithDogs: Boolean? = null,
    val goodWithCats: Boolean? = null,
    val isVaccinated: Boolean? = null,
    val needsSpecialCare: Boolean? = null,
    val specialCareDetails: String? = null
)

/**
 * Payload for updating an existing adoption animal (AdoptionAnimalUpdateModel in Swagger).
 * All fields are optional; only non-null values are sent to the API.
 */
data class AdoptionAnimalUpdateModel(
    val name: String? = null,
    val species: String? = null,
    val breed: String? = null,
    val color: String? = null,
    val sex: String? = null,
    val size: String? = null,
    val approximateAge: Double? = null,
    val weight: Double? = null,
    val description: String? = null,
    val isNeutered: Boolean? = null,
    val personality: String? = null,
    val goodWithKids: Boolean? = null,
    val goodWithDogs: Boolean? = null,
    val goodWithCats: Boolean? = null,
    val isVaccinated: Boolean? = null,
    val needsSpecialCare: Boolean? = null,
    val specialCareDetails: String? = null,
    val status: String? = null,                // "Available" | "Adopted" | "Reserved"
    val adoptionDate: String? = null,          // ISO-8601 datetime
    val adopterId: Long? = null
)
