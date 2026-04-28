package com.petradar.mobileui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petradar.mobileui.api.models.UpdateProfileRequest
import com.petradar.mobileui.api.models.UserProfile
import com.petradar.mobileui.repository.UserRepository
import com.petradar.mobileui.utils.AuthManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProfileViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> = _updateSuccess

    private val _photoUploadSuccess = MutableLiveData<Boolean?>()
    /** Emits true after a profile picture is uploaded successfully. */
    val photoUploadSuccess: LiveData<Boolean?> = _photoUploadSuccess

    fun loadUserProfile(userId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = repository.getUserById(userId)
                if (response.isSuccessful) {
                    val user = response.body()
                    _userProfile.value = user
                    // Keep the cached photo URL up to date so HomeScreen shows it on resume.
                    user?.profilePhotoURL?.let { url ->
                        // We need a Context; use the ViewModel's Application context isn't
                        // available here, so we expose the URL via LiveData and let the
                        // Activity/Screen save it via AuthManager on observe.
                    }
                } else {
                    _errorMessage.value = "Error loading profile: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(
        userId: Long,
        name: String?,
        lastName: String?,
        phoneNumber: String?,
        email: String? = null,
        password: String? = null,
        context: Context? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _updateSuccess.value = false
            try {
                val request = UpdateProfileRequest(
                    name = name, lastName = lastName, phoneNumber = phoneNumber,
                    email = email, password = password
                )
                val response = repository.updateUser(userId, request)
                if (response.isSuccessful) {
                    _userProfile.value = _userProfile.value?.copy(
                        name = name ?: _userProfile.value?.name ?: "",
                        lastName = lastName ?: _userProfile.value?.lastName,
                        phoneNumber = phoneNumber ?: _userProfile.value?.phoneNumber
                    )
                    context?.let {
                        AuthManager.updateUserInfo(
                            it,
                            name = name ?: _userProfile.value?.name,
                            email = email ?: _userProfile.value?.email
                        )
                    }
                    _updateSuccess.value = true
                    loadUserProfile(userId)
                } else {
                    _errorMessage.value = "Update error: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Uploads a profile picture to PUT /api/Users/{id}/profilepicture.
     * Reads the image bytes from the content URI, wraps them as multipart and sends them.
     * On success reloads the profile so profilePhotoURL is updated.
     *
     * @param userId  The user's ID.
     * @param uri     Content URI of the selected image (from gallery or camera).
     * @param context Context needed to open the content resolver.
     */
    fun uploadProfilePicture(userId: Long, uri: Uri, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // Read bytes from the URI via the content resolver.
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: run {
                        _errorMessage.value = "No se pudo leer la imagen seleccionada."
                        _isLoading.value = false
                        return@launch
                    }

                // Determine MIME type; default to JPEG.
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val extension = if (mimeType.contains("png")) "png" else "jpg"

                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData(
                    name = "file",
                    filename = "profile.$extension",
                    body = requestBody
                )

                val response = repository.uploadProfilePicture(userId, filePart)
                if (response.isSuccessful) {
                    _photoUploadSuccess.value = true
                    // Reload profile so the new photoURL is reflected immediately.
                    loadUserProfile(userId)
                } else {
                    _errorMessage.value = "Error al subir la foto (${response.code()}). Inténtalo de nuevo."
                    _photoUploadSuccess.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
                _photoUploadSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearUpdateSuccess() { _updateSuccess.value = false }
    fun clearPhotoUploadSuccess() { _photoUploadSuccess.value = null }
}
