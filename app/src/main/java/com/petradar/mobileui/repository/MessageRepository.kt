package com.petradar.mobileui.repository

import com.petradar.mobileui.api.MessageCreateModel
import com.petradar.mobileui.api.MessageUpdateModel
import com.petradar.mobileui.api.RetrofitClient

class MessageRepository {
    private val api = RetrofitClient.apiService

    suspend fun getAdoptionAnimalConversation(
        adoptionAnimalId: Long,
        recipientId: Long,
        senderId: Long
    ) = api.getAdoptionAnimalConversation(adoptionAnimalId, recipientId, senderId)

    suspend fun getMatchConversation(
        matchId: Long,
        recipientId: Long,
        senderId: Long
    ) = api.getMatchConversation(matchId, recipientId, senderId)

    suspend fun sendMessage(message: MessageCreateModel) = api.sendMessage(message)

    suspend fun markAsRead(id: Long, readDate: String) =
        api.updateMessage(id, MessageUpdateModel(read = true, readDate = readDate))
}
