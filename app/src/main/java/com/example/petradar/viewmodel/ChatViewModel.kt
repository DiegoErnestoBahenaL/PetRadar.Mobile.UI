package com.example.petradar.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petradar.api.MessageCreateModel
import com.example.petradar.api.MessageModel
import com.example.petradar.repository.MessageRepository
import kotlinx.coroutines.launch
import java.time.Instant

class ChatViewModel : ViewModel() {

    private val repository = MessageRepository()

    private val _messages = MutableLiveData<List<MessageModel>>(emptyList())
    val messages: LiveData<List<MessageModel>> = _messages

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var lastAnimalId = -1L
    private var lastRecipientId = -1L
    private var lastSenderId = -1L

    fun loadMessages(adoptionAnimalId: Long, recipientId: Long, senderId: Long) {
        lastAnimalId = adoptionAnimalId
        lastRecipientId = recipientId
        lastSenderId = senderId
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getAdoptionAnimalConversation(
                    adoptionAnimalId, recipientId, senderId
                )
                if (response.isSuccessful) {
                    _messages.value = (response.body() ?: emptyList())
                        .sortedBy { it.sentAt }
                } else {
                    _errorMessage.value = "Error al cargar mensajes: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(senderId: Long, recipientId: Long, content: String, adoptionAnimalId: Long) {
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                val response = repository.sendMessage(
                    MessageCreateModel(
                        senderId = senderId,
                        recipientId = recipientId,
                        content = content,
                        adoptionAnimalId = adoptionAnimalId
                    )
                )
                if (response.isSuccessful) {
                    loadMessages(lastAnimalId, lastRecipientId, lastSenderId)
                } else {
                    _errorMessage.value = "Error al enviar mensaje: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            }
        }
    }

    fun markAsRead(messageId: Long) {
        viewModelScope.launch {
            runCatching {
                repository.markAsRead(messageId, Instant.now().toString())
            }
        }
    }
}
