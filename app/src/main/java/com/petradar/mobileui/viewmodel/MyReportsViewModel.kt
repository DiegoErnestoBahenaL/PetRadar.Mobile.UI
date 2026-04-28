package com.petradar.mobileui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petradar.mobileui.api.MatchViewModel
import com.petradar.mobileui.api.ReportViewModel
import com.petradar.mobileui.repository.MatchRepository
import com.petradar.mobileui.repository.ReportRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MyReportsViewModel : ViewModel() {

    private val reportRepository = ReportRepository()
    private val matchRepository = MatchRepository()

    private val _reports = MutableLiveData<List<ReportViewModel>>(emptyList())
    val reports: LiveData<List<ReportViewModel>> = _reports

    private val _matchesByReportId = MutableLiveData<Map<Long, List<MatchViewModel>>>(emptyMap())
    val matchesByReportId: LiveData<Map<Long, List<MatchViewModel>>> = _matchesByReportId

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _deleteSuccess = MutableLiveData<Long?>()
    val deleteSuccess: LiveData<Long?> = _deleteSuccess

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
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
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
}
