package com.example.petradar.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petradar.api.UserPetViewModel
import com.example.petradar.repository.PetRepository
import kotlinx.coroutines.launch

/**
 * ViewModel that manages the business logic for the user's pet list.
 *
 * Responsibilities:
 *  - Load the user's pets from GET /api/UserPets/user/{userId}.
 *  - Delete a pet via DELETE /api/UserPets/{id} and reload the list.
 *  - Maintain UI state (loading, error, delete success) as LiveData.
 *
 * Exposed LiveData:
 *  - [pets]          → list of the user's pets.
 *  - [isLoading]     → true while a network request is in progress.
 *  - [errorMessage]  → error message for the UI; null when there is no error.
 *  - [deleteSuccess] → true immediately after a successful deletion.
 */
class PetViewModel : ViewModel() {

    private val repository = PetRepository()

    private val _pets = MutableLiveData<List<UserPetViewModel>>()
    /** List of the user's pets. Empty until the first load completes. */
    val pets: LiveData<List<UserPetViewModel>> = _pets

    private val _isLoading = MutableLiveData<Boolean>()
    /** true while a network request is active. */
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    /** Error message to display to the user; null when there is no error. */
    val errorMessage: LiveData<String?> = _errorMessage

    private val _deleteSuccess = MutableLiveData<Boolean>()
    /** Emits true immediately after a pet is successfully deleted. */
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess

    /**
     * Loads the user's pet list from the API.
     * Endpoint: GET /api/UserPets/user/{userId}
     *
     * Also called from [com.example.petradar.PetsActivity.onResume] to reflect recent changes.
     *
     * @param userId ID of the user whose pets should be loaded.
     */
    fun loadPets(userId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = repository.getPetsByUserId(userId)
                if (response.isSuccessful) {
                    _pets.value = response.body() ?: emptyList()
                } else {
                    _errorMessage.value = "Error loading pets: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Deletes a pet from the system and reloads the updated list.
     * Endpoint: DELETE /api/UserPets/{id}
     *
     * After a successful deletion, [loadPets] is called to update the UI
     * without requiring the user to do a manual pull-to-refresh.
     *
     * @param petId  ID of the pet to delete.
     * @param userId Owner user ID (needed to reload the list).
     */
    fun deletePet(petId: Long, userId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = repository.deletePet(petId)
                if (response.isSuccessful) {
                    _deleteSuccess.value = true
                    // Reload the list to reflect the deletion in the UI.
                    loadPets(userId)
                } else {
                    _errorMessage.value = "Error deleting pet: ${response.code()}"
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
