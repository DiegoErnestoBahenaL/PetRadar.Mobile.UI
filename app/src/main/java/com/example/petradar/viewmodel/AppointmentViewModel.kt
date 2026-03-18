package com.example.petradar.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petradar.api.UserPetViewModel
import com.example.petradar.api.VeterinaryAppointmentCreateModel
import com.example.petradar.api.VeterinaryAppointmentUpdateModel
import com.example.petradar.api.VeterinaryAppointmentViewModel
import com.example.petradar.repository.AppointmentRepository
import com.example.petradar.repository.PetRepository
import kotlinx.coroutines.launch

/**
 * ViewModel that manages the business logic for the veterinary appointments screen
 * and the appointment creation/edit form.
 *
 * Responsibilities:
 *  - Load the user's appointments from GET /api/VeterinaryAppointments/user/{userId}.
 *  - Load the user's pets for the form dropdown.
 *  - Create, edit and delete appointments via the API.
 *  - Maintain UI state (loading, error, success) as LiveData.
 *
 * Exposed LiveData:
 *  - [appointments]  → list of the user's appointments; used to render the calendar.
 *  - [selected]      → individual appointment loaded for editing.
 *  - [userPets]      → user's pets for the form selector.
 *  - [isLoading]     → true while a network request is in progress.
 *  - [error]         → error message for the UI; null when there is no error.
 *  - [saveSuccess]   → true immediately after a successful create or update.
 *  - [deleteSuccess] → true immediately after a successful deletion.
 */
class AppointmentViewModel : ViewModel() {

    private val repo = AppointmentRepository()
    /** Pet repository; used to load the dropdown list. */
    private val petRepo = PetRepository()

    private val _appointments = MutableLiveData<List<VeterinaryAppointmentViewModel>>(emptyList())
    /** List of the user's appointments. Used in the calendar and appointment list. */
    val appointments: LiveData<List<VeterinaryAppointmentViewModel>> = _appointments

    private val _selected = MutableLiveData<VeterinaryAppointmentViewModel?>()
    /** Currently selected appointment for editing; null in creation mode. */
    val selected: LiveData<VeterinaryAppointmentViewModel?> = _selected

    private val _userPets = MutableLiveData<List<UserPetViewModel>>(emptyList())
    /** User's pets; shown in the "Pet" dropdown of the appointment form. */
    val userPets: LiveData<List<UserPetViewModel>> = _userPets

    private val _isLoading = MutableLiveData(false)
    /** true while a network request is active. */
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    /** Error message to display to the user; null when there is no error. */
    val error: LiveData<String?> = _error

    private val _saveSuccess = MutableLiveData(false)
    /** Emits true immediately after an appointment is successfully created or updated. */
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _deleteSuccess = MutableLiveData(false)
    /** Emits true immediately after an appointment is successfully deleted. */
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess

    /**
     * Loads all the user's appointments from the API.
     * Endpoint: GET /api/VeterinaryAppointments/user/{userId}
     *
     * Also called from [com.example.petradar.AppointmentsActivity.onResume]
     * to reflect changes made in the form.
     *
     * @param userId ID of the user whose appointments should be loaded.
     */
    fun loadByUser(userId: Long) = viewModelScope.launch {
        _isLoading.value = true
        _error.value = null
        try {
            val r = repo.getByUserId(userId)
            if (r.isSuccessful) _appointments.value = r.body() ?: emptyList()
            else _error.value = "Error ${r.code()}: ${r.message()}"
        } catch (e: Exception) {
            _error.value = "Connection error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Loads the user's pets to display them in the appointment form dropdown.
     * Endpoint: GET /api/UserPets/user/{userId}
     *
     * Errors are silenced (the form will simply have an empty dropdown).
     *
     * @param userId ID of the pet owner.
     */
    fun loadPetsByUser(userId: Long) = viewModelScope.launch {
        try {
            val r = petRepo.getPetsByUserId(userId)
            if (r.isSuccessful) _userPets.value = r.body() ?: emptyList()
        } catch (_: Exception) { /* Silenced; dropdown will be empty */ }
    }

    /**
     * Loads a specific appointment's details for the edit form.
     * Endpoint: GET /api/VeterinaryAppointments/{id}
     *
     * @param id Appointment ID to load.
     */
    fun loadById(id: Long) = viewModelScope.launch {
        _isLoading.value = true
        _error.value = null
        try {
            val r = repo.getById(id)
            if (r.isSuccessful) _selected.value = r.body()
            else _error.value = "Error ${r.code()}"
        } catch (e: Exception) {
            _error.value = "Connection error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Creates a new veterinary appointment.
     * Endpoint: POST /api/VeterinaryAppointments
     *
     * After success, reloads the user's appointments to update the calendar.
     *
     * @param req    New appointment data ([VeterinaryAppointmentCreateModel]).
     * @param userId User ID; needed to reload their appointment list.
     */
    fun create(req: VeterinaryAppointmentCreateModel, userId: Long) = viewModelScope.launch {
        _isLoading.value = true
        _error.value = null
        _saveSuccess.value = false
        try {
            val r = repo.create(req)
            if (r.isSuccessful) {
                _saveSuccess.value = true
                loadByUser(userId) // Reload to show the new appointment in the calendar.
            } else _error.value = "Error creating appointment: ${r.code()}"
        } catch (e: Exception) {
            _error.value = "Connection error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Updates an existing appointment.
     * Endpoint: PUT /api/VeterinaryAppointments/{id}
     *
     * After success, reloads the user's appointments.
     *
     * @param id     ID of the appointment to update.
     * @param req    Fields to modify ([VeterinaryAppointmentUpdateModel]).
     * @param userId User ID; needed to reload their appointment list.
     */
    fun update(id: Long, req: VeterinaryAppointmentUpdateModel, userId: Long) = viewModelScope.launch {
        _isLoading.value = true
        _error.value = null
        _saveSuccess.value = false
        try {
            val r = repo.update(id, req)
            if (r.isSuccessful) {
                _saveSuccess.value = true
                loadByUser(userId)
            } else _error.value = "Error updating appointment: ${r.code()}"
        } catch (e: Exception) {
            _error.value = "Connection error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Deletes an appointment by its ID and reloads the list.
     * Endpoint: DELETE /api/VeterinaryAppointments/{id}
     *
     * @param id     ID of the appointment to delete.
     * @param userId User ID; needed to reload their appointment list.
     */
    fun delete(id: Long, userId: Long) = viewModelScope.launch {
        _isLoading.value = true
        _error.value = null
        _deleteSuccess.value = false
        try {
            val r = repo.delete(id)
            if (r.isSuccessful) {
                _deleteSuccess.value = true
                loadByUser(userId)
            } else _error.value = "Error deleting appointment: ${r.code()}"
        } catch (e: Exception) {
            _error.value = "Connection error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Resets [saveSuccess] to false.
     * Must be called from the UI after processing the event (e.g. closing the form)
     * to prevent recomposition from processing it again.
     */
    fun clearSaveSuccess() { _saveSuccess.value = false }
}
