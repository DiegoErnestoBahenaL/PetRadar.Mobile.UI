package com.example.petradar.repository

import com.example.petradar.api.AdoptionAnimalCreateModel
import com.example.petradar.api.AdoptionAnimalUpdateModel
import com.example.petradar.api.AdoptionAnimalViewModel
import com.example.petradar.api.RetrofitClient
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
}

