package com.example.petradar.repository

import com.example.petradar.api.RetrofitClient
import com.example.petradar.api.VeterinaryAppointmentCreateModel
import com.example.petradar.api.VeterinaryAppointmentUpdateModel
import com.example.petradar.api.VeterinaryAppointmentViewModel
import retrofit2.Response

class AppointmentRepository {

    private val api = RetrofitClient.apiService

    suspend fun getByUserId(userId: Long): Response<List<VeterinaryAppointmentViewModel>> =
        api.getAppointmentsByUserId(userId)

    suspend fun getByPetId(petId: Long): Response<List<VeterinaryAppointmentViewModel>> =
        api.getAppointmentsByPetId(petId)

    suspend fun getById(id: Long): Response<VeterinaryAppointmentViewModel> =
        api.getAppointmentById(id)

    suspend fun create(request: VeterinaryAppointmentCreateModel): Response<Unit> =
        api.createAppointment(request)

    suspend fun update(id: Long, request: VeterinaryAppointmentUpdateModel): Response<Unit> =
        api.updateAppointment(id, request)

    suspend fun delete(id: Long): Response<Unit> =
        api.deleteAppointment(id)
}

