package com.example.petradar.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petradar.api.UserPetViewModel
import com.example.petradar.repository.PetRepository
import kotlinx.coroutines.launch

class PetViewModel : ViewModel() {

    private val repository = PetRepository()

    private val _pets = MutableLiveData<List<UserPetViewModel>>()
    val pets: LiveData<List<UserPetViewModel>> = _pets

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess

    fun loadPets(userId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = repository.getPetsByUserId(userId)
                if (response.isSuccessful) {
                    _pets.value = response.body() ?: emptyList()
                } else {
                    _errorMessage.value = "Error al cargar mascotas: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexion: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePet(petId: Long, userId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = repository.deletePet(petId)
                if (response.isSuccessful) {
                    _deleteSuccess.value = true
                    loadPets(userId)
                } else {
                    _errorMessage.value = "Error al eliminar: ${response.code()}"
                    _deleteSuccess.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexion: ${e.message}"
                _deleteSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}

