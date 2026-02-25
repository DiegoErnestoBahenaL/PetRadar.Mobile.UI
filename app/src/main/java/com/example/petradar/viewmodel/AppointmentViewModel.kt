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

class AppointmentViewModel : ViewModel() {

    private val repo = AppointmentRepository()
    private val petRepo = PetRepository()

    private val _appointments = MutableLiveData<List<VeterinaryAppointmentViewModel>>(emptyList())
    val appointments: LiveData<List<VeterinaryAppointmentViewModel>> = _appointments

    private val _selected = MutableLiveData<VeterinaryAppointmentViewModel?>()
    val selected: LiveData<VeterinaryAppointmentViewModel?> = _selected

    private val _userPets = MutableLiveData<List<UserPetViewModel>>(emptyList())
    val userPets: LiveData<List<UserPetViewModel>> = _userPets

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _saveSuccess = MutableLiveData(false)
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _deleteSuccess = MutableLiveData(false)
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess

    fun loadByUser(userId: Long) = viewModelScope.launch {
        _isLoading.value = true
        _error.value = null
        try {
            val r = repo.getByUserId(userId)
            if (r.isSuccessful) _appointments.value = r.body() ?: emptyList()
            else _error.value = "Error ${r.code()}: ${r.message()}"
        } catch (e: Exception) {
            _error.value = "Error de conexión: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    fun loadPetsByUser(userId: Long) = viewModelScope.launch {
        try {
            val r = petRepo.getPetsByUserId(userId)
            if (r.isSuccessful) _userPets.value = r.body() ?: emptyList()
        } catch (_: Exception) { }
    }

    fun loadById(id: Long) = viewModelScope.launch {
        _isLoading.value = true
        _error.value = null
        try {
            val r = repo.getById(id)
            if (r.isSuccessful) _selected.value = r.body()
            else _error.value = "Error ${r.code()}"
        } catch (e: Exception) {
            _error.value = "Error de conexión: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    fun create(req: VeterinaryAppointmentCreateModel, userId: Long) = viewModelScope.launch {
        _isLoading.value = true
        _error.value = null
        _saveSuccess.value = false
        try {
            val r = repo.create(req)
            if (r.isSuccessful) { _saveSuccess.value = true; loadByUser(userId) }
            else _error.value = "Error al crear cita: ${r.code()}"
        } catch (e: Exception) {
            _error.value = "Error de conexión: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    fun update(id: Long, req: VeterinaryAppointmentUpdateModel, userId: Long) = viewModelScope.launch {
        _isLoading.value = true
        _error.value = null
        _saveSuccess.value = false
        try {
            val r = repo.update(id, req)
            if (r.isSuccessful) { _saveSuccess.value = true; loadByUser(userId) }
            else _error.value = "Error al actualizar cita: ${r.code()}"
        } catch (e: Exception) {
            _error.value = "Error de conexión: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    fun delete(id: Long, userId: Long) = viewModelScope.launch {
        _isLoading.value = true
        _error.value = null
        _deleteSuccess.value = false
        try {
            val r = repo.delete(id)
            if (r.isSuccessful) { _deleteSuccess.value = true; loadByUser(userId) }
            else _error.value = "Error al eliminar cita: ${r.code()}"
        } catch (e: Exception) {
            _error.value = "Error de conexión: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    fun clearSaveSuccess() { _saveSuccess.value = false }
}

