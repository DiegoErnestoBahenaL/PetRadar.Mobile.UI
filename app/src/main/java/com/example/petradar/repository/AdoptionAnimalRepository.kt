package com.example.petradar.repository

import com.example.petradar.api.AdoptionAnimalCreateModel
import com.example.petradar.api.AdoptionAnimalUpdateModel
import com.example.petradar.api.AdoptionAnimalViewModel
import com.example.petradar.api.AdoptionRequest
import com.example.petradar.api.RetrofitClient
import okhttp3.MultipartBody
import retrofit2.Response

/**
 * Repository that centralises all CRUD operations for adoption animals with the API.
 *
 * Acts as the intermediary between ViewModels and [RetrofitClient.apiService].
 * All functions are `suspend` and must be called from a coroutine.
 */
class AdoptionAnimalRepository {

    /** Reference to the Retrofit service obtained from the [RetrofitClient] singleton. */
    private val api = RetrofitClient.apiService

    /**
     * Retrieves all adoption animals.
     * Endpoint: GET /api/AdoptionAnimals
     *
     * @return List of [AdoptionAnimalViewModel]; may be empty.
     */
    suspend fun getAll(): Response<List<AdoptionAnimalViewModel>> =
        api.getAllAdoptionAnimals()

    /**
     * Retrieves adoption animals for a specific shelter.
     * Endpoint: GET /api/AdoptionAnimals/shelter/{shelterId}
     *
     * @param shelterId Shelter ID.
     * @return List of [AdoptionAnimalViewModel] for that shelter.
     */
    suspend fun getByShelterId(shelterId: Long): Response<List<AdoptionAnimalViewModel>> =
        api.getAdoptionAnimalsByShelterId(shelterId)

    /**
     * Retrieves the details of an adoption animal by its ID.
     * Endpoint: GET /api/AdoptionAnimals/{id}
     *
     * @param id Adoption animal ID. HTTP 404 if not found.
     */
    suspend fun getById(id: Long): Response<AdoptionAnimalViewModel> =
        api.getAdoptionAnimalById(id)

    /**
     * Creates a new adoption animal.
     * Endpoint: POST /api/AdoptionAnimals
     *
     * @param request Adoption animal data ([AdoptionAnimalCreateModel]).
     * @return HTTP 201 Created with no body on success.
     */
    suspend fun create(request: AdoptionAnimalCreateModel): Response<Unit> =
        api.createAdoptionAnimal(request)

    /**
     * Updates an existing adoption animal.
     * Endpoint: PUT /api/AdoptionAnimals/{id}
     *
     * @param id      Adoption animal ID to update.
     * @param request Fields to modify ([AdoptionAnimalUpdateModel]).
     * @return HTTP 204 No Content on success.
     */
    suspend fun update(id: Long, request: AdoptionAnimalUpdateModel): Response<Unit> =
        api.updateAdoptionAnimal(id, request)

    /**
     * Deletes an adoption animal by its ID.
     * Endpoint: DELETE /api/AdoptionAnimals/{id}
     *
     * @param id Adoption animal ID to delete.
     * @return HTTP 204 No Content on success.
     */
    suspend fun delete(id: Long): Response<Unit> =
        api.deleteAdoptionAnimal(id)

    /**
     * Uploads or replaces the main picture of an adoption animal.
     * Endpoint: PUT /api/AdoptionAnimals/{id}/mainpicture
     */
    suspend fun uploadMainPicture(id: Long, file: MultipartBody.Part): Response<Unit> =
        api.uploadAdoptionAnimalMainPicture(id, file)

    /**
     * Retrieves all additional photo URLs for an adoption animal.
     * Endpoint: GET /api/AdoptionAnimals/{id}/additionalphotos
     */
    suspend fun getAdditionalPhotos(id: Long): Response<List<String>> =
        api.getAdoptionAnimalAdditionalPhotos(id)

    /**
     * Uploads one or more additional photos for an adoption animal.
     * Endpoint: PUT /api/AdoptionAnimals/{id}/additionalphotos
     */
    suspend fun uploadAdditionalPhotos(id: Long, files: List<MultipartBody.Part>): Response<Unit> =
        api.uploadAdoptionAnimalAdditionalPhotos(id, files)

    /**
     * Deletes a specific additional photo of an adoption animal.
     * Endpoint: DELETE /api/AdoptionAnimals/{id}/additionalphotos/{photoName}
     */
    suspend fun deleteAdditionalPhoto(id: Long, photoName: String): Response<Unit> =
        api.deleteAdoptionAnimalAdditionalPhoto(id, photoName)

    /**
     * Submits an adoption request for an animal.
     * Endpoint: PUT /api/AdoptionAnimals/{id}/adoptionrequest
     */
    suspend fun submitAdoptionRequest(id: Long, request: AdoptionRequest): Response<Unit> =
        api.submitAdoptionRequest(id, request)

    /**
     * Approves an adoption request from a specific user.
     * Endpoint: PUT /api/AdoptionAnimals/{id}/approveadoptionrequest/{adopterId}
     */
    suspend fun approveAdoptionRequest(id: Long, adopterId: Long): Response<Unit> =
        api.approveAdoptionRequest(id, adopterId)
}

