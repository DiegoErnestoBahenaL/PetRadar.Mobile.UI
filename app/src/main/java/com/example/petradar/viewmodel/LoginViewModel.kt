package com.example.petradar.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.petradar.api.models.LoginResponse
import com.example.petradar.api.models.UserProfile
import com.example.petradar.repository.AuthRepository
import com.example.petradar.repository.UserRepository
import com.example.petradar.utils.AuthManager
import com.example.petradar.utils.JwtUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel that manages the business logic for user login and registration.
 *
 * Extends [AndroidViewModel] (instead of [androidx.lifecycle.ViewModel]) because it needs
 * the [Application] context to call [AuthManager] (save/read SharedPreferences)
 * without causing memory leaks.
 *
 * Login flow:
 *  1. The UI calls [login] with email and password.
 *  2. POST /api/gate/Login is called via [AuthRepository].
 *  3. If successful, the token is saved with [AuthManager.saveAuthToken].
 *  4. The user profile is fetched from GET /api/Users to obtain the userId.
 *  5. The userId, email and name are saved with [AuthManager.saveUserInfo].
 *  6. `loginSuccess = true` is emitted → the Activity navigates to HomeActivity.
 *
 * Registration flow:
 *  1. The UI calls [register] with form data.
 *  2. POST /api/Users is called via [AuthRepository].
 *  3. If successful (201), credentials are stored in [registerCredentials].
 *  4. The Activity observes [registerCredentials] and triggers an automatic [login].
 *
 * Exposed LiveData:
 *  - [loginSuccess]        → true when login (or post-registration login) succeeds.
 *  - [registerCredentials] → (email, password) pair for automatic post-registration login.
 *  - [isLoading]           → true while a network operation is in progress.
 *  - [errorMessage]        → error message to display in the UI (Snackbar).
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    // Full login result (token); internal use only, not exposed directly.
    private val _loginResult = MutableLiveData<LoginResponse?>()

    private val _loginSuccess = MutableLiveData<Boolean>()
    /** Emits true when login or post-registration login succeeds. */
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    // User profile retrieved after login; internal use only.
    private val _userProfile = MutableLiveData<UserProfile?>()

    // Registration result (true = 201 Created); internal use only.
    private val _registerSuccess = MutableLiveData<Boolean>()

    private val _registerCredentials = MutableLiveData<Pair<String, String>?>()
    /**
     * Emits the (email, password) pair after a successful registration so that
     * the Activity can trigger an automatic login with those credentials.
     * Clear with [clearRegisterCredentials] to prevent re-triggering.
     */
    val registerCredentials: LiveData<Pair<String, String>?> = _registerCredentials

    private val _isLoading = MutableLiveData<Boolean>()
    /** true while a network request is in progress; useful for showing a spinner. */
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    /** Human-readable error message for the user; null when there is no error. */
    val errorMessage: LiveData<String?> = _errorMessage

    private val _emailNotVerified = MutableLiveData<String?>()
    /**
     * Emits the user's email when login was successful but the account is not yet
     * email-verified. The Activity/Screen should redirect to the email verification screen.
     * Null when not in that state.
     */
    val emailNotVerified: LiveData<String?> = _emailNotVerified

    /** Clears the emailNotVerified state (call after navigating to verification screen). */
    fun clearEmailNotVerified() {
        _emailNotVerified.value = null
    }

    /**
     * Signs in with the provided credentials.
     *
     * Internal steps:
     *  1. Calls the login endpoint.
     *  2. Saves the JWT in [AuthManager].
     *  3. Looks up the user profile by email to obtain the userId.
     *  4. Saves the userId in [AuthManager].
     *  5. Emits [loginSuccess] = true.
     *
     * @param username User's email (the API calls this field `username`).
     * @param password User's password.
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

                    // Save the token immediately so that the next requests
                    // (such as fetchUserIdByEmail) already include the Authorization header.
                    AuthManager.saveAuthToken(
                        getApplication(),
                        loginResponse.token,
                        loginResponse.refreshToken
                    )

                    // Fetch the user profile to obtain the userId and check emailVerified.
                    // loginSuccess / emailNotVerified are emitted inside this call.
                    fetchUserIdByEmail(username)
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Incorrect data"
                        401 -> "Wrong username or password"
                        404 -> "User not found"
                        500 -> "Server error. Please try again later"
                        else -> "Login error: ${response.code()}"
                    }
                    _errorMessage.value = errorMsg
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error: ${e.message}"
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    /**
     * Resolves the user's profile after login.
     *
     * Strategy:
     *  1. Try to decode the userId directly from the JWT payload (no extra API call).
     *  2. Use getUserById to fetch the full profile — this works for any authenticated user.
     *  3. If step 1 fails (claim not present), fall back to getAllUsers filtered by email.
     *
     * After obtaining the profile checks [UserProfile.emailVerified]:
     *  - Verified   → emits [loginSuccess].
     *  - Unverified → emits [emailNotVerified] so the UI navigates to the verification screen.
     */
    private suspend fun fetchUserIdByEmail(email: String) {
        try {
            val token = AuthManager.getAuthToken(getApplication())
            val userIdFromJwt = JwtUtils.getUserIdFromToken(token)
            Log.d("LoginViewModel", "fetchUserIdByEmail: email=$email, userIdFromJwt=$userIdFromJwt")

            var user: UserProfile? = null

            if (userIdFromJwt != null && userIdFromJwt > 0L) {
                val response = userRepository.getUserById(userIdFromJwt)
                Log.d("LoginViewModel", "getUserById($userIdFromJwt) → ${response.code()}")
                if (response.isSuccessful) {
                    user = response.body()
                    if (user != null) {
                        val fullName = "${user.name} ${user.lastName ?: ""}".trim()
                        AuthManager.saveUserInfo(getApplication(), userIdFromJwt, user.email, fullName)
                        AuthManager.saveProfilePhotoURL(getApplication(), user.profilePhotoURL)
                        _userProfile.value = user
                        Log.d("LoginViewModel", "Profile loaded: emailVerified=${user.emailVerified}")
                        Log.d("ProfilePhotoURL", "profilePhotoURL = '${user.profilePhotoURL}'")
                    }
                }
            }

            if (user == null) {
                Log.d("LoginViewModel", "Falling back to getAllUsers()")
                val listResponse = userRepository.getAllUsers()
                Log.d("LoginViewModel", "getAllUsers() → ${listResponse.code()}")
                if (listResponse.isSuccessful) {
                    val found = listResponse.body()?.find { it.email.equals(email, ignoreCase = true) }
                    if (found != null) {
                        val fullName = "${found.name} ${found.lastName ?: ""}".trim()
                        AuthManager.saveUserInfo(getApplication(), found.id ?: 0L, found.email, fullName)
                        AuthManager.saveProfilePhotoURL(getApplication(), found.profilePhotoURL)
                        _userProfile.value = found
                        user = found
                        Log.d("LoginViewModel", "Found via list: id=${found.id}, emailVerified=${found.emailVerified}")
                    }
                }
            }

            if (user != null) {
                if (user.emailVerified) {
                    _loginSuccess.value = true
                } else {
                    _emailNotVerified.value = email
                }
            } else {
                Log.w("LoginViewModel", "Could not retrieve profile, proceeding to Home as fallback")
                AuthManager.saveUserInfo(getApplication(), userIdFromJwt ?: 0L, email, "")
                _loginSuccess.value = true
            }
        } catch (e: Exception) {
            Log.e("LoginViewModel", "fetchUserIdByEmail error", e)
            AuthManager.saveUserInfo(getApplication(), 0L, email, "")
            _loginSuccess.value = true
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Registers a new user in the system.
     *
     * If registration is successful (HTTP 201), credentials are stored in
     * [registerCredentials] so the Activity can trigger an automatic login.
     *
     * Note: In the QA environment POST /api/Users requires admin authentication,
     * so it may return 401 even if the data is correct.
     *
     * @param name        User first name.
     * @param lastName    Last name (optional).
     * @param email       Unique user email.
     * @param password    Chosen password.
     * @param phoneNumber Contact phone number (optional).
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
                    _registerSuccess.value = true
                    // Store credentials so RegisterActivity can trigger the automatic login.
                    _registerCredentials.value = Pair(email, password)
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Invalid data. Please check the information"
                        401 -> "Registration requires admin permissions in this environment. Contact the backend team to create your account."
                        409 -> "Email already registered"
                        500 -> "Server error. Please try again later"
                        else -> "Registration error: ${response.code()}"
                    }
                    _errorMessage.value = errorMsg
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clears the temporarily stored registration credentials.
     * Must be called in [com.example.petradar.RegisterActivity] after reading
     * [registerCredentials] to prevent the observer from triggering again.
     */
    fun clearRegisterCredentials() {
        _registerCredentials.value = null
    }

    // ── Email verification polling ────────────────────────────────────────────

    private val _emailVerified = MutableLiveData<Boolean?>()
    /**
     * Emits `true` when the polling detects that the user's email has been verified.
     * The screen observes this to auto-redirect to the login screen.
     */
    val emailVerified: LiveData<Boolean?> = _emailVerified

    private var pollingJob: Job? = null

    /**
     * Starts polling GET /api/Users/{id} every [intervalMs] milliseconds to check whether
     * the user has verified their email address.
     *
     * Uses getUserById (not getAllUsers) because regular users don't have permission
     * to list all users. The userId and token were saved in [AuthManager] during the
     * login attempt that detected the unverified account.
     *
     * When [UserProfile.emailVerified] becomes `true` the polling stops automatically
     * and [emailVerified] emits `true`.
     *
     * Call [stopPollingEmailVerification] when the screen is no longer visible.
     *
     * @param email       Email of the account to watch (used as fallback for userId lookup).
     * @param intervalMs  Polling interval in milliseconds (default 5 s).
     */
    fun startPollingEmailVerification(email: String, intervalMs: Long = 5_000L) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                try {
                    val userId = AuthManager.getUserId(getApplication())
                    Log.d("LoginViewModel", "Polling: storedUserId=$userId")
                    if (userId != null && userId > 0L) {
                        val response = userRepository.getUserById(userId)
                        Log.d("LoginViewModel", "Polling getUserById($userId) → ${response.code()}, emailVerified=${response.body()?.emailVerified}")
                        if (response.isSuccessful) {
                            val user = response.body()
                            if (user?.emailVerified == true) {
                                _emailVerified.value = true
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LoginViewModel", "Polling error", e)
                }
                delay(intervalMs)
            }
        }
    }

    /** Stops the email-verification polling loop (call from onStop / onDestroy). */
    fun stopPollingEmailVerification() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /** Performs a single immediate check triggered by the "Ya verifiqué mi cuenta" button. */
    fun checkEmailVerifiedNow(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val userId = AuthManager.getUserId(getApplication())
                Log.d("LoginViewModel", "checkEmailVerifiedNow: email=$email, storedUserId=$userId")
                if (userId != null && userId > 0L) {
                    val response = userRepository.getUserById(userId)
                    Log.d("LoginViewModel", "getUserById($userId) → ${response.code()}")
                    if (response.isSuccessful) {
                        val user = response.body()
                        Log.d("LoginViewModel", "emailVerified=${user?.emailVerified}")
                        if (user?.emailVerified == true) {
                            _emailVerified.value = true
                        } else {
                            _errorMessage.value = "Tu cuenta aún no ha sido verificada. Revisa tu email."
                        }
                    } else {
                        _errorMessage.value = "Error al comprobar el estado (${response.code()}). Inténtalo de nuevo."
                    }
                } else {
                    Log.w("LoginViewModel", "No userId stored, falling back to getAllUsers()")
                    val listResponse = userRepository.getAllUsers()
                    Log.d("LoginViewModel", "getAllUsers() → ${listResponse.code()}")
                    if (listResponse.isSuccessful) {
                        val user = listResponse.body()
                            ?.find { it.email.equals(email, ignoreCase = true) }
                        if (user?.emailVerified == true) {
                            _emailVerified.value = true
                        } else {
                            _errorMessage.value = "Tu cuenta aún no ha sido verificada. Revisa tu email."
                        }
                    } else {
                        _errorMessage.value = "Error al comprobar el estado. Inténtalo de nuevo."
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "checkEmailVerifiedNow error", e)
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Resets the emailVerified state. */
    fun clearEmailVerified() {
        _emailVerified.value = null
    }
}
