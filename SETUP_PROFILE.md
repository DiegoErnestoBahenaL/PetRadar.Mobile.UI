# Configuración del Sistema de Perfil - PetRadar

## ✅ Implementación Completada

Se ha implementado exitosamente un sistema de perfil de usuario conectado al endpoint de PetRadar QA.

### 🎯 Características Implementadas

1. **Menú de Navegación en HomeActivity**
   - Drawer navigation con opciones de menú
   - Acceso rápido al perfil del usuario
   - Header personalizado con información del usuario

2. **Pantalla de Perfil (ProfileActivity)**
   - Ver información del perfil del usuario
   - Editar datos personales (nombre, apellido, teléfono)
   - Editar dirección (dirección, ciudad, país)
   - Validación de campos requeridos
   - Indicadores de carga y mensajes de error

3. **Integración con API**
   - Retrofit configurado para conectar con `https://api-qa.petradar-qa.org/`
   - Endpoints de perfil implementados:
     - `GET /api/users/profile` - Obtener perfil del usuario
     - `PUT /api/users/profile` - Actualizar perfil del usuario
   - Manejo de errores y respuestas HTTP
   - Logging de peticiones para debugging

4. **Arquitectura MVVM**
   - `ProfileViewModel` para lógica de negocio
   - `UserRepository` para acceso a datos
   - LiveData para observación de cambios
   - Coroutines para operaciones asíncronas

### 📁 Archivos Creados

**API y Networking:**
- `app/src/main/java/com/example/petradar/api/ApiService.kt`
- `app/src/main/java/com/example/petradar/api/RetrofitClient.kt`
- `app/src/main/java/com/example/petradar/api/models/UserProfile.kt`

**Repository:**
- `app/src/main/java/com/example/petradar/repository/UserRepository.kt`

**ViewModel:**
- `app/src/main/java/com/example/petradar/viewmodel/ProfileViewModel.kt`

**Activities:**
- `app/src/main/java/com/example/petradar/ProfileActivity.kt` (nuevo)
- `app/src/main/java/com/example/petradar/HomeActivity.kt` (actualizado)

**Layouts:**
- `app/src/main/res/layout/activity_home.xml` (actualizado con drawer)
- `app/src/main/res/layout/activity_profile.xml` (nuevo)
- `app/src/main/res/layout/nav_header.xml` (nuevo)
- `app/src/main/res/menu/nav_menu.xml` (nuevo)

### 📦 Dependencias Agregadas

```kotlin
// Retrofit para llamadas HTTP
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// OkHttp para logging
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// ViewModel y LiveData
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

// Material Components
implementation("androidx.appcompat:appcompat:1.6.1")
implementation("com.google.android.material:material:1.11.0")
implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
implementation("androidx.drawerlayout:drawerlayout:1.2.0")
```

### 🔧 Configuración Necesaria

#### 1. Sincronizar el Proyecto

**Opción A - Usando Android Studio:**
1. Abre el proyecto en Android Studio
2. Click en "File" → "Sync Project with Gradle Files"
3. Espera a que se descarguen las dependencias

**Opción B - Usando línea de comandos:**
```powershell
cd C:\Users\U\StudioProjects\PetRadar.Mobile.UI
.\gradlew clean build
```

Si el wrapper de Gradle no funciona, reinstálalo:
```powershell
gradle wrapper
```

#### 2. Permisos Agregados

Se agregaron los siguientes permisos en `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 🚀 Cómo Usar

1. **Abrir el Menú:**
   - En `HomeActivity`, desliza desde el borde izquierdo o toca el ícono del menú (☰)

2. **Acceder al Perfil:**
   - En el menú lateral, selecciona "Mi Perfil"
   - Se abrirá `ProfileActivity`

3. **Ver Perfil:**
   - La app cargará automáticamente el perfil del usuario desde la API
   - Verás los campos prellenados con la información actual

4. **Editar Perfil:**
   - Modifica los campos que desees
   - Presiona "Guardar Cambios"
   - La app enviará los datos actualizados a la API

### 🔐 Autenticación

**IMPORTANTE:** Los endpoints de la API probablemente requieren autenticación. Deberás:

1. **Agregar Token de Autenticación:**
   Edita `RetrofitClient.kt` y descomenta/agrega el header de autorización:
   ```kotlin
   .addInterceptor { chain ->
       val token = "TU_TOKEN_AQUI" // Obtener de SharedPreferences o similar
       val request = chain.request().newBuilder()
           .addHeader("Authorization", "Bearer $token")
           .build()
       chain.proceed(request)
   }
   ```

2. **Implementar Login:**
   - Necesitarás implementar un flujo de login
   - Guardar el token en SharedPreferences
   - Inyectar el token en las peticiones

### 📊 Modelos de Datos

El modelo `UserProfile` incluye los siguientes campos:
- `id`: String (ID del usuario)
- `firstName`: String (Nombre)
- `lastName`: String (Apellido)
- `email`: String (Email - solo lectura)
- `phoneNumber`: String? (Teléfono - opcional)
- `address`: String? (Dirección - opcional)
- `city`: String? (Ciudad - opcional)
- `country`: String? (País - opcional)
- `profileImageUrl`: String? (URL de imagen - opcional)
- `createdAt`: String? (Fecha de creación)
- `updatedAt`: String? (Fecha de actualización)

### 🎨 Personalización

**Colores y Tema:**
- Edita `app/src/main/res/values/colors.xml` para cambiar colores
- Edita `app/src/main/res/values/themes.xml` para cambiar el tema

**Menú:**
- Edita `app/src/main/res/menu/nav_menu.xml` para agregar más opciones

**Campos del Perfil:**
- Edita `activity_profile.xml` para agregar más campos
- Actualiza `UserProfile.kt` y `UpdateProfileRequest.kt` con los nuevos campos
- Actualiza `ProfileActivity.kt` para manejar los nuevos campos

### 🐛 Debugging

Para ver los logs de las peticiones HTTP:
1. Abre el Logcat en Android Studio
2. Filtra por "OkHttp" para ver las peticiones y respuestas
3. El `HttpLoggingInterceptor` está configurado en modo `BODY` para logging completo

### 📝 Notas Importantes

1. **Endpoints Reales:** Verifica en el Swagger (`https://api-qa.petradar-qa.org/swagger/index.html`) los endpoints exactos y la estructura de datos requerida.

2. **Modelos de Datos:** Puede que necesites ajustar los modelos según la respuesta real de la API.

3. **Autenticación:** La mayoría de endpoints requieren token de autenticación. Implementa el flujo de login primero.

4. **Manejo de Errores:** Se implementó manejo básico de errores. Considera agregar:
   - Reintentos automáticos
   - Manejo de errores de red específicos
   - Mensajes de error más amigables

5. **Validación:** Se implementó validación básica. Considera agregar:
   - Validación de formato de email
   - Validación de número de teléfono
   - Límites de caracteres

### 🔄 Próximos Pasos Sugeridos

1. **Implementar Login/Registro:**
   - Crear pantalla de login
   - Guardar token de autenticación
   - Implementar refresh token

2. **Agregar Funcionalidades:**
   - Cambiar foto de perfil
   - Cambiar contraseña
   - Gestión de mascotas
   - Reportes de mascotas perdidas

3. **Mejorar UX:**
   - Agregar animaciones
   - Pull-to-refresh
   - Caché de datos locales
   - Modo offline

4. **Testing:**
   - Unit tests para ViewModels
   - Integration tests para Repository
   - UI tests para Activities

### ❓ Soporte

Si encuentras problemas:
1. Verifica que las dependencias se hayan descargado correctamente
2. Revisa el Logcat para ver errores
3. Verifica la conectividad con el endpoint de la API
4. Asegúrate de tener los permisos necesarios en el manifest

---

**Estado del Proyecto:** ✅ Listo para sincronizar con Gradle y probar

