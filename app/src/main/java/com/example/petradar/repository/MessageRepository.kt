package com.example.petradar.repository

import com.example.petradar.api.MessageCreateModel
import com.example.petradar.api.MessageUpdateModel
import com.example.petradar.api.RetrofitClient

class MessageRepository {
    private val api = RetrofitClient.apiService

    suspend fun getAdoptionAnimalConversation(
        adoptionAnimalId: Long,
        recipientId: Long,
        senderId: Long
    ) = api.getAdoptionAnimalConversation(adoptionAnimalId, recipientId, senderId)

    suspend fun sendMessage(message: MessageCreateModel) = api.sendMessage(message)

    suspend fun markAsRead(id: Long, readDate: String) =
        api.updateMessage(id, MessageUpdateModel(read = true, readDate = readDate))
}
