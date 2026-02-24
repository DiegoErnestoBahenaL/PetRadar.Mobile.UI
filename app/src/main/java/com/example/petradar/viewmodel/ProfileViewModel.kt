package com.example.petradar.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petradar.api.models.UpdateProfileRequest
import com.example.petradar.api.models.UserProfile
import com.example.petradar.repository.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> = _updateSuccess

    /**
     * Cargar perfil de usuario por ID
     * Endpoint: GET /api/Users/{id}
     */
    fun loadUserProfile(userId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = repository.getUserById(userId)
                if (response.isSuccessful) {
                    _userProfile.value = response.body()
                } else {
                    _errorMessage.value = "Error al cargar el perfil: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualizar perfil de usuario
     * Endpoint: PUT /api/Users/{id}
     */
    fun updateProfile(
        userId: Long,
        name: String?,
        lastName: String?,
        phoneNumber: String?,
        email: String? = null,
        password: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _updateSuccess.value = false
            try {
                val request = UpdateProfileRequest(
                    name = name,
                    lastName = lastName,
                    phoneNumber = phoneNumber,
                    email = email,
                    password = password
                )
                val response = repository.updateUser(userId, request)
                if (response.isSuccessful) {
                    // Optimistically update the local profile so the UI reflects changes immediately
                    _userProfile.value = _userProfile.value?.copy(
                        name = name ?: _userProfile.value?.name ?: "",
                        lastName = lastName ?: _userProfile.value?.lastName,
                        phoneNumber = phoneNumber ?: _userProfile.value?.phoneNumber
                    )
                    _updateSuccess.value = true
                    // Refresh from server in background to stay in sync
                    loadUserProfile(userId)
                } else {
                    _errorMessage.value = "Error al actualizar: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearUpdateSuccess() { _updateSuccess.value = false }
}



