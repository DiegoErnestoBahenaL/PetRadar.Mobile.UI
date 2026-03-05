package com.example.petradar.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petradar.api.AdoptionAnimalViewModel
import com.example.petradar.repository.AdoptionAnimalRepository
import kotlinx.coroutines.launch

/**
 * ViewModel that manages the business logic for the adoption animals list.
 *
 * Responsibilities:
 *  - Load all adoption animals from GET /api/AdoptionAnimals.
 *  - Delete an adoption animal via DELETE /api/AdoptionAnimals/{id} and reload the list.
 *  - Maintain UI state (loading, error, delete success) as LiveData.
 *
 * Exposed LiveData:
 *  - [animals]       → list of adoption animals.
 *  - [isLoading]     → true while a network request is in progress.
 *  - [errorMessage]  → error message for the UI; null when there is no error.
 *  - [deleteSuccess] → true immediately after a successful deletion.
 */
class AdoptionAnimalListViewModel : ViewModel() {

    private val repository = AdoptionAnimalRepository()

    private val _animals = MutableLiveData<List<AdoptionAnimalViewModel>>(emptyList())
    /** List of adoption animals. Empty until the first load completes. */
    val animals: LiveData<List<AdoptionAnimalViewModel>> = _animals

    private val _isLoading = MutableLiveData<Boolean>()
    /** true while a network request is active. */
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    /** Error message to display to the user; null when there is no error. */
    val errorMessage: LiveData<String?> = _errorMessage

    private val _deleteSuccess = MutableLiveData<Boolean>()
    /** Emits true immediately after an adoption animal is successfully deleted. */
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess

    /**
     * Loads all adoption animals from the API.
     * Endpoint: GET /api/AdoptionAnimals
     */
    fun loadAnimals() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = repository.getAll()
                if (response.isSuccessful) {
                    _animals.value = response.body() ?: emptyList()
                } else {
                    _errorMessage.value = "Error loading adoption animals: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Deletes an adoption animal from the system and reloads the updated list.
     * Endpoint: DELETE /api/AdoptionAnimals/{id}
     *
     * @param animalId ID of the adoption animal to delete.
     */
    fun deleteAnimal(animalId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = repository.delete(animalId)
                if (response.isSuccessful) {
                    _deleteSuccess.value = true
                    loadAnimals()
                } else {
                    _errorMessage.value = "Error deleting adoption animal: ${response.code()}"
                    _deleteSuccess.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error: ${e.message}"
                _deleteSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}

