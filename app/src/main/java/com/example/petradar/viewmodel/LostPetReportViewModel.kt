package com.example.petradar.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petradar.api.ReportCreateModel
import com.example.petradar.api.UserPetViewModel
import com.example.petradar.repository.PetRepository
import com.example.petradar.repository.ReportRepository
import kotlinx.coroutines.launch

class LostPetReportViewModel : ViewModel() {

    private val petRepository = PetRepository()
    private val reportRepository = ReportRepository()

    private val _pet = MutableLiveData<UserPetViewModel?>()
    val pet: LiveData<UserPetViewModel?> = _pet

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _saveSuccess = MutableLiveData(false)
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    fun loadPet(petId: Long) {
        if (petId <= 0) return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = petRepository.getPetById(petId)
                if (response.isSuccessful) {
                    _pet.value = response.body()
                } else {
                    _errorMessage.value = "No se pudo cargar la mascota (${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createLostReport(request: ReportCreateModel) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _saveSuccess.value = false
            try {
                val response = reportRepository.create(request)
                if (response.isSuccessful) {
                    _saveSuccess.value = true
                } else {
                    _errorMessage.value = "No se pudo crear el reporte (${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

