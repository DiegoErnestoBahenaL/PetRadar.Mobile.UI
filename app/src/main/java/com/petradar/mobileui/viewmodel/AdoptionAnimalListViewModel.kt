package com.petradar.mobileui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petradar.mobileui.api.AdoptionAnimalViewModel
import com.petradar.mobileui.api.AdoptionRequest
import com.petradar.mobileui.repository.AdoptionAnimalRepository
import com.petradar.mobileui.repository.MessageRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class AdoptionAnimalListViewModel : ViewModel() {

    private val repository = AdoptionAnimalRepository()
    private val messageRepository = MessageRepository()

    private val _animals = MutableLiveData<List<AdoptionAnimalViewModel>>(emptyList())
    val animals: LiveData<List<AdoptionAnimalViewModel>> = _animals

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess

    private val _adoptSuccess = MutableLiveData<Boolean?>(null)
    val adoptSuccess: LiveData<Boolean?> = _adoptSuccess

    private val _unreadCountByAnimalId = MutableLiveData<Map<Long, Int>>(emptyMap())
    val unreadCountByAnimalId: LiveData<Map<Long, Int>> = _unreadCountByAnimalId

    private var storedUserId: Long = -1L

    fun loadAnimals(currentUserId: Long = -1L) {
        if (currentUserId > 0) storedUserId = currentUserId
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = repository.getAll()
                if (response.isSuccessful) {
                    val animals = response.body() ?: emptyList()
                    _animals.value = animals
                    if (storedUserId > 0) loadUnreadCounts(animals, storedUserId)
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

    private fun loadUnreadCounts(animals: List<AdoptionAnimalViewModel>, currentUserId: Long) {
        viewModelScope.launch {
            val counts = mutableMapOf<Long, Int>()
            val tasks: List<Pair<Long, kotlinx.coroutines.Deferred<Int>>> = animals.flatMap { animal ->
                val isOwner = animal.shelterId == currentUserId
                if (isOwner) {
                    (animal.adoptionRequests ?: emptyList()).map { request ->
                        animal.id to async {
                            runCatching {
                                val r = messageRepository.getAdoptionAnimalConversation(
                                    animal.id, request.userId, currentUserId
                                )
                                if (r.isSuccessful) r.body().orEmpty()
                                    .count { !it.read && it.recipientId == currentUserId }
                                else 0
                            }.getOrDefault(0)
                        }
                    }
                } else if (animal.adoptionRequests?.any { it.userId == currentUserId } == true) {
                    listOf(animal.id to async {
                        runCatching {
                            val r = messageRepository.getAdoptionAnimalConversation(
                                animal.id, animal.shelterId, currentUserId
                            )
                            if (r.isSuccessful) r.body().orEmpty()
                                .count { !it.read && it.recipientId == currentUserId }
                            else 0
                        }.getOrDefault(0)
                    })
                } else emptyList()
            }
            for ((animalId, deferred) in tasks) {
                val count = deferred.await()
                if (count > 0) counts[animalId] = (counts[animalId] ?: 0) + count
            }
            _unreadCountByAnimalId.value = counts
        }
    }

    fun deleteAnimal(animalId: Long) {
        _animals.value = _animals.value?.filter { it.id != animalId }
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
                    loadAnimals()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error: ${e.message}"
                _deleteSuccess.value = false
                loadAnimals()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitAdoptionRequest(animalId: Long, request: AdoptionRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = repository.submitAdoptionRequest(animalId, request)
                if (response.isSuccessful) {
                    _adoptSuccess.value = true
                    loadAnimals()
                } else {
                    _adoptSuccess.value = false
                    _errorMessage.value = "Error al enviar la solicitud: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun approveAdoptionRequest(animalId: Long, adopterId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = repository.approveAdoptionRequest(animalId, adopterId)
                if (response.isSuccessful) {
                    loadAnimals()
                } else {
                    _errorMessage.value = "Error al aprobar la solicitud: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}