package com.example.petradar.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petradar.api.AdoptionAnimalCreateModel
import com.example.petradar.api.AdoptionAnimalUpdateModel
import com.example.petradar.api.AdoptionAnimalViewModel
import com.example.petradar.repository.AdoptionAnimalRepository
import kotlinx.coroutines.launch

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
 *  - [animal]       → animal data loaded in edit mode; null in creation mode.
 *  - [isLoading]    → true while a network request is in progress.
 *  - [errorMessage] → error message for the UI; null when there is no error.
 *  - [saveSuccess]  → true immediately after a successful create or update.
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
    /** Emits true immediately after an animal is successfully created or updated. */
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    /** ID of the adoption animal being edited. -1L indicates creation mode. */
    var currentAnimalId: Long = -1L
    /** ID of the shelter; required for the creation payload. */
    var currentShelterId: Long = -1L

    /**
     * URI of the photo chosen by the user before saving.
     * The Activity reads this when [saveSuccess] is true to persist it in AdoptionPhotoStore.
     */
    var pendingPhotoUri: String? = null

    /**
     * Main entry point for saving an adoption animal.
     * Decides whether to create or update based on [currentAnimalId].
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
        photoUri: String? = null
    ) {
        pendingPhotoUri = photoUri
        if (currentAnimalId > 0) {
            updateAnimal(
                animalId = currentAnimalId, name = name, species = speciesValue,
                breed = breed, color = color, sex = sexValue, size = sizeValue,
                approximateAge = approximateAge, weight = weight, description = description,
                isNeutered = isNeutered, personality = personality, goodWithKids = goodWithKids,
                goodWithDogs = goodWithDogs, goodWithCats = goodWithCats,
                isVaccinated = isVaccinated, needsSpecialCare = needsSpecialCare,
                specialCareDetails = specialCareDetails, status = status,
                adoptionDate = adoptionDate, adopterId = adopterId
            )
        } else {
            createAnimal(
                shelterId = currentShelterId, name = name, species = speciesValue,
                breed = breed, color = color, sex = sexValue, size = sizeValue,
                approximateAge = approximateAge, weight = weight, description = description,
                isNeutered = isNeutered, personality = personality, goodWithKids = goodWithKids,
                goodWithDogs = goodWithDogs, goodWithCats = goodWithCats,
                isVaccinated = isVaccinated, needsSpecialCare = needsSpecialCare,
                specialCareDetails = specialCareDetails
            )
        }
    }

    /**
     * Loads an existing adoption animal's data from the API.
     * Endpoint: GET /api/AdoptionAnimals/{id}
     * Only called in edit mode when [currentAnimalId] > 0.
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
                    _errorMessage.value = "Error loading animal: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Creates a new adoption animal via the API.
     * Endpoint: POST /api/AdoptionAnimals
     */
    private fun createAnimal(
        shelterId: Long, name: String, species: String, breed: String?,
        color: String?, sex: String?, size: String?, approximateAge: Double?,
        weight: Double?, description: String?, isNeutered: Boolean?,
        personality: String?, goodWithKids: Boolean?, goodWithDogs: Boolean?,
        goodWithCats: Boolean?, isVaccinated: Boolean?, needsSpecialCare: Boolean?,
        specialCareDetails: String?
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
                    _saveSuccess.value = true
                } else {
                    _errorMessage.value = "Error creating adoption animal: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates an existing adoption animal via the API.
     * Endpoint: PUT /api/AdoptionAnimals/{id}
     */
    private fun updateAnimal(
        animalId: Long, name: String?, species: String?, breed: String?,
        color: String?, sex: String?, size: String?, approximateAge: Double?,
        weight: Double?, description: String?, isNeutered: Boolean?,
        personality: String?, goodWithKids: Boolean?, goodWithDogs: Boolean?,
        goodWithCats: Boolean?, isVaccinated: Boolean?, needsSpecialCare: Boolean?,
        specialCareDetails: String?, status: String?, adoptionDate: String?,
        adopterId: Long?
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
                    _saveSuccess.value = true
                } else {
                    _errorMessage.value = "Error updating adoption animal: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}



