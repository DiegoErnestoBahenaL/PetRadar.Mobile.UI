# 💡 Ejemplos de Código - PetRadar

## 📖 Índice
1. [Usar el Perfil en tu Activity](#usar-perfil)
2. [Implementar Login](#implementar-login)
3. [Agregar Nuevos Endpoints](#agregar-endpoints)
4. [Cargar Imagen de Perfil](#cargar-imagen)

---

## 🔹 Usar el Perfil en tu Activity {#usar-perfil}

### Cargar y mostrar información del perfil:

```kotlin
class MiActivity : AppCompatActivity() {
    
    private lateinit var viewModel: ProfileViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mi)
        
        // Inicializar ViewModel
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        
        // Observar cambios en el perfil
        viewModel.userProfile.observe(this) { profile ->
            profile?.let {
                // Usar los datos del perfil
                tvNombre.text = "${it.firstName} ${it.lastName}"
                tvEmail.text = it.email
                tvTelefono.text = it.phoneNumber ?: "No especificado"
            }
        }
        
        // Observar estado de carga
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Observar errores
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
        
        // Cargar el perfil
        viewModel.loadUserProfile()
    }
}
```

---

## 🔹 Implementar Login {#implementar-login}

### 1. Agregar modelo de Login en `api/models/`:

```kotlin
// LoginRequest.kt
data class LoginRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String
)

// LoginResponse.kt
data class LoginResponse(
    @SerializedName("token")
    val token: String,
    
    @SerializedName("user")
    val user: UserProfile,
    
    @SerializedName("expiresIn")
    val expiresIn: Long?
)
```

### 2. Agregar endpoint en `ApiService.kt`:

```kotlin
@POST("api/auth/login")
suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

@POST("api/auth/register")
suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

@POST("api/auth/logout")
suspend fun logout(): Response<Unit>
```

### 3. Crear Repository:

```kotlin
// repository/AuthRepository.kt
class AuthRepository {
    
    private val apiService = RetrofitClient.apiService
    
    suspend fun login(email: String, password: String): Response<LoginResponse> {
        val request = LoginRequest(email, password)
        return apiService.login(request)
    }
    
    suspend fun logout(): Response<Unit> {
        return apiService.logout()
    }
}
```

### 4. Crear ViewModel:

```kotlin
// viewmodel/LoginViewModel.kt
class LoginViewModel : ViewModel() {
    
    private val repository = AuthRepository()
    
    private val _loginResult = MutableLiveData<LoginResponse?>()
    val loginResult: LiveData<LoginResponse?> = _loginResult
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = repository.login(email, password)
                if (response.isSuccessful) {
                    _loginResult.value = response.body()
                } else {
                    _errorMessage.value = "Error: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```

### 5. Usar en LoginActivity:

```kotlin
class LoginActivity : AppCompatActivity() {
    
    private lateinit var viewModel: LoginViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        
        // Observar resultado del login
        viewModel.loginResult.observe(this) { response ->
            response?.let {
                // Guardar token y datos del usuario
                AuthManager.saveAuthToken(this, it.token)
                AuthManager.saveUserInfo(
                    this,
                    it.user.id ?: "",
                    it.user.email,
                    "${it.user.firstName} ${it.user.lastName}"
                )
                
                // Navegar a HomeActivity
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        
        // Observar estado de carga
        viewModel.isLoading.observe(this) { isLoading ->
            btnLogin.isEnabled = !isLoading
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Observar errores
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
        
        // Listener del botón de login
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            
            if (validateInputs(email, password)) {
                viewModel.login(email, password)
            }
        }
    }
    
    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            etEmail.error = "El email es requerido"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Email inválido"
            return false
        }
        if (password.isEmpty()) {
            etPassword.error = "La contraseña es requerida"
            return false
        }
        return true
    }
}
```

---

## 🔹 Agregar Nuevos Endpoints {#agregar-endpoints}

### Ejemplo: Gestión de Mascotas

### 1. Crear modelos:

```kotlin
// api/models/Pet.kt
data class Pet(
    @SerializedName("id")
    val id: String?,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("species")
    val species: String, // "dog", "cat", etc.
    
    @SerializedName("breed")
    val breed: String?,
    
    @SerializedName("age")
    val age: Int?,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("imageUrl")
    val imageUrl: String?,
    
    @SerializedName("isLost")
    val isLost: Boolean = false,
    
    @SerializedName("ownerId")
    val ownerId: String?
)

data class CreatePetRequest(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("species")
    val species: String,
    
    @SerializedName("breed")
    val breed: String?,
    
    @SerializedName("age")
    val age: Int?,
    
    @SerializedName("description")
    val description: String?
)
```

### 2. Agregar endpoints:

```kotlin
// ApiService.kt
@GET("api/pets")
suspend fun getUserPets(): Response<List<Pet>>

@GET("api/pets/{id}")
suspend fun getPetById(@Path("id") petId: String): Response<Pet>

@POST("api/pets")
suspend fun createPet(@Body request: CreatePetRequest): Response<Pet>

@PUT("api/pets/{id}")
suspend fun updatePet(
    @Path("id") petId: String,
    @Body request: CreatePetRequest
): Response<Pet>

@DELETE("api/pets/{id}")
suspend fun deletePet(@Path("id") petId: String): Response<Unit>

@POST("api/pets/{id}/report-lost")
suspend fun reportPetLost(@Path("id") petId: String): Response<Pet>
```

### 3. Crear Repository:

```kotlin
// repository/PetRepository.kt
class PetRepository {
    
    private val apiService = RetrofitClient.apiService
    
    suspend fun getUserPets(): Response<List<Pet>> {
        return apiService.getUserPets()
    }
    
    suspend fun createPet(request: CreatePetRequest): Response<Pet> {
        return apiService.createPet(request)
    }
    
    suspend fun updatePet(petId: String, request: CreatePetRequest): Response<Pet> {
        return apiService.updatePet(petId, request)
    }
    
    suspend fun deletePet(petId: String): Response<Unit> {
        return apiService.deletePet(petId)
    }
    
    suspend fun reportPetLost(petId: String): Response<Pet> {
        return apiService.reportPetLost(petId)
    }
}
```

### 4. Crear ViewModel:

```kotlin
// viewmodel/PetsViewModel.kt
class PetsViewModel : ViewModel() {
    
    private val repository = PetRepository()
    
    private val _pets = MutableLiveData<List<Pet>>()
    val pets: LiveData<List<Pet>> = _pets
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    fun loadPets() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getUserPets()
                if (response.isSuccessful) {
                    _pets.value = response.body() ?: emptyList()
                } else {
                    _errorMessage.value = "Error al cargar mascotas"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun createPet(name: String, species: String, breed: String?, age: Int?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = CreatePetRequest(name, species, breed, age, null)
                val response = repository.createPet(request)
                if (response.isSuccessful) {
                    loadPets() // Recargar la lista
                } else {
                    _errorMessage.value = "Error al crear mascota"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```

---

## 🔹 Cargar Imagen de Perfil {#cargar-imagen}

### 1. Agregar dependencia Glide en `build.gradle.kts`:

```kotlin
implementation("com.github.bumptech.glide:glide:4.16.0")
```

### 2. Cargar imagen en ImageView:

```kotlin
import com.bumptech.glide.Glide

// En tu Activity o Fragment
viewModel.userProfile.observe(this) { profile ->
    profile?.let {
        // Cargar imagen de perfil
        Glide.with(this)
            .load(it.profileImageUrl)
            .placeholder(R.drawable.petradar_logo) // Imagen mientras carga
            .error(R.drawable.petradar_logo) // Imagen si hay error
            .circleCrop() // Hacer la imagen circular
            .into(ivProfileImage)
    }
}
```

### 3. Para subir una imagen al servidor:

```kotlin
// Modelo para subir imagen
data class UploadImageResponse(
    @SerializedName("imageUrl")
    val imageUrl: String
)

// Endpoint en ApiService.kt
@Multipart
@POST("api/users/profile/image")
suspend fun uploadProfileImage(
    @Part image: MultipartBody.Part
): Response<UploadImageResponse>

// Función para subir imagen
fun uploadImage(context: Context, imageUri: Uri) {
    viewModelScope.launch {
        try {
            val file = getFileFromUri(context, imageUri)
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData(
                "image", 
                file.name, 
                requestFile
            )
            
            val response = repository.uploadProfileImage(imagePart)
            if (response.isSuccessful) {
                val imageUrl = response.body()?.imageUrl
                // Actualizar UI con la nueva URL
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error al subir imagen: ${e.message}"
        }
    }
}

private fun getFileFromUri(context: Context, uri: Uri): File {
    val inputStream = context.contentResolver.openInputStream(uri)
    val file = File(context.cacheDir, "temp_image.jpg")
    inputStream?.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return file
}
```

---

## 🔹 Verificar si está autenticado

### En MainActivity o SplashActivity:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    if (AuthManager.isAuthenticated(this)) {
        // Usuario ya está autenticado, ir a Home
        startActivity(Intent(this, HomeActivity::class.java))
    } else {
        // No está autenticado, ir a Login
        startActivity(Intent(this, LoginActivity::class.java))
    }
    finish()
}
```

---

## 🔹 Cerrar Sesión

### En HomeActivity o SettingsActivity:

```kotlin
fun logout() {
    // Llamar endpoint de logout (opcional)
    viewModelScope.launch {
        try {
            repository.logout()
        } catch (e: Exception) {
            // Ignorar errores
        }
        
        // Limpiar datos locales
        AuthManager.logout(this@HomeActivity)
        
        // Navegar a Login
        val intent = Intent(this@HomeActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
```

---

## 🔹 Manejo de Errores HTTP

### Crear clase para manejar errores comunes:

```kotlin
// utils/ApiErrorHandler.kt
object ApiErrorHandler {
    
    fun getErrorMessage(code: Int): String {
        return when (code) {
            400 -> "Solicitud inválida"
            401 -> "No autorizado. Por favor inicia sesión"
            403 -> "Acceso denegado"
            404 -> "Recurso no encontrado"
            500 -> "Error del servidor"
            503 -> "Servicio no disponible"
            else -> "Error desconocido: $code"
        }
    }
    
    fun <T> handleResponse(
        response: Response<T>,
        onSuccess: (T) -> Unit,
        onError: (String) -> Unit
    ) {
        if (response.isSuccessful) {
            response.body()?.let {
                onSuccess(it)
            } ?: onError("Respuesta vacía")
        } else {
            onError(getErrorMessage(response.code()))
        }
    }
}

// Uso:
val response = repository.getUserProfile()
ApiErrorHandler.handleResponse(
    response,
    onSuccess = { profile ->
        _userProfile.value = profile
    },
    onError = { error ->
        _errorMessage.value = error
    }
)
```

---

## 📚 Recursos Adicionales

- **Retrofit:** https://square.github.io/retrofit/
- **Coroutines:** https://developer.android.com/kotlin/coroutines
- **LiveData:** https://developer.android.com/topic/libraries/architecture/livedata
- **ViewModel:** https://developer.android.com/topic/libraries/architecture/viewmodel
- **Glide:** https://github.com/bumptech/glide

---

¡Estos ejemplos te ayudarán a extender la funcionalidad de tu app! 🚀

