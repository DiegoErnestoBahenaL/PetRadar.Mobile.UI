package com.petradar.mobileui.repository

import com.petradar.mobileui.api.MatchUpdateModel
import com.petradar.mobileui.api.RetrofitClient

class MatchRepository {
    private val api = RetrofitClient.apiService

    suspend fun getByUserId(userId: Long) = api.getMatchesByUserId(userId)

    suspend fun dismissMatch(matchId: Long) =
        api.updateMatch(matchId, MatchUpdateModel(status = "Dismissed"))
}
