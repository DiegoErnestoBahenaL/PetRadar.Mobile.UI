package com.example.petradar.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petradar.api.ReportUpdateModel
import com.example.petradar.api.ReportViewModel
import com.example.petradar.repository.ReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ReportEditViewModel : ViewModel() {

    private val repository = ReportRepository()

    private val _report = MutableLiveData<ReportViewModel?>()
    val report: LiveData<ReportViewModel?> = _report

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _saveSuccess = MutableLiveData(false)
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _additionalPhotos = MutableLiveData<List<String>>(emptyList())
    val additionalPhotos: LiveData<List<String>> = _additionalPhotos

    fun loadReport(reportId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = repository.getById(reportId)
                if (response.isSuccessful) {
                    _report.value = response.body()
                } else {
                    _errorMessage.value = "No se pudo cargar el reporte (${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAdditionalPhotos(reportId: Long) {
        viewModelScope.launch {
            try {
                val response = repository.getAdditionalPhotos(reportId)
                if (response.isSuccessful) {
                    _additionalPhotos.value = response.body() ?: emptyList()
                }
            } catch (_: Exception) { /* non-critical */ }
        }
    }

    fun updateReport(
        reportId: Long,
        request: ReportUpdateModel,
        photoUri: Uri? = null,
        additionalPhotoUris: List<Uri> = emptyList(),
        context: Context? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = repository.update(reportId, request)
                if (response.isSuccessful) {
                    if (context != null) {
                        if (photoUri != null) uploadMainPictureInternal(reportId, photoUri, context)
                        if (additionalPhotoUris.isNotEmpty()) uploadAdditionalPhotosInternal(reportId, additionalPhotoUris, context)
                    }
                    _saveSuccess.value = true
                } else {
                    _errorMessage.value = "No se pudo guardar el reporte (${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAdditionalPhoto(reportId: Long, photoName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.deleteAdditionalPhoto(reportId, photoName)
                if (response.isSuccessful) {
                    loadAdditionalPhotos(reportId)
                } else {
                    _errorMessage.value = "Error al eliminar la foto: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun uploadMainPictureInternal(reportId: Long, photoUri: Uri, context: Context) {
        runCatching {
            val part = buildFilePart(context, photoUri, "file", "main.jpg") ?: return
            repository.uploadMainPicture(reportId, part)
        }
    }

    private suspend fun uploadAdditionalPhotosInternal(reportId: Long, uris: List<Uri>, context: Context) {
        runCatching {
            val parts = uris.mapIndexedNotNull { index, uri ->
                buildFilePart(context, uri, "files", "extra_$index.jpg")
            }
            if (parts.isNotEmpty()) repository.uploadAdditionalPhotos(reportId, parts)
        }
    }

    private suspend fun buildFilePart(
        context: Context, uri: Uri, fieldName: String, fileName: String
    ): MultipartBody.Part? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext null
            val mediaType = context.contentResolver.getType(uri)?.toMediaTypeOrNull()
                ?: "image/jpeg".toMediaTypeOrNull()
            val requestBody = bytes.toRequestBody(mediaType)
            MultipartBody.Part.createFormData(fieldName, fileName, requestBody)
        }.getOrNull()
    }
}
