package com.example.petradar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.petradar.api.models.LoginResponse
import com.example.petradar.api.models.UserProfile
import com.example.petradar.repository.AuthRepository
import com.example.petradar.repository.UserRepository
import com.example.petradar.utils.AuthManager
import kotlinx.coroutines.launch

/**
 * ViewModel para manejar la lógica de Login y Registro
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _loginResult = MutableLiveData<LoginResponse?>()

    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    private val _userProfile = MutableLiveData<UserProfile?>()

    private val _registerSuccess = MutableLiveData<Boolean>()

    private val _registerCredentials = MutableLiveData<Pair<String, String>?>()
    val registerCredentials: LiveData<Pair<String, String>?> = _registerCredentials

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Realizar login con username (email) y password
     * Después del login exitoso, busca el perfil del usuario para obtener el ID
     */
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _loginResult.value = null
            try {
                val response = authRepository.login(username, password)
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    _loginResult.value = loginResponse

                    // Save token immediately so subsequent calls include Authorization header
                    AuthManager.saveAuthToken(
                        getApplication(),
                        loginResponse.token,
                        loginResponse.refreshToken
                    )

                    // Fetch user profile (blocking) so userId is saved before navigating
                    fetchUserIdByEmail(username)

                    // Signal login success so the UI navigates to Home
                    _loginSuccess.value = true
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Datos incorrectos"
                        401 -> "Usuario o contraseña incorrectos"
                        404 -> "Usuario no encontrado"
                        500 -> "Error del servidor. Intenta más tarde"
                        else -> "Error al iniciar sesión: ${response.code()}"
                    }
                    _errorMessage.value = errorMsg
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    /**
     * Buscar el userId del usuario por email
     */
    private suspend fun fetchUserIdByEmail(email: String) {
        try {
            val response = userRepository.getAllUsers()
            if (response.isSuccessful) {
                val users = response.body()
                val user = users?.find { it.email.equals(email, ignoreCase = true) }
                if (user != null) {
                    val fullName = "${user.name} ${user.lastName ?: ""}".trim()
                    AuthManager.saveUserInfo(getApplication(), user.id ?: 0L, user.email, fullName)
                    _userProfile.value = user
                } else {
                    // User not found in list — save email as fallback
                    AuthManager.saveUserInfo(getApplication(), 0L, email, "")
                }
            } else {
                // API call failed — save email as fallback so app can still open
                AuthManager.saveUserInfo(getApplication(), 0L, email, "")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Network error — save email as fallback
            AuthManager.saveUserInfo(getApplication(), 0L, email, "")
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Registrar nuevo usuario
     * Nota: El API de PetRadar no devuelve token en el registro,
     * hay que hacer login después del registro exitoso
     */
    fun register(
        name: String,
        lastName: String?,
        email: String,
        password: String,
        phoneNumber: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _registerSuccess.value = false
            try {
                val response = authRepository.register(name, lastName, email, password, phoneNumber)
                if (response.isSuccessful) {
                    // Registro exitoso
                    _registerSuccess.value = true
                    // Guardar credenciales para hacer login automático
                    _registerCredentials.value = Pair(email, password)
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Datos inválidos. Verifica la información"
                        401 -> "El registro requiere permisos de administrador en este ambiente. Contacta al equipo de backend para crear tu cuenta."
                        409 -> "El email ya está registrado"
                        500 -> "Error del servidor. Intenta más tarde"
                        else -> "Error al registrarse: ${response.code()}"
                    }
                    _errorMessage.value = errorMsg
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Limpiar credenciales de registro
     */
    fun clearRegisterCredentials() {
        _registerCredentials.value = null
    }
}

