package com.petradar.mobileui.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.net.toUri
import com.petradar.mobileui.api.ReportCreateModel
import com.petradar.mobileui.api.RetrofitClient
import com.petradar.mobileui.repository.ReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class StrayReportViewModel : ViewModel() {

    private val reportRepository = ReportRepository()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _saveSuccess = MutableLiveData(false)
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    fun createStrayReport(
        request: ReportCreateModel,
        photoUri: String? = null,
        additionalPhotoUris: List<String> = emptyList(),
        context: Context? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _saveSuccess.value = false
            try {
                val response = reportRepository.create(request)
                if (response.isSuccessful) {
                    if (context != null) {
                        val createdReportId = resolveCreatedReportId(request.userId)
                        if (createdReportId != null) {
                            if (!photoUri.isNullOrBlank()) {
                                uploadMainPicture(createdReportId, photoUri, context)
                            }
                            if (additionalPhotoUris.isNotEmpty()) {
                                uploadAdditionalPhotos(createdReportId, additionalPhotoUris, context)
                            }
                        }
                    }
                    _saveSuccess.value = true
                } else {
                    _errorMessage.value = "No se pudo crear el reporte (${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun resolveCreatedReportId(userId: Long): Long? {
        val response = reportRepository.getByUserId(userId)
        if (!response.isSuccessful) return null
        return response.body()?.maxByOrNull { it.id }?.id
    }

    private suspend fun uploadMainPicture(reportId: Long, uriString: String, context: Context) {
        val uri = runCatching { uriString.toUri() }.getOrNull() ?: return
        val filePart = withContext(Dispatchers.IO) {
            val scheme = uri.scheme?.lowercase() ?: ""
            val isRemote = scheme == "http" || scheme == "https"
            val bytes: ByteArray?
            val mimeType: String
            if (isRemote) {
                val response = runCatching { RetrofitClient.apiService.downloadFile(uriString) }.getOrNull()
                    ?.takeIf { it.isSuccessful } ?: return@withContext null
                bytes = response.body()?.bytes()
                mimeType = response.headers()["Content-Type"]
                    ?.substringBefore(";")?.trim() ?: "image/jpeg"
            } else {
                bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            }
            if (bytes == null) return@withContext null
            val extension = when {
                mimeType.contains("png", ignoreCase = true) -> "png"
                mimeType.contains("webp", ignoreCase = true) -> "webp"
                else -> "jpg"
            }
            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("file", "report_main.$extension", requestBody)
        } ?: return

        val uploadResponse = reportRepository.uploadMainPicture(reportId, filePart)
        if (!uploadResponse.isSuccessful) {
            _errorMessage.value = "No se pudo subir la foto principal (${uploadResponse.code()})"
        }
    }

    private suspend fun uploadAdditionalPhotos(
        reportId: Long,
        uriStrings: List<String>,
        context: Context
    ) {
        val parts = withContext(Dispatchers.IO) {
            uriStrings.mapIndexedNotNull { index, uriString ->
                runCatching {
                    val uri = uriString.toUri()
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@mapIndexedNotNull null
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val extension = when {
                        mimeType.contains("png", ignoreCase = true) -> "png"
                        mimeType.contains("webp", ignoreCase = true) -> "webp"
                        else -> "jpg"
                    }
                    val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                    MultipartBody.Part.createFormData(
                        "files",
                        "stray_photo_$index.$extension",
                        requestBody
                    )
                }.getOrNull()
            }
        }
        if (parts.isEmpty()) return
        val uploadResponse = reportRepository.uploadAdditionalPhotos(reportId, parts)
        if (!uploadResponse.isSuccessful) {
            _errorMessage.value = "No se pudieron subir las fotos adicionales (${uploadResponse.code()})"
        }
    }
}
