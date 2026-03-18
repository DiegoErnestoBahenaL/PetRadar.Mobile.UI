package com.example.petradar.repository

import com.example.petradar.api.RetrofitClient
import com.example.petradar.api.UserPetCreateModel
import com.example.petradar.api.UserPetUpdateModel
import com.example.petradar.api.UserPetViewModel
import retrofit2.Response

/**
 * Repository that centralises all CRUD operations for pets with the API.
 *
 * Acts as the intermediary between ViewModels and [RetrofitClient.apiService].
 * All functions are `suspend` and must be called from a coroutine.
 */
class PetRepository {

    /** Reference to the Retrofit service obtained from the [RetrofitClient] singleton. */
    private val apiService = RetrofitClient.apiService

    /**
     * Retrieves the list of pets belonging to a user.
     * Endpoint: GET /api/UserPets/user/{userId}
     *
     * @param userId Owner user ID.
     * @return List of [UserPetViewModel]; may be empty if the user has no pets.
     */
    suspend fun getPetsByUserId(userId: Long): Response<List<UserPetViewModel>> {
        return apiService.getUserPetsByUserId(userId)
    }

    /**
     * Retrieves the details of a pet by its ID.
     * Endpoint: GET /api/UserPets/{id}
     *
     * @param petId Pet ID. HTTP 404 if not found.
     */
    suspend fun getPetById(petId: Long): Response<UserPetViewModel> {
        return apiService.getUserPetById(petId)
    }

    /**
     * Creates a new pet in the system.
     * Endpoint: POST /api/UserPets
     *
     * @param request Data for the new pet ([UserPetCreateModel]).
     * @return HTTP 201 Created with no body on success.
     */
    suspend fun createPet(request: UserPetCreateModel): Response<Unit> {
        return apiService.createUserPet(request)
    }

    /**
     * Updates an existing pet's data.
     * Endpoint: PUT /api/UserPets/{id}
     *
     * @param petId   ID of the pet to update.
     * @param request Fields to modify ([UserPetUpdateModel]); null fields are ignored.
     * @return HTTP 204 No Content on success.
     */
    suspend fun updatePet(petId: Long, request: UserPetUpdateModel): Response<Unit> {
        return apiService.updateUserPet(petId, request)
    }

    /**
     * Deletes a pet from the system.
     * Endpoint: DELETE /api/UserPets/{id}
     *
     * Note: the local photo associated with the pet should also be removed
     * from [com.example.petradar.utils.PetPhotoStore] in the ViewModel.
     *
     * @param petId ID of the pet to delete.
     * @return HTTP 204 No Content on success.
     */
    suspend fun deletePet(petId: Long): Response<Unit> {
        return apiService.deleteUserPet(petId)
    }
}
