package com.example.petradar.viewmodel

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petradar.api.AdoptionAnimalCreateModel
import com.example.petradar.api.AdoptionAnimalUpdateModel
import com.example.petradar.api.AdoptionAnimalViewModel
import com.example.petradar.repository.AdoptionAnimalRepository
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * ViewModel that manages the business logic for creating and editing an adoption animal.
 *
 * Shared between creation and edit modes:
 *  - If [currentAnimalId] > 0 → edit mode (PUT /api/AdoptionAnimals/{id}).
 *  - If [currentAnimalId] ≤ 0 → creation mode (POST /api/AdoptionAnimals).
 *
 * The public entry point is [saveAnimal], which internally decides which
 * operation to execute based on [currentAnimalId].
 *
 * Exposed LiveData:
 *  - [animal]            → animal data loaded in edit mode; null in creation mode.
 *  - [isLoading]         → true while a network request is in progress.
 *  - [errorMessage]      → error message for the UI; null when there is no error.
 *  - [saveSuccess]       → true immediately after a successful create or update (including photo upload).
 *  - [additionalPhotos]  → list of additional photo URLs for the current animal.
 */
class AdoptionAnimalDetailViewModel : ViewModel() {

    private val repository = AdoptionAnimalRepository()

    private val _animal = MutableLiveData<AdoptionAnimalViewModel?>()
    /** Animal data in edit mode; null until loaded or in creation mode. */
    val animal: LiveData<AdoptionAnimalViewModel?> = _animal

    private val _isLoading = MutableLiveData<Boolean>()
    /** true while a network request is active. */
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    /** Error message to display to the user; null when there is no error. */
    val errorMessage: LiveData<String?> = _errorMessage

    private val _saveSuccess = MutableLiveData<Boolean>()
    /** Emits true after the animal and any pending photos are successfully saved. */
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _additionalPhotos = MutableLiveData<List<String>>(emptyList())
    /** List of additional photo URLs for the current animal. */
    val additionalPhotos: LiveData<List<String>> = _additionalPhotos

    /** ID of the adoption animal being edited. -1L indicates creation mode. */
    var currentAnimalId: Long = -1L
    /** ID of the shelter; required for the creation payload. */
    var currentShelterId: Long = -1L

    /**
     * Main entry point for saving an adoption animal.
     * Decides whether to create or update based on [currentAnimalId].
     *
     * @param photoUri             Optional URI of the main picture chosen by the user.
     * @param additionalPhotoUris  Optional list of URIs for additional photos to upload after save.
     * @param context              Required to read the photo bytes from each URI.
     */
    fun saveAnimal(
        name: String, speciesValue: String, breed: String?, color: String?,
        sexValue: String?, sizeValue: String?, approximateAge: Double?,
        weight: Double?, description: String?, isNeutered: Boolean?,
        personality: String?, goodWithKids: Boolean?, goodWithDogs: Boolean?,
        goodWithCats: Boolean?, isVaccinated: Boolean?, needsSpecialCare: Boolean?,
        specialCareDetails: String?,
        // Update-only fields
        status: String? = null, adoptionDate: String? = null, adopterId: Long? = null,
        photoUri: String? = null,
        additionalPhotoUris: List<Uri> = emptyList(),
        context: Context? = null
    ) {
        if (currentAnimalId > 0) {
            updateAnimal(
                animalId = currentAnimalId, name = name, species = speciesValue,
                breed = breed, color = color, sex = sexValue, size = sizeValue,
                approximateAge = approximateAge, weight = weight, description = description,
                isNeutered = isNeutered, personality = personality, goodWithKids = goodWithKids,
                goodWithDogs = goodWithDogs, goodWithCats = goodWithCats,
                isVaccinated = isVaccinated, needsSpecialCare = needsSpecialCare,
                specialCareDetails = specialCareDetails, status = status,
                adoptionDate = adoptionDate, adopterId = adopterId,
                photoUri = photoUri, additionalPhotoUris = additionalPhotoUris, context = context
            )
        } else {
            createAnimal(
                shelterId = currentShelterId, name = name, species = speciesValue,
                breed = breed, color = color, sex = sexValue, size = sizeValue,
                approximateAge = approximateAge, weight = weight, description = description,
                isNeutered = isNeutered, personality = personality, goodWithKids = goodWithKids,
                goodWithDogs = goodWithDogs, goodWithCats = goodWithCats,
                isVaccinated = isVaccinated, needsSpecialCare = needsSpecialCare,
                specialCareDetails = specialCareDetails,
                photoUri = photoUri, additionalPhotoUris = additionalPhotoUris, context = context
            )
        }
    }

    /**
     * Loads an existing adoption animal's data from the API.
     * Endpoint: GET /api/AdoptionAnimals/{id}
     *
     * @param animalId ID of the animal to load.
     */
    fun loadAnimal(animalId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = repository.getById(animalId)
                if (response.isSuccessful) {
                    _animal.value = response.body()
                } else {
                    _errorMessage.value = "Error al cargar el animal: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads additional photos for an existing animal.
     * Endpoint: GET /api/AdoptionAnimals/{id}/additionalphotos
     *
     * @param animalId Adoption animal ID.
     */
    fun loadAdditionalPhotos(animalId: Long) {
        viewModelScope.launch {
            try {
                val response = repository.getAdditionalPhotos(animalId)
                if (response.isSuccessful) {
                    _additionalPhotos.value = response.body() ?: emptyList()
                }
            } catch (_: Exception) { /* ignore, non-critical */ }
        }
    }

    /**
     * Uploads one or more additional photos for the animal.
     * Endpoint: PUT /api/AdoptionAnimals/{id}/additionalphotos
     *
     * @param animalId ID of the adoption animal.
     * @param uris     List of local content URIs selected by the user.
     * @param context  Required to read the bytes from each URI.
     */
    fun uploadAdditionalPhotos(animalId: Long, uris: List<Uri>, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val parts = uris.mapIndexedNotNull { index, uri ->
                    buildFilePart(context, uri, "files", "photo_$index.jpg")
                }
                if (parts.isNotEmpty()) {
                    val response = repository.uploadAdditionalPhotos(animalId, parts)
                    if (response.isSuccessful) {
                        loadAdditionalPhotos(animalId)
                    } else {
                        _errorMessage.value = "Error al subir fotos adicionales: ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Deletes a specific additional photo.
     * Endpoint: DELETE /api/AdoptionAnimals/{id}/additionalphotos/{photoName}
     *
     * @param animalId  ID of the adoption animal.
     * @param photoName File name of the photo to delete.
     */
    fun deleteAdditionalPhoto(animalId: Long, photoName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.deleteAdditionalPhoto(animalId, photoName)
                if (response.isSuccessful) {
                    loadAdditionalPhotos(animalId)
                } else {
                    _errorMessage.value = "Error al eliminar la foto: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun createAnimal(
        shelterId: Long, name: String, species: String, breed: String?,
        color: String?, sex: String?, size: String?, approximateAge: Double?,
        weight: Double?, description: String?, isNeutered: Boolean?,
        personality: String?, goodWithKids: Boolean?, goodWithDogs: Boolean?,
        goodWithCats: Boolean?, isVaccinated: Boolean?, needsSpecialCare: Boolean?,
        specialCareDetails: String?, photoUri: String?,
        additionalPhotoUris: List<Uri>, context: Context?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _saveSuccess.value = false
            try {
                val request = AdoptionAnimalCreateModel(
                    shelterId = shelterId, name = name, species = species,
                    breed = breed, color = color, sex = sex, size = size,
                    approximateAge = approximateAge, weight = weight, description = description,
                    isNeutered = isNeutered, personality = personality,
                    goodWithKids = goodWithKids, goodWithDogs = goodWithDogs,
                    goodWithCats = goodWithCats, isVaccinated = isVaccinated,
                    needsSpecialCare = needsSpecialCare, specialCareDetails = specialCareDetails
                )
                val response = repository.create(request)
                if (response.isSuccessful) {
                    // Try to extract the new animal ID from the Location header
                    val newId = response.headers()["Location"]
                        ?.trimEnd('/')
                        ?.substringAfterLast('/')
                        ?.toLongOrNull() ?: -1L

                    if (newId > 0 && context != null) {
                        if (!photoUri.isNullOrBlank()) {
                            uploadMainPictureInternal(newId, photoUri.toUri(), context)
                        }
                        if (additionalPhotoUris.isNotEmpty()) {
                            uploadAdditionalPhotosInternal(newId, additionalPhotoUris, context)
                        }
                    }
                    _saveSuccess.value = true
                } else {
                    _errorMessage.value = "Error al crear el animal: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateAnimal(
        animalId: Long, name: String?, species: String?, breed: String?,
        color: String?, sex: String?, size: String?, approximateAge: Double?,
        weight: Double?, description: String?, isNeutered: Boolean?,
        personality: String?, goodWithKids: Boolean?, goodWithDogs: Boolean?,
        goodWithCats: Boolean?, isVaccinated: Boolean?, needsSpecialCare: Boolean?,
        specialCareDetails: String?, status: String?, adoptionDate: String?,
        adopterId: Long?, photoUri: String?, additionalPhotoUris: List<Uri>, context: Context?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _saveSuccess.value = false
            try {
                val request = AdoptionAnimalUpdateModel(
                    name = name, species = species, breed = breed, color = color,
                    sex = sex, size = size, approximateAge = approximateAge, weight = weight,
                    description = description, isNeutered = isNeutered, personality = personality,
                    goodWithKids = goodWithKids, goodWithDogs = goodWithDogs,
                    goodWithCats = goodWithCats, isVaccinated = isVaccinated,
                    needsSpecialCare = needsSpecialCare, specialCareDetails = specialCareDetails,
                    status = status, adoptionDate = adoptionDate, adopterId = adopterId
                )
                val response = repository.update(animalId, request)
                if (response.isSuccessful) {
                    if (context != null) {
                        if (!photoUri.isNullOrBlank()) {
                            uploadMainPictureInternal(animalId, photoUri.toUri(), context)
                        }
                        if (additionalPhotoUris.isNotEmpty()) {
                            uploadAdditionalPhotosInternal(animalId, additionalPhotoUris, context)
                        }
                    }
                    _saveSuccess.value = true
                } else {
                    _errorMessage.value = "Error al actualizar el animal: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Uploads the main picture; errors are silent (save already succeeded). */
    private suspend fun uploadMainPictureInternal(animalId: Long, photoUri: Uri, context: Context) {
        runCatching {
            val part = buildFilePart(context, photoUri, "file", "main.jpg") ?: return
            repository.uploadMainPicture(animalId, part)
        }
    }

    /** Uploads additional photos; errors are silent (save already succeeded). */
    private suspend fun uploadAdditionalPhotosInternal(animalId: Long, uris: List<Uri>, context: Context) {
        runCatching {
            val parts = uris.mapIndexedNotNull { index, uri ->
                buildFilePart(context, uri, "files", "extra_$index.jpg")
            }
            if (parts.isNotEmpty()) repository.uploadAdditionalPhotos(animalId, parts)
        }
    }

    /** Reads a URI and builds a [MultipartBody.Part] for upload. Returns null on failure. */
    private fun buildFilePart(
        context: Context,
        uri: Uri,
        fieldName: String,
        fileName: String
    ): MultipartBody.Part? = runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val mediaType = context.contentResolver.getType(uri)?.toMediaTypeOrNull()
            ?: "image/jpeg".toMediaTypeOrNull()
        val requestBody = bytes.toRequestBody(mediaType)
        MultipartBody.Part.createFormData(fieldName, fileName, requestBody)
    }.getOrNull()
}
