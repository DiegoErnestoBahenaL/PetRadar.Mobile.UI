package com.example.petradar.repository

import com.example.petradar.api.RetrofitClient
import com.example.petradar.api.UserPetCreateModel
import com.example.petradar.api.UserPetUpdateModel
import com.example.petradar.api.UserPetViewModel
import okhttp3.MultipartBody
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

    suspend fun getPetsByUserId(userId: Long): Response<List<UserPetViewModel>> {
        return apiService.getUserPetsByUserId(userId)
    }

    suspend fun getPetById(petId: Long): Response<UserPetViewModel> {
        return apiService.getUserPetById(petId)
    }

    suspend fun createPet(request: UserPetCreateModel): Response<Unit> {
        return apiService.createUserPet(request)
    }

    suspend fun updatePet(petId: Long, request: UserPetUpdateModel): Response<Unit> {
        return apiService.updateUserPet(petId, request)
    }

    suspend fun deletePet(petId: Long): Response<Unit> {
        return apiService.deleteUserPet(petId)
    }

    suspend fun uploadPetMainPicture(petId: Long, filePart: MultipartBody.Part): Response<Unit> {
        return apiService.uploadPetMainPicture(petId, filePart)
    }

    suspend fun getAdditionalPhotos(petId: Long): Response<List<String>> =
        apiService.getUserPetAdditionalPhotos(petId)

    suspend fun uploadAdditionalPhotos(petId: Long, files: List<MultipartBody.Part>): Response<Unit> =
        apiService.uploadUserPetAdditionalPhotos(petId, files)

    suspend fun deleteAdditionalPhoto(petId: Long, photoName: String): Response<Unit> =
        apiService.deleteUserPetAdditionalPhoto(petId, photoName)
}
