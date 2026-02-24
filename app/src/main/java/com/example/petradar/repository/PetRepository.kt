package com.example.petradar.repository

import com.example.petradar.api.RetrofitClient
import com.example.petradar.api.UserPetCreateModel
import com.example.petradar.api.UserPetUpdateModel
import com.example.petradar.api.UserPetViewModel
import retrofit2.Response

class PetRepository {

    private val apiService = RetrofitClient.apiService

    /**
     * Obtener mascotas por userId
     * Endpoint: GET /api/UserPets/user/{userId}
     */
    suspend fun getPetsByUserId(userId: Long): Response<List<UserPetViewModel>> {
        return apiService.getUserPetsByUserId(userId)
    }

    /**
     * Obtener mascota por ID
     * Endpoint: GET /api/UserPets/{id}
     */
    suspend fun getPetById(petId: Long): Response<UserPetViewModel> {
        return apiService.getUserPetById(petId)
    }

    /**
     * Crear mascota
     * Endpoint: POST /api/UserPets
     */
    suspend fun createPet(request: UserPetCreateModel): Response<Unit> {
        return apiService.createUserPet(request)
    }

    /**
     * Actualizar mascota
     * Endpoint: PUT /api/UserPets/{id}
     */
    suspend fun updatePet(petId: Long, request: UserPetUpdateModel): Response<Unit> {
        return apiService.updateUserPet(petId, request)
    }

    /**
     * Eliminar mascota
     * Endpoint: DELETE /api/UserPets/{id}
     */
    suspend fun deletePet(petId: Long): Response<Unit> {
        return apiService.deleteUserPet(petId)
    }
}

