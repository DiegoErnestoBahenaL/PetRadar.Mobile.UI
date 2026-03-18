package com.example.petradar.repository

import com.example.petradar.api.RetrofitClient
import com.example.petradar.api.VeterinaryAppointmentCreateModel
import com.example.petradar.api.VeterinaryAppointmentUpdateModel
import com.example.petradar.api.VeterinaryAppointmentViewModel
import retrofit2.Response

/**
 * Repository that centralises all CRUD operations for veterinary appointments with the API.
 *
 * Acts as the intermediary between [com.example.petradar.viewmodel.AppointmentViewModel] and [RetrofitClient.apiService].
 * All functions are `suspend` and must be called from a coroutine.
 */
class AppointmentRepository {

    /** Reference to the Retrofit service obtained from the [RetrofitClient] singleton. */
    private val api = RetrofitClient.apiService

    /**
     * Retrieves all appointments for a user (across their pets).
     * Endpoint: GET /api/VeterinaryAppointments/user/{userId}
     *
     * @param userId User ID whose appointments are to be retrieved.
     * @return List of [VeterinaryAppointmentViewModel]; may be empty.
     */
    suspend fun getByUserId(userId: Long): Response<List<VeterinaryAppointmentViewModel>> =
        api.getAppointmentsByUserId(userId)

    /**
     * Retrieves the details of a single appointment by its ID.
     * Endpoint: GET /api/VeterinaryAppointments/{id}
     *
     * @param id Appointment ID. HTTP 404 if not found.
     */
    suspend fun getById(id: Long): Response<VeterinaryAppointmentViewModel> =
        api.getAppointmentById(id)

    /**
     * Creates a new veterinary appointment.
     * Endpoint: POST /api/VeterinaryAppointments
     *
     * @param request Appointment data ([VeterinaryAppointmentCreateModel]).
     * @return HTTP 201 Created with no body on success.
     */
    suspend fun create(request: VeterinaryAppointmentCreateModel): Response<Unit> =
        api.createAppointment(request)

    /**
     * Updates an existing appointment.
     * Endpoint: PUT /api/VeterinaryAppointments/{id}
     *
     * @param id      Appointment ID to update.
     * @param request Fields to modify ([VeterinaryAppointmentUpdateModel]).
     * @return HTTP 204 No Content on success.
     */
    suspend fun update(id: Long, request: VeterinaryAppointmentUpdateModel): Response<Unit> =
        api.updateAppointment(id, request)

    /**
     * Deletes an appointment by its ID.
     * Endpoint: DELETE /api/VeterinaryAppointments/{id}
     *
     * @param id Appointment ID to delete.
     * @return HTTP 204 No Content on success.
     */
    suspend fun delete(id: Long): Response<Unit> =
        api.deleteAppointment(id)
}
