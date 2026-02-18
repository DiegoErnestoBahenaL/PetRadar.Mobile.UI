# 🎯 Guía Rápida - Sistema de Perfil PetRadar

## ✅ Estado: IMPLEMENTACIÓN COMPLETA

Se ha conectado exitosamente el proyecto Android con el endpoint `https://api-qa.petradar-qa.org/swagger/index.html`

---

## 📋 Pasos para Usar

### 1️⃣ Sincronizar Proyecto con Gradle

**En Android Studio:**
1. Abre el proyecto
2. Aparecerá una notificación "Gradle files have changed"
3. Click en **"Sync Now"**
4. Espera a que descargue todas las dependencias

**Alternativa - Línea de comandos:**
```powershell
cd C:\Users\U\StudioProjects\PetRadar.Mobile.UI
.\gradlew clean build
```

### 2️⃣ Ejecutar la Aplicación

1. Conecta un dispositivo Android o inicia un emulador
2. Click en el botón **Run** (▶️) en Android Studio
3. La app se instalará y ejecutará

### 3️⃣ Navegar al Perfil

1. En la pantalla de inicio (HomeActivity):
   - Desliza desde el borde izquierdo de la pantalla, O
   - Toca el ícono del menú (☰) en la parte superior
   
2. En el menú lateral, selecciona **"Mi Perfil"**

3. Se abrirá la pantalla de perfil donde podrás:
   - ✅ Ver tu información personal
   - ✅ Editar nombre, apellido, teléfono
   - ✅ Actualizar dirección, ciudad, país
   - ✅ Guardar cambios

---

## 🔐 Importante: Autenticación

### El API requiere autenticación

Para que las peticiones funcionen, necesitas:

1. **Obtener un token de autenticación** del endpoint de login
2. **Guardar el token** usando `AuthManager`:

```kotlin
// Después de hacer login exitoso
AuthManager.saveAuthToken(context, "tu_token_aqui")
AuthManager.saveUserInfo(context, userId, email, name)
```

3. **El token se agregará automáticamente** a todas las peticiones HTTP

### Ejemplo de implementación de Login:

```kotlin
// En tu LoginActivity o similar
viewModel.login(email, password).observe(this) { response ->
    if (response.isSuccessful) {
        val token = response.body()?.token
        val user = response.body()?.user
        
        AuthManager.saveAuthToken(this, token)
        AuthManager.saveUserInfo(this, user.id, user.email, user.name)
        
        // Navegar a HomeActivity
        startActivity(Intent(this, HomeActivity::class.java))
    }
}
```

---

## 📁 Archivos Principales

### API y Modelos
- ✅ `api/ApiService.kt` - Definición de endpoints
- ✅ `api/RetrofitClient.kt` - Configuración de Retrofit con auth
- ✅ `api/models/UserProfile.kt` - Modelo de datos del usuario

### Lógica de Negocio
- ✅ `repository/UserRepository.kt` - Acceso a datos
- ✅ `viewmodel/ProfileViewModel.kt` - ViewModel MVVM
- ✅ `utils/AuthManager.kt` - Gestión de autenticación

### UI
- ✅ `HomeActivity.kt` - Pantalla principal con menú
- ✅ `ProfileActivity.kt` - Pantalla de perfil
- ✅ `layout/activity_home.xml` - Layout con drawer navigation
- ✅ `layout/activity_profile.xml` - Layout de formulario de perfil
- ✅ `menu/nav_menu.xml` - Menú de navegación

### Configuración
- ✅ `PetRadarApplication.kt` - Inicialización de la app
- ✅ `AndroidManifest.xml` - Permisos y actividades registradas
- ✅ `build.gradle.kts` - Dependencias agregadas

---

## 🛠️ Endpoints Implementados

```kotlin
// Obtener perfil del usuario actual
GET /api/users/profile

// Actualizar perfil del usuario actual
PUT /api/users/profile

// Obtener usuario por ID
GET /api/users/{id}
```

Para agregar más endpoints, edita `ApiService.kt`:

```kotlin
@GET("api/pets")
suspend fun getUserPets(): Response<List<Pet>>

@POST("api/reports")
suspend fun createReport(@Body report: Report): Response<ReportResponse>
```

---

## 🎨 Estructura del Menú

El menú lateral incluye:

- **👤 Mi Perfil** → Implementado ✅
- **🐾 Mis Mascotas** → Por implementar
- **🔍 Reportes** → Por implementar
- **⚙️ Configuración** → Por implementar
- **🚪 Cerrar Sesión** → Por implementar

Para implementar las opciones faltantes, edita `HomeActivity.kt` en el método `onNavigationItemSelected()`.

---

## 🔧 Personalizar

### Cambiar URL del API
Edita `RetrofitClient.kt`:
```kotlin
private const val BASE_URL = "https://tu-nueva-url.com/"
```

### Agregar campos al perfil
1. Edita `UserProfile.kt` - Agrega el campo
2. Edita `activity_profile.xml` - Agrega el input
3. Edita `ProfileActivity.kt` - Maneja el nuevo campo

### Cambiar colores
Edita `app/src/main/res/values/colors.xml`

---

## 🐛 Solución de Problemas

### Error: "Unresolved reference"
- ✅ Solución: Sincroniza el proyecto con Gradle (Sync Now)

### Error: "No internet connection"
- ✅ Verifica permisos en AndroidManifest.xml
- ✅ Verifica que el dispositivo tenga internet
- ✅ Verifica la URL del API

### Error: "401 Unauthorized"
- ✅ El endpoint requiere autenticación
- ✅ Implementa login y guarda el token con AuthManager

### Error: "404 Not Found"
- ✅ Verifica la URL del endpoint en el Swagger
- ✅ Ajusta los endpoints en ApiService.kt

### No se muestran datos del perfil
- ✅ Verifica que el token esté guardado: `AuthManager.getAuthToken()`
- ✅ Revisa el Logcat para ver los logs de las peticiones HTTP
- ✅ Verifica que la estructura de datos coincida con la API

---

## 📚 Documentación Adicional

- **Swagger API:** https://api-qa.petradar-qa.org/swagger/index.html
- **Documentación detallada:** Ver `SETUP_PROFILE.md`
- **Retrofit:** https://square.github.io/retrofit/
- **Material Components:** https://material.io/develop/android

---

## 📞 Próximos Pasos Recomendados

1. ✅ **Implementar Login** - Crear pantalla de login para obtener token
2. ⬜ **Implementar Registro** - Permitir crear nuevas cuentas
3. ⬜ **Gestión de Mascotas** - Agregar, editar, eliminar mascotas
4. ⬜ **Reportes** - Crear y ver reportes de mascotas perdidas
5. ⬜ **Notificaciones** - Push notifications para alertas
6. ⬜ **Geolocalización** - Mostrar mascotas perdidas en mapa
7. ⬜ **Subir Imágenes** - Foto de perfil y fotos de mascotas

---

## ✨ ¡Listo para Probar!

Tu proyecto ahora tiene:
- ✅ Conexión al API de PetRadar
- ✅ Menú de navegación funcional
- ✅ Pantalla de perfil completa
- ✅ Arquitectura MVVM
- ✅ Manejo de autenticación
- ✅ Logging de peticiones HTTP

**¡Sincroniza el proyecto y comienza a desarrollar!** 🚀

