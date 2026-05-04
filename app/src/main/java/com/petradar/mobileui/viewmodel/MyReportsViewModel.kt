package com.petradar.mobileui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petradar.mobileui.api.MatchViewModel
import com.petradar.mobileui.api.ReportUpdateModel
import com.petradar.mobileui.api.ReportViewModel
import com.petradar.mobileui.repository.MatchRepository
import com.petradar.mobileui.repository.MessageRepository
import com.petradar.mobileui.repository.ReportRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MyReportsViewModel : ViewModel() {

    private val reportRepository = ReportRepository()
    private val matchRepository = MatchRepository()
    private val messageRepository = MessageRepository()

    private val _reports = MutableLiveData<List<ReportViewModel>>(emptyList())
    val reports: LiveData<List<ReportViewModel>> = _reports

    private val _matchesByReportId = MutableLiveData<Map<Long, List<MatchViewModel>>>(emptyMap())
    val matchesByReportId: LiveData<Map<Long, List<MatchViewModel>>> = _matchesByReportId

    private val _unreadCountByMatchId = MutableLiveData<Map<Long, Int>>(emptyMap())
    val unreadCountByMatchId: LiveData<Map<Long, Int>> = _unreadCountByMatchId

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _deleteSuccess = MutableLiveData<Long?>()
    val deleteSuccess: LiveData<Long?> = _deleteSuccess

    private val _dismissSuccess = MutableLiveData<Long?>()
    val dismissSuccess: LiveData<Long?> = _dismissSuccess

    fun loadReports(userId: Long) {
        if (userId <= 0) {
            _errorMessage.value = "Usuario no identificado"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val reportsDeferred = async { reportRepository.getByUserId(userId) }
                val matchesDeferred = async { matchRepository.getByUserId(userId) }

                val reportsResponse = reportsDeferred.await()
                val matchesResponse = matchesDeferred.await()

                if (reportsResponse.isSuccessful) {
                    _reports.value = reportsResponse.body() ?: emptyList()
                } else {
                    _errorMessage.value = "No se pudieron cargar los reportes (${reportsResponse.code()})"
                }

                if (matchesResponse.isSuccessful) {
                    val allMatches = matchesResponse.body() ?: emptyList()
                    val map = mutableMapOf<Long, MutableList<MatchViewModel>>()
                    for (match in allMatches) {
                        map.getOrPut(match.lostReport.id) { mutableListOf() }.add(match)
                        map.getOrPut(match.strayReport.id) { mutableListOf() }.add(match)
                    }
                    _matchesByReportId.value = map
                    loadUnreadCounts(userId, allMatches)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadUnreadCounts(userId: Long, allMatches: List<MatchViewModel>) {
        viewModelScope.launch {
            val counts = mutableMapOf<Long, Int>()
            val tasks = allMatches.map { match ->
                val otherUserId = if (match.lostReport.userId == userId)
                    match.strayReport.userId
                else
                    match.lostReport.userId
                match.id to async {
                    runCatching {
                        val r = messageRepository.getMatchConversation(match.id, otherUserId, userId)
                        if (r.isSuccessful) r.body().orEmpty()
                            .count { !it.read && it.recipientId == userId }
                        else 0
                    }.getOrDefault(0)
                }
            }
            for ((matchId, deferred) in tasks) {
                val count = deferred.await()
                if (count > 0) counts[matchId] = count
            }
            _unreadCountByMatchId.value = counts
        }
    }

    fun deleteReport(reportId: Long) {
        viewModelScope.launch {
            try {
                val response = reportRepository.delete(reportId)
                if (response.isSuccessful) {
                    _reports.value = _reports.value.orEmpty().filter { it.id != reportId }
                    _deleteSuccess.value = reportId
                } else {
                    _errorMessage.value = "No se pudo eliminar el reporte (${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            }
        }
    }

    fun clearDeleteSuccess() {
        _deleteSuccess.value = null
    }

    fun dismissReport(reportId: Long) {
        viewModelScope.launch {
            try {
                val response = reportRepository.update(reportId, ReportUpdateModel(reportStatus = "Dismissed"))
                if (response.isSuccessful) {
                    _reports.value = _reports.value.orEmpty().map {
                        if (it.id == reportId) it.copy(reportStatus = "Dismissed") else it
                    }
                    _dismissSuccess.value = reportId
                } else {
                    _errorMessage.value = "No se pudo descartar el reporte (${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            }
        }
    }

    fun clearDismissSuccess() {
        _dismissSuccess.value = null
    }
}