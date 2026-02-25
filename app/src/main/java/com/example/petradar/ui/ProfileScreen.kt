@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.petradar.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.petradar.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    userId: Long,
    onBack: () -> Unit
) {
    val userProfile by viewModel.userProfile.observeAsState()
    val isLoading = viewModel.isLoading.observeAsState(false).value
    val errorMessage by viewModel.errorMessage.observeAsState()
    val updateSuccess = viewModel.updateSuccess.observeAsState(false).value

    var editMode by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) { if (!isLoading) isRefreshing = false }

    var firstName   by remember { mutableStateOf("") }
    var lastName    by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var firstNameError by remember { mutableStateOf<String?>(null) }

    var visible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(userProfile, editMode) {
        val p = userProfile ?: return@LaunchedEffect
        if (editMode) {
            firstName   = p.name
            lastName    = p.lastName ?: ""
            phoneNumber = p.phoneNumber ?: ""
            newPassword = ""
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            viewModel.clearUpdateSuccess()
            editMode = false
            snackbarHostState.showSnackbar("Perfil actualizado ✓")
        }
    }

    val onBackPressed: () -> Unit = {
        if (editMode) editMode = false else onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editMode) "Editar Perfil" else "Mi Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (!editMode && userProfile != null) {
                        IconButton(onClick = { editMode = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        AnimatedContent(
            targetState = editMode,
            transitionSpec = {
                if (targetState) {
                    slideInVertically(tween(320)) { it } + fadeIn(tween(320)) togetherWith
                        slideOutVertically(tween(320)) { -it } + fadeOut(tween(320))
                } else {
                    slideInVertically(tween(320)) { -it } + fadeIn(tween(320)) togetherWith
                        slideOutVertically(tween(320)) { it } + fadeOut(tween(320))
                }
            },
            label = "profile_mode"
        ) { isEditMode ->
            if (isEditMode) {
                EditProfileContent(
                    isLoading = isLoading,
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumber = phoneNumber,
                    newPassword = newPassword,
                    showPassword = showPassword,
                    firstNameError = firstNameError,
                    padding = padding,
                    onFirstNameChange = { firstName = it; firstNameError = null },
                    onLastNameChange  = { lastName = it },
                    onPhoneChange     = { phoneNumber = it },
                    onPasswordChange  = { newPassword = it },
                    onTogglePassword  = { showPassword = !showPassword },
                    onCancel = { editMode = false },
                    onSave = {
                        if (firstName.isBlank()) { firstNameError = "El nombre es requerido"; return@EditProfileContent }
                        val profileId = userProfile?.id ?: return@EditProfileContent
                        viewModel.updateProfile(
                            userId      = profileId,
                            name        = firstName.trim(),
                            lastName    = lastName.trim().ifEmpty { null },
                            phoneNumber = phoneNumber.trim().ifEmpty { null },
                            password    = newPassword.trim().ifEmpty { null }
                        )
                    }
                )
            } else {
                ViewProfileContent(
                    userProfile = userProfile,
                    isLoading   = isLoading,
                    isRefreshing = isRefreshing,
                    visible     = visible,
                    padding     = padding,
                    onRefresh   = { isRefreshing = true; viewModel.loadUserProfile(userId) },
                    onEdit      = { editMode = true }
                )
            }
        }
    }
}

@Composable
private fun ViewProfileContent(
    userProfile: com.example.petradar.api.models.UserProfile?,
    isLoading: Boolean,
    isRefreshing: Boolean,
    visible: Boolean,
    padding: PaddingValues,
    onRefresh: () -> Unit,
    onEdit: () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize().padding(padding)
    ) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (isLoading && userProfile == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        val profile = userProfile ?: return@Box

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(visible, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -40 }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.name.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = listOfNotNull(profile.name, profile.lastName).joinToString(" "),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (profile.email.isNotBlank()) {
                        Text(
                            text = profile.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                    profile.role?.let { role ->
                        Spacer(Modifier.height(4.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text(role) },
                        )
                    }
                }
            }

            AnimatedVisibility(visible, enter = fadeIn(tween(400, 120)) + slideInVertically(tween(400, 120)) { 40 }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        ProfileInfoRow(Icons.Default.Person, "Nombre", profile.name)
                        if (!profile.lastName.isNullOrBlank())
                            ProfileInfoRow(Icons.Default.Person, "Apellido", profile.lastName)
                        ProfileInfoRow(Icons.Default.Email, "Correo", profile.email)
                        if (!profile.phoneNumber.isNullOrBlank())
                            ProfileInfoRow(Icons.Default.Phone, "Teléfono", profile.phoneNumber)
                    }
                }
            }

            if (!profile.organizationName.isNullOrBlank()) {
                AnimatedVisibility(visible, enter = fadeIn(tween(400, 200)) + slideInVertically(tween(400, 200)) { 40 }) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Organización", style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 6.dp))
                            ProfileInfoRow(Icons.Default.Business, "Nombre",
                                profile.organizationName
                            )
                            if (!profile.organizationAddress.isNullOrBlank())
                                ProfileInfoRow(Icons.Default.LocationOn, "Dirección",
                                    profile.organizationAddress
                                )
                            if (!profile.organizationPhone.isNullOrBlank())
                                ProfileInfoRow(Icons.Default.Phone, "Teléfono",
                                    profile.organizationPhone
                                )
                        }
                    }
                }
            }

            AnimatedVisibility(visible, enter = fadeIn(tween(400, 280)) + slideInVertically(tween(400, 280)) { 60 }) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Editar perfil", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }   // end Box
    }   // end PullToRefreshBox
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun EditProfileContent(
    isLoading: Boolean,
    firstName: String,
    lastName: String,
    phoneNumber: String,
    newPassword: String,
    showPassword: Boolean,
    firstNameError: String?,
    padding: PaddingValues,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Información Personal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Card(
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = firstName, onValueChange = onFirstNameChange,
                    label = { Text("Nombre *") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    isError = firstNameError != null,
                    supportingText = firstNameError?.let { e -> { Text(e) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = lastName, onValueChange = onLastNameChange,
                    label = { Text("Apellido") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = phoneNumber, onValueChange = onPhoneChange,
                    label = { Text("Teléfono") },
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }
        }

        Text("Cambiar contraseña", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = newPassword, onValueChange = onPasswordChange,
                    label = { Text("Nueva contraseña (opcional)") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = onTogglePassword) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Ocultar" else "Mostrar"
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Cancelar", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f).height(52.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Guardar", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
