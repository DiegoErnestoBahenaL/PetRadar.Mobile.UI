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
    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess
    /**
     * Emits the real server-assigned ID of a newly created pet.
     * Populated after a successful creation + list reload so the Activity
     * can associate the pending photo with the correct pet ID.
     */
    private val _createdPetId = MutableLiveData<Long?>()
    val createdPetId: LiveData<Long?> = _createdPetId
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
                    _errorMessage.value = "Error loading pet: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error: ${e.message}"
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
                    // API returns 201 with no body – fetch the list to find the new pet ID
                    resolveCreatedPetId(userId, name)
                    _saveSuccess.value = true
                } else {
                    _errorMessage.value = "Error creating pet: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    /**
     * Fetches the user's pet list and emits the ID of the newest pet
     * whose name matches [name] via [createdPetId].
     */
    private suspend fun resolveCreatedPetId(userId: Long, name: String) {
        try {
            val response = repository.getPetsByUserId(userId)
            if (response.isSuccessful) {
                val newPet = response.body()
                    ?.filter { it.name.equals(name, ignoreCase = true) }
                    ?.maxByOrNull { it.id }
                _createdPetId.value = newPet?.id
            }
        } catch (_: Exception) { /* non-fatal – photo just won't be saved */ }
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
                    loadPet(petId)
                } else {
                    _errorMessage.value = "Error updating pet: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun deletePet() {
        val petId = currentPetId
        if (petId <= 0) return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = repository.deletePet(petId)
                if (response.isSuccessful) {
                    _deleteSuccess.value = true
                } else {
                    _errorMessage.value = "Error al eliminar: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexion: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
