# 🐛 Solución de Problemas - App Crashea al Iniciar

## ⚠️ Problema
La app PetRadar se inicia pero crashea inmediatamente.

---

## ✅ Soluciones Paso a Paso

### 1. **Limpiar Datos de la App (MÁS IMPORTANTE)**

La app puede tener datos corruptos de versiones anteriores. 

**En el dispositivo/emulador:**
1. Ve a **Configuración → Apps**
2. Busca **PetRadar** (com.example.petradar)
3. Toca en la app
4. Toca **Almacenamiento**
5. Toca **Borrar datos** y **Borrar caché**
6. Vuelve a ejecutar la app

**O desde Android Studio:**
```
Run → Stop (detener la app)
→ Desinstalar la app del dispositivo
→ Run (instalar y ejecutar de nuevo)
```

---

### 2. **Limpiar y Reconstruir el Proyecto**

**En Android Studio:**
```
Build → Clean Project
(Espera a que termine)
Build → Rebuild Project
(Espera a que termine)
Run (▶️)
```

**O desde terminal:**
```powershell
cd C:\Users\U\StudioProjects\PetRadar.Mobile.UI
.\gradlew clean
.\gradlew assembleDebug
```

---

### 3. **Ver el Log Completo del Crash**

**En Android Studio:**
1. Abre la pestaña **Logcat** (parte inferior)
2. Selecciona tu dispositivo/emulador
3. En el filtro, selecciona **Error** o **Assert**
4. Ejecuta la app y espera el crash
5. Busca líneas que digan:
   - `FATAL EXCEPTION`
   - `AndroidRuntime`
   - `com.example.petradar`

**Copia el stack trace completo** y compártelo.

---

### 4. **Desinstalar Completamente la App**

```powershell
# Si tienes adb configurado:
adb uninstall com.example.petradar
adb install app/build/outputs/apk/debug/app-debug.apk
```

**O manualmente:**
- Mantén presionado el ícono de PetRadar en el dispositivo
- Arrastra a "Desinstalar"
- Vuelve a instalar desde Android Studio

---

### 5. **Verificar que RetrofitClient se Inicialice**

La app necesita que `PetRadarApplication` se ejecute primero. Verifica que el manifest tenga:

```xml
<application
    android:name=".PetRadarApplication"
    ...>
```

✅ Ya está configurado correctamente.

---

### 6. **Verificar Permisos de Internet**

La app necesita internet. Verifica que el manifest tenga:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

✅ Ya está configurado correctamente.

---

## 🔍 Cómo Obtener el Stack Trace del Error

### Método 1: Logcat en Android Studio

1. **Run (▶️)** la app
2. Cuando crashee, ve a **Logcat**
3. Busca líneas rojas con el error
4. Copia desde `FATAL EXCEPTION` hasta el final del error

Ejemplo de lo que buscas:
```
E/AndroidRuntime: FATAL EXCEPTION: main
    Process: com.example.petradar, PID: 12345
    java.lang.RuntimeException: Unable to start activity
    ...
    at com.example.petradar.LoginActivity.onCreate(LoginActivity.kt:34)
    ...
```

### Método 2: Desde el dispositivo (sin Android Studio)

Si usas el dispositivo directamente:
1. Ve a **Configuración → Sistema → Acerca del teléfono**
2. Toca **Número de compilación** 7 veces (activar opciones de desarrollador)
3. Ve a **Configuración → Sistema → Opciones de desarrollador**
4. Activa **Depuración USB**
5. Conecta el dispositivo a la PC
6. Ejecuta en PowerShell:
   ```powershell
   adb logcat -d > crash_log.txt
   ```
7. Abre `crash_log.txt` y busca el error

---

## 🐛 Errores Comunes y Soluciones

### Error: "Unable to instantiate activity"
**Causa:** La Activity no está registrada en el manifest  
**Solución:** ✅ Ya están todas registradas

### Error: "Resources$NotFoundException"
**Causa:** Falta un recurso (imagen, string, color)  
**Solución:** Verificar que `petradar_logo.png` exista en `res/drawable/`  
✅ Ya existe

### Error: "NullPointerException" en onCreate
**Causa:** Un View no se encuentra en el layout  
**Solución:** ✅ Agregué try-catch para mostrar el error

### Error: "ClassNotFoundException"
**Causa:** Falta una clase o dependencia  
**Solución:** Clean + Rebuild

---

## 📱 Pasos de Depuración Recomendados

### Paso 1: Limpiar TODO
```powershell
# En el proyecto
cd C:\Users\U\StudioProjects\PetRadar.Mobile.UI
.\gradlew clean

# Desinstalar del dispositivo (Android Studio)
Run → Stop
Desinstalar manualmente la app del dispositivo
```

### Paso 2: Reconstruir
```
Build → Rebuild Project
(Esperar a que termine)
```

### Paso 3: Instalar Limpio
```
Run → Run 'app' (▶️)
```

### Paso 4: Observar Logcat
- Si crashea, copiar el stack trace completo
- Buscar la línea que dice `at com.example.petradar...`
- Identificar qué línea de código causa el problema

---

## 🔧 Cambios que Hice para Ayudar

### 1. Try-Catch en LoginActivity
Agregué manejo de errores en `onCreate()`:
```kotlin
try {
    // Código de inicialización
} catch (e: Exception) {
    e.printStackTrace()
    Toast.makeText(this, "Error al iniciar: ${e.message}", Toast.LENGTH_LONG).show()
    finish()
}
```

Ahora si hay un error, verás un Toast con el mensaje antes de que cierre.

### 2. Corregí onBackPressed
Agregué `@Deprecated` y `super.onBackPressed()` para evitar warnings.

---

## 📞 Siguiente Paso

**Por favor, haz esto:**

1. **Desinstala la app completamente** del dispositivo
2. **Clean Project** en Android Studio
3. **Rebuild Project**
4. **Run** de nuevo
5. **Si crashea**, abre Logcat y copia el error que aparece en rojo
6. Compárteme el error completo (desde `FATAL EXCEPTION` hasta el final)

---

## 💡 Tip: Modo Avión para Probar

Si el crash es por problemas de red, puedes probar:
1. Activa modo avión
2. Ejecuta la app
3. Si NO crashea, el problema es de red/API
4. Si SÍ crashea, el problema es del código/layout

---

## ✅ Lo que Sabemos que Está Bien

- ✅ El manifest está correcto
- ✅ PetRadarApplication está registrada
- ✅ Permisos de internet están
- ✅ LoginActivity está registrada como LAUNCHER
- ✅ El logo petradar_logo.png existe
- ✅ Los layouts están sintácticamente correctos

**El problema está en runtime, no en compilación.**

---

**Siguiente acción: Limpiar datos de la app y obtener el stack trace completo del crash.** 🔍

