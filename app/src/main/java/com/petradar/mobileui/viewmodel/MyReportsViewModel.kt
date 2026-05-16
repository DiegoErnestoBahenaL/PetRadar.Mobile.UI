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

    private val _dismissMatchSuccess = MutableLiveData<Long?>()
    val dismissMatchSuccess: LiveData<Long?> = _dismissMatchSuccess

    private val _confirmMatchSuccess = MutableLiveData<Long?>()
    val confirmMatchSuccess: LiveData<Long?> = _confirmMatchSuccess

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
                    _reports.value = (reportsResponse.body() ?: emptyList())
                        .filter { it.reportStatus?.lowercase() != "resolved" }
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
                        val r = messageRepository.getMatchUnreadCount(match.id, userId, otherUserId)
                        if (r.isSuccessful) r.body()?.unreadMessagesCount ?: 0 else 0
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

    fun dismissMatch(matchId: Long) {
        viewModelScope.launch {
            try {
                val response = matchRepository.dismissMatch(matchId)
                if (response.isSuccessful) {
                    val updatedMap = _matchesByReportId.value.orEmpty().toMutableMap()
                    for ((reportId, matches) in updatedMap.toMap()) {
                        val filtered = matches.filter { it.id != matchId }
                        if (filtered.isEmpty()) updatedMap.remove(reportId)
                        else updatedMap[reportId] = filtered
                    }
                    _matchesByReportId.value = updatedMap
                    val updatedCounts = _unreadCountByMatchId.value.orEmpty().toMutableMap()
                    updatedCounts.remove(matchId)
                    _unreadCountByMatchId.value = updatedCounts
                    _dismissMatchSuccess.value = matchId
                } else {
                    _errorMessage.value = "No se pudo descartar la coincidencia (${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            }
        }
    }

    fun clearDismissMatchSuccess() {
        _dismissMatchSuccess.value = null
    }

    fun confirmMatch(matchId: Long) {
        viewModelScope.launch {
            try {
                val response = matchRepository.confirmMatch(matchId)
                if (response.isSuccessful) {
                    val confirmedMatch = _matchesByReportId.value.orEmpty()
                        .values.flatten().find { it.id == matchId }

                    if (confirmedMatch != null) {
                        val lostReportId = confirmedMatch.lostReport.id
                        val strayReportId = confirmedMatch.strayReport.id
                        val resolvedUpdate = ReportUpdateModel(reportStatus = "Resolved")

                        async { runCatching { reportRepository.update(lostReportId, resolvedUpdate) } }
                        async { runCatching { reportRepository.update(strayReportId, resolvedUpdate) } }

                        _reports.value = _reports.value.orEmpty()
                            .filter { it.id != lostReportId && it.id != strayReportId }

                        val updatedMap = _matchesByReportId.value.orEmpty().toMutableMap()
                        updatedMap.remove(lostReportId)
                        updatedMap.remove(strayReportId)
                        _matchesByReportId.value = updatedMap
                    }

                    _confirmMatchSuccess.value = matchId
                } else {
                    _errorMessage.value = "No se pudo confirmar la coincidencia (${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            }
        }
    }

    fun clearConfirmMatchSuccess() {
        _confirmMatchSuccess.value = null
    }
}