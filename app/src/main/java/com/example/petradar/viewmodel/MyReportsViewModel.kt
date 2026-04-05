package com.example.petradar.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petradar.api.ReportViewModel
import com.example.petradar.repository.ReportRepository
import kotlinx.coroutines.launch

class MyReportsViewModel : ViewModel() {

    private val repository = ReportRepository()

    private val _reports = MutableLiveData<List<ReportViewModel>>(emptyList())
    val reports: LiveData<List<ReportViewModel>> = _reports

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadReports(userId: Long) {
        if (userId <= 0) {
            _errorMessage.value = "Usuario no identificado"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = repository.getByUserId(userId)
                if (response.isSuccessful) {
                    _reports.value = response.body() ?: emptyList()
                } else {
                    _errorMessage.value = "No se pudieron cargar los reportes (${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

