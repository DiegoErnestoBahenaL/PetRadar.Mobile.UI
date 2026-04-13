package com.example.petradar.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.net.toUri
import com.example.petradar.api.ReportCreateModel
import com.example.petradar.api.RetrofitClient
import com.example.petradar.api.UserPetViewModel
import com.example.petradar.repository.PetRepository
import com.example.petradar.repository.ReportRepository
import com.example.petradar.utils.PetImageUrlResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class LostPetReportViewModel : ViewModel() {

    private val petRepository = PetRepository()
    private val reportRepository = ReportRepository()

    private val _pet = MutableLiveData<UserPetViewModel?>()
    val pet: LiveData<UserPetViewModel?> = _pet

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _saveSuccess = MutableLiveData(false)
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _petAdditionalPhotoNames = MutableLiveData<List<String>>(emptyList())
    val petAdditionalPhotoNames: LiveData<List<String>> = _petAdditionalPhotoNames

    fun loadPet(petId: Long) {
        if (petId <= 0) return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = petRepository.getPetById(petId)
                if (response.isSuccessful) {
                    _pet.value = response.body()
                    // Also load additional photos for pre-populating the report form
                    runCatching {
                        val photosResponse = petRepository.getAdditionalPhotos(petId)
                        if (photosResponse.isSuccessful) {
                            _petAdditionalPhotoNames.value = photosResponse.body() ?: emptyList()
                        }
                    }
                } else {
                    _errorMessage.value = "No se pudo cargar la mascota (${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createLostReport(
        request: ReportCreateModel,
        photoUri: String? = null,
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
                                uploadReportMainPicture(createdReportId, photoUri, context)
                            }
                            // Auto-copy the pet's additional photos to the report
                            val petId = request.userPetId ?: 0L
                            val petPhotoNames = _petAdditionalPhotoNames.value.orEmpty()
                            if (petId > 0 && petPhotoNames.isNotEmpty()) {
                                copyPetPhotosToReport(petId, petPhotoNames, createdReportId)
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

    /** Downloads each pet additional photo and re-uploads it to the report's endpoint. */
    private suspend fun copyPetPhotosToReport(petId: Long, photoNames: List<String>, reportId: Long) {
        val api = RetrofitClient.apiService
        val parts = withContext(Dispatchers.IO) {
            photoNames.mapNotNull { photoName ->
                runCatching {
                    val url = PetImageUrlResolver.petAdditionalPhotoUrl(petId, photoName)
                    val downloadResponse = api.downloadFile(url)
                    if (!downloadResponse.isSuccessful) return@mapNotNull null
                    val bytes = downloadResponse.body()?.bytes() ?: return@mapNotNull null
                    val extension = photoName.substringAfterLast('.', "jpg").lowercase()
                    val mimeType = when (extension) {
                        "png" -> "image/png"
                        "webp" -> "image/webp"
                        else -> "image/jpeg"
                    }
                    val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("files", "report_$photoName", requestBody)
                }.getOrNull()
            }
        }
        if (parts.isEmpty()) return
        val uploadResponse = reportRepository.uploadAdditionalPhotos(reportId, parts)
        if (!uploadResponse.isSuccessful) {
            _errorMessage.value = "No se pudieron copiar las fotos adicionales (${uploadResponse.code()})"
        }
    }

    private suspend fun resolveCreatedReportId(userId: Long): Long? {
        val response = reportRepository.getByUserId(userId)
        if (!response.isSuccessful) return null
        return response.body()?.maxByOrNull { it.id }?.id
    }

    private suspend fun uploadReportMainPicture(reportId: Long, uriString: String, context: Context) {
        val uri = runCatching { uriString.toUri() }.getOrNull() ?: return
        val filePart = withContext(Dispatchers.IO) {
            val scheme = uri.scheme?.lowercase() ?: ""
            val isRemote = scheme == "http" || scheme == "https"
            val bytes: ByteArray?
            val mimeType: String
            if (isRemote) {
                val response = runCatching { RetrofitClient.apiService.downloadFile(uriString) }.getOrNull()
                bytes = response?.takeIf { it.isSuccessful }?.body()?.bytes()
                mimeType = "image/jpeg"
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
            MultipartBody.Part.createFormData(
                name = "file",
                filename = "report_main.$extension",
                body = requestBody
            )
        } ?: return

        val uploadResponse = reportRepository.uploadMainPicture(reportId, filePart)
        if (!uploadResponse.isSuccessful) {
            _errorMessage.value = "No se pudo subir la foto del reporte (${uploadResponse.code()})"
        }
    }
}

