package com.petradar.mobileui.ui

import android.Manifest
import android.net.Uri
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.petradar.mobileui.viewmodel.LoginViewModel
import java.io.File

/**
 * New account registration screen for PetRadar.
 *
 * Contains two sections:
 *  - **Personal data**: First name*, Last name*, Email*, Phone (optional).
 *  - **Security**: Password*, Confirm password*, Privacy policy checkbox.
 *
 * Validation is performed locally before calling the ViewModel:
 *  - Required fields must not be blank.
 *  - Email must match a valid format.
 *  - Password must be at least 6 characters.
 *  - Passwords must match.
 *  - Privacy policy checkbox must be checked.
 *
 * Calls [LoginViewModel.register] on submit; if successful (201 Created),
 * the ViewModel stores the credentials and `RegisterActivity` triggers an automatic login.
 *
 * Note: In the QA environment POST /api/Users may require admin authentication.
 * If it returns 401, an explanatory message is shown in the Snackbar.
 *
 * @param viewModel         ViewModel shared with LoginActivity to reuse the post-registration login.
 * @param onRegisterSuccess Callback invoked after a successful post-registration login; navigates to Home.
 * @param onBack            Callback to return to the login screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: LoginViewModel,
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit,
    onPhotoSelected: (Uri) -> Unit = {}
) {
    val context = LocalContext.current
    val isLoadingRaw by viewModel.isLoading.observeAsState(false)
    val isLoading = isLoadingRaw
    val errorMessage by viewModel.errorMessage.observeAsState()
    val loginSuccess by viewModel.loginSuccess.observeAsState()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var privacyAccepted by remember { mutableStateOf(false) }

    var firstNameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var privacyError by remember { mutableStateOf(false) }

    // Photo state
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoSheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    fun createCameraUri(): Uri {
        val photoDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
        val photoFile = File(photoDir, "profile_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { selectedPhotoUri = uri; onPhotoSelected(uri) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) { cameraImageUri?.let { selectedPhotoUri = it; onPhotoSelected(it) } }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createCameraUri()
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }
    var visible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(loginSuccess) { if (loginSuccess == true) onRegisterSuccess() }
    LaunchedEffect(errorMessage) {
        val msg = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
    }

    // Privacy notice bottom sheet
    if (showPrivacySheet) {
        ModalBottomSheet(onDismissRequest = { showPrivacySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Aviso de Privacidad",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Última actualización: 28 de abril de 2026",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("1. Identidad del responsable", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "El responsable del tratamiento de los datos personales recabados a través de la aplicación móvil y del sitio web PetRadar es Diego Ernesto Bahena López (en adelante, \"PetRadar\", \"nosotros\" o \"el responsable\"), con domicilio en Guadalajara, Jalisco, México. Para cualquier consulta relacionada con este aviso puede contactarnos en business@petradar-qa.org.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("2. Alcance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Este aviso describe cómo PetRadar recopila, utiliza, almacena, comparte y protege la información personal de las personas que utilizan nuestra aplicación móvil para Android, nuestra aplicación web y los servicios relacionados (en conjunto, los \"Servicios\"). Al utilizar los Servicios usted acepta las prácticas descritas en este documento.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("3. Información que recopilamos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("Recopilamos las siguientes categorías de información:", style = MaterialTheme.typography.bodyMedium)
                Text("• Datos de cuenta: nombre, correo electrónico, contraseña cifrada y, opcionalmente, número de teléfono, proporcionados al registrarse.", style = MaterialTheme.typography.bodyMedium)
                Text("• Datos de mascota: nombre, especie, raza, edad, sexo, color, señas particulares, fotografías y cualquier otra información que el usuario decida agregar a la ficha de su mascota.", style = MaterialTheme.typography.bodyMedium)
                Text("• Reportes y avistamientos: descripciones, fotografías, ubicación aproximada y fecha del evento reportado.", style = MaterialTheme.typography.bodyMedium)
                Text("• Imágenes y contenido de la cámara: fotografías que el usuario decide tomar o seleccionar para asociarlas a una mascota o a un reporte de avistamiento.", style = MaterialTheme.typography.bodyMedium)
                Text("• Datos de ubicación: ubicación geográfica aproximada asociada a un reporte o avistamiento, cuando el usuario opta por proporcionarla.", style = MaterialTheme.typography.bodyMedium)
                Text("• Datos técnicos y de uso: identificadores del dispositivo, versión del sistema operativo, modelo, idioma, registros de errores y eventos de uso necesarios para operar y mejorar el servicio.", style = MaterialTheme.typography.bodyMedium)

                Text("4. Permiso de cámara (android.permission.CAMERA)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "La aplicación móvil de PetRadar solicita el permiso de cámara con la finalidad exclusiva de permitir al usuario tomar fotografías de su mascota para crear o actualizar la ficha de identificación, o fotografiar animales avistados para incluirlos en un reporte.\n\nEl acceso a la cámara se activa únicamente cuando el usuario presiona de forma explícita el botón correspondiente. PetRadar no accede a la cámara en segundo plano, no graba audio, no graba vídeo y no recopila imágenes sin la acción directa del usuario.\n\nPuede revocar el permiso en cualquier momento desde Ajustes → Aplicaciones → PetRadar → Permisos.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("5. Finalidades del tratamiento", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("• Crear y administrar su cuenta de usuario.", style = MaterialTheme.typography.bodyMedium)
                Text("• Registrar mascotas y reportes de avistamientos, y mostrarlos a la comunidad para favorecer la reunificación de mascotas perdidas.", style = MaterialTheme.typography.bodyMedium)
                Text("• Generar alertas y notificaciones cuando se detecten posibles coincidencias entre mascotas reportadas como perdidas y avistamientos cercanos.", style = MaterialTheme.typography.bodyMedium)
                Text("• Operar, mantener, depurar y mejorar los Servicios.", style = MaterialTheme.typography.bodyMedium)
                Text("• Cumplir con obligaciones legales aplicables y prevenir usos indebidos del servicio.", style = MaterialTheme.typography.bodyMedium)

                Text("6. Base de legitimación", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "El tratamiento de los datos se realiza con fundamento en el consentimiento que el usuario otorga al registrarse y utilizar los Servicios, así como en el interés legítimo de PetRadar para prestar y mejorar la plataforma y cumplir con sus obligaciones legales.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("7. Compartición con terceros", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("PetRadar no vende su información personal. Compartimos datos únicamente en los siguientes supuestos:", style = MaterialTheme.typography.bodyMedium)
                Text("• Visibilidad pública dentro del servicio: la información contenida en una ficha de mascota perdida o en un reporte de avistamiento puede ser visible para otros usuarios de la comunidad.", style = MaterialTheme.typography.bodyMedium)
                Text("• Proveedores de infraestructura: empleamos proveedores de alojamiento en la nube, almacenamiento y análisis para operar el servicio.", style = MaterialTheme.typography.bodyMedium)
                Text("• Autoridades: cuando la legislación aplicable lo requiera o ante un requerimiento válido de autoridad competente.", style = MaterialTheme.typography.bodyMedium)

                Text("8. Conservación de los datos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Conservamos sus datos mientras su cuenta permanezca activa y por los periodos adicionales necesarios para cumplir con obligaciones legales, resolver disputas o hacer cumplir nuestros acuerdos. Cuando los datos ya no sean necesarios, serán eliminados o anonimizados.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("9. Derechos del titular", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Usted tiene derecho a acceder, rectificar, cancelar u oponerse al tratamiento de sus datos personales (derechos ARCO), así como a revocar el consentimiento otorgado y a limitar el uso o divulgación de su información. Para ejercer estos derechos puede editar o eliminar fichas, reportes y fotografías directamente desde la aplicación, o solicitar la eliminación total de su cuenta enviando una solicitud a business@petradar-qa.org desde la dirección de correo registrada.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("10. Seguridad", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Implementamos medidas técnicas, administrativas y físicas razonables para proteger sus datos personales contra el acceso no autorizado, la pérdida, alteración o divulgación. Las contraseñas se almacenan cifradas y la comunicación con nuestros servidores se realiza mediante conexiones HTTPS.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("11. Menores de edad", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Los Servicios no están dirigidos a menores de 13 años. Si tomamos conocimiento de que hemos recopilado información personal de un menor sin el consentimiento verificable de quien ejerce la patria potestad o tutela, eliminaremos dicha información a la brevedad.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("12. Transferencias internacionales", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Sus datos pueden ser almacenados o procesados en servidores ubicados fuera de su país de residencia. En todos los casos adoptamos las medidas necesarias para que dichas transferencias se realicen con un nivel de protección adecuado.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("13. Cambios a este aviso", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Podemos actualizar este Aviso de Privacidad para reflejar cambios en nuestras prácticas o por motivos legales u operativos. Publicaremos la versión vigente e indicaremos la fecha de la última actualización. Le recomendamos revisarlo periódicamente.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("14. Contacto", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Para cualquier duda, comentario o ejercicio de derechos relacionados con este Aviso de Privacidad, puede comunicarse con nosotros a través del correo business@petradar-qa.org.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    // Photo source bottom sheet
    if (showPhotoSheet) {
        ModalBottomSheet(onDismissRequest = { showPhotoSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text("Foto de perfil", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
                ListItem(
                    headlineContent = { Text("Tomar foto") },
                    leadingContent = { Icon(Icons.Default.CameraAlt, null) },
                    modifier = Modifier.clickable {
                        showPhotoSheet = false
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            val uri = createCameraUri()
                            cameraImageUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                )
                ListItem(
                    headlineContent = { Text("Elegir de la galería") },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, null) },
                    modifier = Modifier.clickable {
                        showPhotoSheet = false
                        galleryLauncher.launch("image/*")
                    }
                )
            }
        }
    }

    fun validate(): Boolean {
        var ok = true
        if (firstName.isBlank()) { firstNameError = "Requerido"; ok = false }
        if (lastName.isBlank()) { lastNameError = "Requerido"; ok = false }
        if (email.isBlank()) { emailError = "Requerido"; ok = false }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { emailError = "Email inválido"; ok = false }
        if (password.isBlank()) { passwordError = "Requerido"; ok = false }
        else if (password.length < 6) { passwordError = "Mínimo 6 caracteres"; ok = false }
        if (confirmPassword.isBlank()) { confirmPasswordError = "Requerido"; ok = false }
        else if (password != confirmPassword) { confirmPasswordError = "Las contraseñas no coinciden"; ok = false }
        if (!privacyAccepted) { privacyError = true; ok = false }
        return ok
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Crear cuenta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 80)) + slideInVertically(tween(400, 80)) { 60 }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ── Profile photo (optional) ──────────────────────────
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .clickable { showPhotoSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedPhotoUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(selectedPhotoUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Foto seleccionada",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(44.dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable { },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(15.dp))
                            }
                        }
                        Text(
                            text = if (selectedPhotoUri != null) "Foto seleccionada ✓" else "Foto de perfil (opcional)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedPhotoUri != null) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    HorizontalDivider()
                    Text("Datos personales", style = MaterialTheme.typography.titleMedium)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it; firstNameError = null },
                            label = { Text("Nombre *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            isError = firstNameError != null,
                            supportingText = firstNameError?.let { err -> { Text(err) } },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                            enabled = !isLoading,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it; lastNameError = null },
                            label = { Text("Apellido *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            isError = lastNameError != null,
                            supportingText = lastNameError?.let { err -> { Text(err) } },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            enabled = !isLoading,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; emailError = null },
                        label = { Text("Correo electrónico *") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        isError = emailError != null,
                        supportingText = emailError?.let { err -> { Text(err) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        enabled = !isLoading,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono (opcional)") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        enabled = !isLoading,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    HorizontalDivider()
                    Text("Seguridad", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; passwordError = null },
                        label = { Text("Contraseña *") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        isError = passwordError != null,
                        supportingText = passwordError?.let { err -> { Text(err) } },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        enabled = !isLoading,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; confirmPasswordError = null },
                        label = { Text("Confirmar contraseña *") },
                        leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null) },
                        isError = confirmPasswordError != null,
                        supportingText = confirmPasswordError?.let { err -> { Text(err) } },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        enabled = !isLoading,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = privacyAccepted,
                            onCheckedChange = { privacyAccepted = it; privacyError = false },
                            enabled = !isLoading,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        if (privacyError) {
                            Text(
                                text = "Debes aceptar el aviso de privacidad",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = "Acepto el ",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "aviso de privacidad",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = TextDecoration.Underline
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(enabled = !isLoading) {
                                    showPrivacySheet = true
                                }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (validate()) {
                                viewModel.register(
                                    firstName.trim(), lastName.trim(),
                                    email.trim(), password, phone.trim().ifEmpty { null }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Crear cuenta", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(visible = visible, enter = fadeIn(tween(400, 300))) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿Ya tienes cuenta? ")
                    TextButton(onClick = onBack, enabled = !isLoading, contentPadding = PaddingValues(4.dp)) {
                        Text("Inicia sesión", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
