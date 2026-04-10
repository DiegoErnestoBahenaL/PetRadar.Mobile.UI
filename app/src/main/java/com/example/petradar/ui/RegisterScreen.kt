package com.example.petradar.ui

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.petradar.viewmodel.LoginViewModel
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

    // Photo source bottom sheet
    if (showPhotoSheet) {
        ModalBottomSheet(onDismissRequest = { }) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text("Foto de perfil", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
                ListItem(
                    headlineContent = { Text("Tomar foto") },
                    leadingContent = { Icon(Icons.Default.CameraAlt, null) },
                    modifier = Modifier.clickable {
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
                    modifier = Modifier.clickable { galleryLauncher.launch("image/*") }
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
                        Text(
                            text = if (privacyError) "Debes aceptar el aviso de privacidad"
                            else "Acepto el aviso de privacidad",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (privacyError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                        )
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
