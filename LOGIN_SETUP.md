# 🚀 Sistema de Login Implementado - PetRadar

## ✅ IMPLEMENTACIÓN COMPLETA

Se ha implementado un sistema completo de autenticación (Login y Registro) para PetRadar.

---

## 📦 Nuevos Archivos Creados

### Modelos de Datos
- ✅ `api/models/Auth.kt` - LoginRequest, LoginResponse, RegisterRequest, ApiError

### Repositorios
- ✅ `repository/AuthRepository.kt` - Manejo de autenticación

### ViewModels
- ✅ `viewmodel/LoginViewModel.kt` - Lógica de login y registro

### Activities
- ✅ `LoginActivity.kt` - Pantalla de inicio de sesión
- ✅ `RegisterActivity.kt` - Pantalla de registro

### Layouts
- ✅ `activity_login.xml` - Diseño Material Design del login
- ✅ `activity_register_new.xml` - Diseño Material Design del registro

---

## 🎯 Funcionalidades Implementadas

### 1. Login (LoginActivity)
- ✅ Formulario con email y contraseña
- ✅ Validación de campos (email válido, contraseña mínimo 6 caracteres)
- ✅ Mensajes de error específicos por código HTTP
- ✅ Indicador de carga (progress bar)
- ✅ Botón para ir a registro
- ✅ Diseño Material Design con logo
- ✅ Toggle para mostrar/ocultar contraseña
- ✅ Persistencia de sesión con AuthManager
- ✅ Redirección automática a Home si ya está autenticado

### 2. Registro (RegisterActivity)
- ✅ Formulario completo: nombre, apellido, email, teléfono, contraseña
- ✅ Confirmación de contraseña
- ✅ Validación de todos los campos
- ✅ Verificación de que las contraseñas coincidan
- ✅ Teléfono opcional
- ✅ Toolbar con botón de volver
- ✅ Link para volver al login
- ✅ Registro automático y redirección a Home

### 3. Logout
- ✅ Implementado en el menú lateral de HomeActivity
- ✅ Limpia los datos de sesión
- ✅ Redirecciona al login

### 4. Flujo Completo
- ✅ App inicia en LoginActivity
- ✅ Si ya está autenticado → va directo a Home
- ✅ Login exitoso → guarda token → va a Home
- ✅ Registro exitoso → guarda token → va a Home
- ✅ Logout → limpia sesión → va a Login
- ✅ Datos del usuario se muestran en el header del menú

---

## 🔧 Pasos para Probar

### 1. Sincronizar Proyecto

**En Android Studio:**
```
File → Sync Project with Gradle Files
```

Espera a que se descarguen todas las dependencias.

### 2. Ejecutar la App

1. Conecta un dispositivo Android o inicia un emulador
2. Click en Run (▶️)
3. La app abrirá en **LoginActivity**

### 3. Flujo de Prueba

#### Opción A: Crear Cuenta Nueva
1. Click en "Crear Cuenta Nueva"
2. Llenar el formulario de registro:
   - Nombre: `Juan`
   - Apellido: `Pérez`
   - Email: `juan.perez@test.com`
   - Teléfono: `+1234567890` (opcional)
   - Contraseña: `123456`
   - Confirmar: `123456`
3. Click en "Registrarse"
4. Si el registro es exitoso → se guardará el token → irá a Home

#### Opción B: Iniciar Sesión
1. Ingresar email y contraseña de un usuario existente
2. Click en "Iniciar Sesión"
3. Si el login es exitoso → se guardará el token → irá a Home

#### Ver Perfil
1. En Home, abrir el menú lateral (☰)
2. Verás tu nombre y email en el header
3. Click en "Mi Perfil"
4. Podrás ver y editar tu información

#### Cerrar Sesión
1. Abrir el menú lateral
2. Click en "Cerrar Sesión"
3. Se limpiarán los datos y volverás al Login

---

## 📡 Endpoints Utilizados

```kotlin
// Login
POST /api/auth/login
Body: { "email": "...", "password": "..." }
Response: { "token": "...", "user": {...} }

// Registro
POST /api/auth/register
Body: { 
  "firstName": "...", 
  "lastName": "...", 
  "email": "...", 
  "password": "...",
  "phoneNumber": "..." 
}
Response: { "token": "...", "user": {...} }

// Logout
POST /api/auth/logout
```

---

## 🔐 Sistema de Autenticación

### AuthManager
Almacena en SharedPreferences:
- `auth_token` - Token de autenticación
- `user_id` - ID del usuario
- `user_email` - Email del usuario
- `user_name` - Nombre completo del usuario

### Uso del Token
El token se agrega automáticamente a todas las peticiones HTTP mediante un interceptor en `RetrofitClient`:

```kotlin
.addInterceptor { chain ->
    val token = AuthManager.getAuthToken(context)
    val request = chain.request().newBuilder()
        .addHeader("Authorization", "Bearer $token")
        .build()
    chain.proceed(request)
}
```

---

## ⚠️ Notas Importantes

### 1. Endpoints Reales
Los endpoints (`/api/auth/login`, `/api/auth/register`) deben existir en el API de PetRadar. Verifica en el Swagger:
```
https://api-qa.petradar-qa.org/swagger/index.html
```

Si los endpoints son diferentes, actualiza `ApiService.kt`:
```kotlin
@POST("ruta/correcta/login")
suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
```

### 2. Estructura de Datos
Verifica que los modelos (`LoginRequest`, `LoginResponse`, `RegisterRequest`) coincidan con lo que espera el API. Ajusta los campos en `api/models/Auth.kt` si es necesario.

### 3. Códigos de Error
El LoginViewModel maneja estos códigos de error:
- `400` - Datos inválidos
- `401` - Credenciales incorrectas
- `404` - Usuario no encontrado
- `409` - Email ya registrado
- `500` - Error del servidor

### 4. Testing sin Backend
Si el backend no está listo, puedes usar un mock server o datos hardcodeados temporalmente:

```kotlin
// En LoginViewModel, comentar la petición real y usar:
viewModelScope.launch {
    _isLoading.value = true
    delay(1000) // Simular petición
    
    // Mock de respuesta exitosa
    val mockUser = UserProfile(
        id = "1",
        firstName = "Test",
        lastName = "User",
        email = email,
        phoneNumber = null,
        address = null,
        city = null,
        country = null,
        profileImageUrl = null
    )
    val mockResponse = LoginResponse(
        token = "mock_token_12345",
        refreshToken = null,
        user = mockUser,
        expiresIn = null
    )
    _loginResult.value = mockResponse
    _isLoading.value = false
}
```

---

## 🎨 Personalización

### Cambiar Colores
Edita `app/src/main/res/values/colors.xml`

### Cambiar Logo
Reemplaza `app/src/main/res/drawable/petradar_logo.png`

### Agregar Validaciones
Edita las funciones `attemptLogin()` y `attemptRegister()` en las Activities

### Modificar Campos del Registro
1. Edita `activity_register_new.xml` - Agrega el campo visual
2. Edita `RegisterRequest.kt` - Agrega el campo al modelo
3. Edita `RegisterActivity.kt` - Captura el valor del campo

---

## 🐛 Solución de Problemas

### "Unresolved reference" en el código
✅ Solución: Sync Project with Gradle Files

### "No internet connection"
✅ Verifica que el dispositivo/emulador tenga internet
✅ Verifica que la URL del API sea correcta en `RetrofitClient.kt`

### "401 Unauthorized" al ver perfil
✅ El token no se guardó correctamente
✅ Verifica que el login/registro haya sido exitoso primero

### No se ven los datos del usuario en el header
✅ Verifica que `AuthManager.saveUserInfo()` se haya llamado
✅ Revisa el código en `LoginActivity` y `RegisterActivity`

### App crash al abrir
✅ Revisa el Logcat en Android Studio
✅ Verifica que todas las dependencias estén sincronizadas

---

## 📊 Estado del TODO.md

### Completado Ahora:
- [x] Crear layout de LoginActivity
- [x] Implementar LoginActivity
- [x] Crear LoginViewModel
- [x] Agregar endpoint de login en ApiService
- [x] Agregar endpoint de registro
- [x] Implementar flujo de logout
- [x] Agregar validación de email y contraseña

### Pendiente:
- [ ] Implementar "Recordar sesión"
- [ ] Agregar "Olvidé mi contraseña"

---

## ✨ ¡Listo para Probar!

Tu app ahora tiene un sistema completo de autenticación:

1. ✅ Pantalla de Login profesional
2. ✅ Registro de nuevos usuarios
3. ✅ Validaciones completas
4. ✅ Persistencia de sesión
5. ✅ Logout funcional
6. ✅ Integración con el API de PetRadar

**Siguiente paso:**
1. Sync Project with Gradle Files
2. Run (▶️)
3. ¡Probar el flujo completo!

---

**Creado:** 2026-02-17
**Versión:** 1.0
**Endpoint API:** https://api-qa.petradar-qa.org/

