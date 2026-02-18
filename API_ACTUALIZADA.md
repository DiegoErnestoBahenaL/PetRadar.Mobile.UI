# ✅ ENDPOINTS ACTUALIZADOS - PetRadar API Real

## 🎯 Todos los Archivos Actualizados con la API Real

Se han actualizado **TODOS los archivos** para que coincidan exactamente con la API real de PetRadar según el Swagger JSON proporcionado.

---

## 📡 Endpoints Reales Implementados

### Autenticación
```kotlin
POST /api/gate/Login                    // Login
POST /api/gate/Login/refresh            // Refresh token
```

### Usuarios
```kotlin
POST   /api/Users                       // Crear usuario (Registro)
GET    /api/Users                       // Obtener todos los usuarios
GET    /api/Users/{id}                  // Obtener usuario por ID
PUT    /api/Users/{id}                  // Actualizar usuario
DELETE /api/Users/{id}                  // Eliminar usuario
```

### Mascotas
```kotlin
GET    /api/UserPets                    // Obtener mascotas del usuario actual
GET    /api/UserPets/user/{userId}      // Obtener mascotas por userId
GET    /api/UserPets/{id}               // Obtener mascota por ID
POST   /api/UserPets                    // Crear mascota
PUT    /api/UserPets/{id}               // Actualizar mascota
DELETE /api/UserPets/{id}               // Eliminar mascota
```

---

## 🔄 Cambios Principales Implementados

### 1. Modelos de Datos Actualizados

**LoginRequest** (API usa `username` no `email`):
```kotlin
data class LoginRequest(
    @SerializedName("username")  // ← email se envía como username
    val username: String,
    @SerializedName("password")
    val password: String
)
```

**LoginResponse** (UserTokenViewModel):
```kotlin
data class LoginResponse(
    @SerializedName("token")
    val token: String?,
    @SerializedName("tokenValidTo")
    val tokenValidTo: String?,
    @SerializedName("refreshToken")
    val refreshToken: String?,
    @SerializedName("refreshTokenExpiryTime")
    val refreshTokenExpiryTime: String?
)
// ⚠️ NO devuelve objeto user
```

**RegisterRequest** (UserCreateModel):
```kotlin
data class RegisterRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("name")        // ← "name" no "firstName"
    val name: String,
    @SerializedName("lastName")
    val lastName: String? = null,
    @SerializedName("phoneNumber")
    val phoneNumber: String? = null,
    @SerializedName("role")
    val role: String = "User"
)
```

**UserProfile** (UserViewModel):
```kotlin
data class UserProfile(
    @SerializedName("id")
    val id: Long? = null,          // ← Long no String
    @SerializedName("email")
    val email: String = "",
    @SerializedName("name")        // ← "name" no "firstName"
    val name: String = "",
    @SerializedName("lastName")
    val lastName: String? = null,
    @SerializedName("phoneNumber")
    val phoneNumber: String? = null,
    @SerializedName("profilePhotoURL")  // ← "profilePhotoURL" no "profileImageUrl"
    val profilePhotoURL: String? = null,
    @SerializedName("role")
    val role: String? = null,
    @SerializedName("organizationName")
    val organizationName: String? = null,
    @SerializedName("organizationAddress")
    val organizationAddress: String? = null,
    @SerializedName("organizationPhone")
    val organizationPhone: String? = null
)
// ⚠️ NO tiene campos: address, city, country, createdAt, updatedAt
```

### 2. Flujo de Autenticación Actualizado

#### Login:
1. Usuario ingresa email y contraseña
2. Se llama `POST /api/gate/Login` con `username` (email) y `password`
3. API devuelve solo token (NO devuelve datos de usuario)
4. Se guarda token y email (userId se guarda como 0 temporalmente)

#### Registro:
1. Usuario llena formulario
2. Se llama `POST /api/Users` para crear usuario
3. API devuelve `201 Created` (NO devuelve token)
4. Se llama automáticamente `POST /api/gate/Login` con las mismas credenciales
5. Se guarda token del login
6. Usuario entra a la app

### 3. Archivos Actualizados

#### API
- ✅ `ApiService.kt` - Endpoints correctos
- ✅ `api/models/Auth.kt` - LoginRequest, LoginResponse, RegisterRequest actualizados
- ✅ `api/models/UserProfile.kt` - UserProfile y UpdateProfileRequest actualizados

#### Repositories
- ✅ `AuthRepository.kt` - login() y register() actualizados
- ✅ `UserRepository.kt` - getUserById() y updateUser() con Long userId

#### ViewModels
- ✅ `LoginViewModel.kt` - Nuevo flujo: registro → login automático
- ✅ `ProfileViewModel.kt` - loadUserProfile(userId) y updateProfile(userId, ...)

#### Utils
- ✅ `AuthManager.kt` - userId como Long, saveAuthToken() con refreshToken

#### Activities
- ✅ `LoginActivity.kt` - Usa username, guarda email, userId=0
- ✅ `RegisterActivity.kt` - Flujo: registro → login automático → home
- ✅ `ProfileActivity.kt` - Carga perfil con userId, usa name/lastName

---

## 🔑 Diferencias Clave vs Implementación Anterior

| Aspecto | Anterior | API Real |
|---------|----------|----------|
| **Login endpoint** | `/api/auth/login` | `/api/gate/Login` |
| **Campo de login** | `email` | `username` |
| **Login response** | Incluía `user` object | Solo `token` y `refreshToken` |
| **Register endpoint** | `/api/auth/register` | `/api/Users` POST |
| **Register response** | Devolvía token | Solo `201 Created`, sin token |
| **Get profile endpoint** | `/api/users/profile` | `/api/Users/{id}` |
| **Update profile** | `/api/users/profile` PUT | `/api/Users/{id}` PUT |
| **User ID type** | `String` | `Long` (Int64) |
| **Nombre usuario** | `firstName` | `name` |
| **Campos extras** | `address`, `city`, `country` | `organizationName`, `organizationAddress`, etc. |

---

## 🚀 Flujo Completo Actualizado

### 1. Registro + Login
```
1. Usuario → Formulario de registro
2. App → POST /api/Users (crear usuario)
3. API → 201 Created ✅
4. App → POST /api/gate/Login (login automático)
5. API → {token, refreshToken} ✅
6. App → Guardar token y email
7. App → Navegar a Home
```

### 2. Ver Perfil
```
1. Usuario → Click "Mi Perfil"
2. App → Obtener userId del AuthManager
3. App → GET /api/Users/{userId}
4. API → {id, name, lastName, email, phoneNumber, ...}
5. App → Mostrar datos en formulario
```

### 3. Actualizar Perfil
```
1. Usuario → Editar campos y guardar
2. App → Obtener userId del AuthManager
3. App → PUT /api/Users/{userId} con {name, lastName, phoneNumber, ...}
4. API → 204 No Content ✅
5. App → GET /api/Users/{userId} (recargar perfil)
6. App → Mostrar datos actualizados
```

---

## ⚠️ Limitaciones Conocidas

### 1. UserId Temporal
- El login no devuelve datos de usuario
- Se guarda userId=0 temporalmente
- **Solución:** Hacer GET /api/Users con email para obtener el ID real
- O implementar endpoint `/api/Users/current` si existe

### 2. Campos del Formulario
- Los campos `address`, `city`, `country` no existen en la API
- Se dejaron en el layout pero no se usan
- **Solución:** Ocultar estos campos o usar `organizationAddress`

### 3. Foto de Perfil
- El campo es `profilePhotoURL` (solo URL, no subida)
- No hay endpoint para subir imagen implementado
- **Solución:** Implementar upload de imagen cuando el API lo soporte

---

## 📋 Modelos de Mascotas Agregados

Se agregaron los modelos completos para trabajar con mascotas:

```kotlin
// UserPetViewModel - Representa una mascota
data class UserPetViewModel(
    val id: Long,
    val userId: Long,
    val name: String?,
    val species: String?,        // "Dog" or "Cat"
    val breed: String?,
    val color: String?,
    val sex: String?,            // "Male", "Female", "Unknown"
    val size: String?,           // "Small", "Medium", "Large"
    val birthDate: String?,
    val approximateAge: Double?,
    val weight: Double?,
    val description: String?,
    val photoURL: String?,
    val additionalPhotosURL: String?,
    val isNeutered: Boolean?,
    val allergies: String?,
    val medicalNotes: String?
)

// UserPetCreateModel - Para crear una mascota
// UserPetUpdateModel - Para actualizar una mascota
```

**Endpoints de mascotas listos para usar:**
- `GET /api/UserPets` - Obtener mascotas del usuario
- `POST /api/UserPets` - Crear mascota
- `PUT /api/UserPets/{id}` - Actualizar mascota
- `DELETE /api/UserPets/{id}` - Eliminar mascota

---

## ✅ Estado Final

### ✅ Completado
- Sistema de Login actualizado con endpoints reales
- Sistema de Registro con login automático
- Sistema de Perfil con userId
- Modelos de datos actualizados
- AuthManager con userId Long y refreshToken
- Endpoints de mascotas definidos y listos

### 📝 Notas Importantes
1. **Probar con datos reales:** Ahora que los endpoints están correctos, prueba con email/password reales
2. **userId:** Después del login, considera hacer GET /api/Users para obtener el userId real
3. **Campos del formulario:** Puedes ocultar address, city, country del layout
4. **RefreshToken:** El sistema ya soporta refresh tokens, solo falta implementar la lógica de refresh automático

---

## 🔧 Próximos Pasos Recomendados

1. **Sync Gradle** en Android Studio
2. **Probar Login** con email y password reales
3. **Probar Registro** y verificar que funcione el login automático
4. **Obtener userId real** después del login
5. **Implementar gestión de mascotas** usando los modelos ya definidos
6. **Implementar refresh token** automático cuando expire

---

**¡Todos los archivos están actualizados y listos para probar con la API real!** 🚀

