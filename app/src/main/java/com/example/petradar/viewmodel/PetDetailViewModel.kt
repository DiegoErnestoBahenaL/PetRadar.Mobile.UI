package com.example.petradar.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petradar.api.UserPetCreateModel
import com.example.petradar.api.UserPetUpdateModel
import com.example.petradar.api.UserPetViewModel
import com.example.petradar.repository.PetRepository
import kotlinx.coroutines.launch

class PetDetailViewModel : ViewModel() {

    private val repository = PetRepository()

    private val _pet = MutableLiveData<UserPetViewModel?>()
    val pet: LiveData<UserPetViewModel?> = _pet

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    // Exposed after a successful create so the Activity can persist the photo
    var pendingPhotoUri: String? = null

    var currentPetId: Long = -1L
    var currentUserId: Long = -1L

    fun savePet(
        name: String, speciesValue: String, breed: String?, color: String?,
        sexValue: String?, sizeValue: String?, birthDate: String?,
        weight: Double?, description: String?, isNeutered: Boolean,
        allergies: String?, medicalNotes: String?,
        photoUri: String? = null
    ) {
        pendingPhotoUri = photoUri
        if (currentPetId > 0) {
            updatePet(
                petId = currentPetId, name = name, species = speciesValue,
                breed = breed, color = color, sex = sexValue, size = sizeValue,
                birthDate = birthDate, weight = weight, description = description,
                isNeutered = isNeutered, allergies = allergies, medicalNotes = medicalNotes
            )
        } else {
            createPet(
                userId = currentUserId, name = name, species = speciesValue,
                breed = breed, color = color, sex = sexValue, size = sizeValue,
                birthDate = birthDate, weight = weight, description = description,
                isNeutered = isNeutered, allergies = allergies, medicalNotes = medicalNotes
            )
        }
    }

    fun loadPet(petId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = repository.getPetById(petId)
                if (response.isSuccessful) {
                    _pet.value = response.body()
                } else {
                    _errorMessage.value = "Error al cargar mascota: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexion: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPet(
        userId: Long, name: String, species: String, breed: String?,
        color: String?, sex: String?, size: String?, birthDate: String?,
        weight: Double?, description: String?, isNeutered: Boolean?,
        allergies: String?, medicalNotes: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _saveSuccess.value = false
            try {
                val request = UserPetCreateModel(
                    userId = userId, name = name, species = species,
                    breed = breed, color = color, sex = sex, size = size,
                    birthDate = birthDate, weight = weight, description = description,
                    isNeutered = isNeutered, allergies = allergies, medicalNotes = medicalNotes
                )
                val response = repository.createPet(request)
                if (response.isSuccessful) {
                    _saveSuccess.value = true
                } else {
                    _errorMessage.value = "Error al crear mascota: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexion: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePet(
        petId: Long, name: String?, species: String?, breed: String?,
        color: String?, sex: String?, size: String?, birthDate: String?,
        weight: Double?, description: String?, isNeutered: Boolean?,
        allergies: String?, medicalNotes: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _saveSuccess.value = false
            try {
                val request = UserPetUpdateModel(
                    name = name, species = species, breed = breed, color = color,
                    sex = sex, size = size, birthDate = birthDate, weight = weight,
                    description = description, isNeutered = isNeutered,
                    allergies = allergies, medicalNotes = medicalNotes
                )
                val response = repository.updatePet(petId, request)
                if (response.isSuccessful) {
                    _saveSuccess.value = true
                } else {
                    _errorMessage.value = "Error al actualizar mascota: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexion: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

