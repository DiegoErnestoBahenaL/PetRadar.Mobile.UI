package com.example.petradar.viewmodel
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.net.toUri
import com.example.petradar.api.UserPetCreateModel
import com.example.petradar.api.UserPetUpdateModel
import com.example.petradar.api.UserPetViewModel
import com.example.petradar.repository.PetRepository
import com.example.petradar.utils.PetPhotoStore
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val _additionalPhotos = MutableLiveData<List<String>>(emptyList())
    val additionalPhotos: LiveData<List<String>> = _additionalPhotos

    var pendingPhotoUri: String? = null
    var currentPetId: Long = -1L
    var currentUserId: Long = -1L

    fun loadAdditionalPhotos(petId: Long) {
        viewModelScope.launch {
            try {
                val response = repository.getAdditionalPhotos(petId)
                if (response.isSuccessful) {
                    _additionalPhotos.value = response.body() ?: emptyList()
                }
            } catch (_: Exception) { }
        }
    }

    fun uploadAdditionalPhotos(petId: Long, uris: List<Uri>, context: Context) {
        viewModelScope.launch {
            uploadAdditionalPhotosInternal(petId, uris, context)
            loadAdditionalPhotos(petId)
        }
    }

    fun deleteAdditionalPhoto(petId: Long, photoName: String) {
        viewModelScope.launch {
            try {
                val response = repository.deleteAdditionalPhoto(petId, photoName)
                if (response.isSuccessful) {
                    _additionalPhotos.value = _additionalPhotos.value.orEmpty().filter { it != photoName }
                } else {
                    _errorMessage.value = "No se pudo eliminar la foto (${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            }
        }
    }

    private suspend fun uploadAdditionalPhotosInternal(petId: Long, uris: List<Uri>, context: Context) {
        if (uris.isEmpty()) return
        val parts = uris.mapNotNull { uri -> buildFilePart(uri, context) }
        if (parts.isEmpty()) return
        try {
            val response = repository.uploadAdditionalPhotos(petId, parts)
            if (!response.isSuccessful) {
                _errorMessage.value = "Error al subir fotos adicionales (${response.code()})"
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error de conexión: ${e.message}"
        }
    }

    private suspend fun buildFilePart(uri: Uri, context: Context): MultipartBody.Part? = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val extension = when {
            mimeType.contains("png", ignoreCase = true) -> "png"
            mimeType.contains("webp", ignoreCase = true) -> "webp"
            else -> "jpg"
        }
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        MultipartBody.Part.createFormData(
            name = "files",
            filename = "pet_extra_${System.currentTimeMillis()}.$extension",
            body = requestBody
        )
    }

    fun savePet(
        name: String, speciesValue: String, breed: String?, color: String?,
        sexValue: String?, sizeValue: String?, birthDate: String?,
        weight: Double?, description: String?, isNeutered: Boolean,
        allergies: String?, medicalNotes: String?,
        photoUri: String? = null,
        additionalPhotoUris: List<Uri> = emptyList(),
        context: Context
    ) {
        pendingPhotoUri = photoUri
        if (currentPetId > 0) {
            updatePet(
                petId = currentPetId, name = name, species = speciesValue,
                breed = breed, color = color, sex = sexValue, size = sizeValue,
                birthDate = birthDate, weight = weight, description = description,
                isNeutered = isNeutered,
                allergies = allergies,
                medicalNotes = medicalNotes,
                photoUri = photoUri, additionalPhotoUris = additionalPhotoUris, context = context
            )
        } else {
            createPet(
                userId = currentUserId, name = name, species = speciesValue,
                breed = breed, color = color, sex = sexValue, size = sizeValue,
                birthDate = birthDate, weight = weight, description = description,
                isNeutered = isNeutered,
                allergies = allergies,
                medicalNotes = medicalNotes,
                photoUri = photoUri, additionalPhotoUris = additionalPhotoUris, context = context
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
        allergies: String?, medicalNotes: String?,
        photoUri: String? = null,
        additionalPhotoUris: List<Uri> = emptyList(),
        context: Context
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
                    val createdPetId = resolveCreatedPetId(userId, name)
                    if (createdPetId != null) {
                        if (!photoUri.isNullOrBlank()) {
                            PetPhotoStore.save(context, createdPetId, photoUri)
                            uploadPetMainPicture(createdPetId, photoUri, context)
                        }
                        if (additionalPhotoUris.isNotEmpty()) {
                            uploadAdditionalPhotosInternal(createdPetId, additionalPhotoUris, context)
                        }
                    }
                    _saveSuccess.value = true
                } else {
                    _errorMessage.value = if (response.code() == 401) {
                        "Sesion expirada. Inicia sesion nuevamente."
                    } else {
                        "Error creating pet: ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun resolveCreatedPetId(userId: Long, name: String): Long? {
        try {
            val response = repository.getPetsByUserId(userId)
            if (response.isSuccessful) {
                val newPet = response.body()
                    ?.filter { it.name.equals(name, ignoreCase = true) }
                    ?.maxByOrNull { it.id }
                _createdPetId.value = newPet?.id
                return newPet?.id
            }
        } catch (_: Exception) { }
        return null
    }

    fun updatePet(
        petId: Long, name: String?, species: String?, breed: String?,
        color: String?, sex: String?, size: String?, birthDate: String?,
        weight: Double?, description: String?, isNeutered: Boolean?,
        allergies: String?, medicalNotes: String?,
        photoUri: String? = null,
        additionalPhotoUris: List<Uri> = emptyList(),
        context: Context
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
                    if (!photoUri.isNullOrBlank()) {
                        try {
                            PetPhotoStore.save(context, petId, photoUri)
                            uploadPetMainPicture(petId, photoUri, context)
                        } catch (e: Exception) {
                            _errorMessage.value = "Pet updated, but photo upload failed: ${e.message}"
                        }
                    }
                    if (additionalPhotoUris.isNotEmpty()) {
                        uploadAdditionalPhotosInternal(petId, additionalPhotoUris, context)
                    }
                    _saveSuccess.value = true
                    loadPet(petId)
                } else {
                    _errorMessage.value = if (response.code() == 401) {
                        "Sesion expirada. Inicia sesion nuevamente."
                    } else {
                        "Error updating pet: ${response.code()}"
                    }
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

    private suspend fun uploadPetMainPicture(petId: Long, uriString: String, context: Context) {
        val uri = runCatching { uriString.toUri() }.getOrNull() ?: return

        val filePart = withContext(Dispatchers.IO) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val extension = when {
                mimeType.contains("png", ignoreCase = true) -> "png"
                mimeType.contains("webp", ignoreCase = true) -> "webp"
                else -> "jpg"
            }
            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData(
                name = "file",
                filename = "pet_main.$extension",
                body = requestBody
            )
        } ?: return

        val response = repository.uploadPetMainPicture(petId, filePart)
        if (!response.isSuccessful) {
            _errorMessage.value = "Error uploading photo (${response.code()})"
        }
    }
}
