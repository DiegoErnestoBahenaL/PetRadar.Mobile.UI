package com.example.petradar.ui

import android.util.Patterns
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petradar.ui.theme.*
import com.example.petradar.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: LoginViewModel,
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val isLoadingRaw by viewModel.isLoading.observeAsState(false)
    val isLoading = isLoadingRaw ?: false
    val errorMessage by viewModel.errorMessage.observeAsState()
    val userProfile by viewModel.userProfile.observeAsState()

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

    var visible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(userProfile) { if (userProfile != null) onRegisterSuccess() }
    LaunchedEffect(errorMessage) {
        val msg = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
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
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to GradientTop,
                        0.5f to GradientMid,
                        1f to GradientBottom
                    )
                )
                .padding(innerPadding)
        ) {
            // Decorative circles
            GlossyCircle(size = 220.dp, offsetX = 180.dp, offsetY = (-40).dp, color = PetGold.copy(alpha = 0.09f))
            GlossyCircle(size = 160.dp, offsetX = (-60).dp, offsetY = 350.dp, color = Color.White.copy(alpha = 0.05f))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                // Top bar (back button)
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    Text("Crear cuenta", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }

                Spacer(Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(400, 80)) + slideInVertically(tween(400, 80)) { 60 }
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Datos personales", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.weight(1f)) {
                                    GlassTextField(
                                        value = firstName,
                                        onValueChange = { firstName = it; firstNameError = null },
                                        label = "Nombre *",
                                        icon = Icons.Default.Person,
                                        error = firstNameError,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                                        enabled = !isLoading
                                    )
                                }
                                Box(Modifier.weight(1f)) {
                                    GlassTextField(
                                        value = lastName,
                                        onValueChange = { lastName = it; lastNameError = null },
                                        label = "Apellido *",
                                        icon = Icons.Default.Person,
                                        error = lastNameError,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                        enabled = !isLoading
                                    )
                                }
                            }

                            GlassTextField(
                                value = email,
                                onValueChange = { email = it; emailError = null },
                                label = "Correo electrónico *",
                                icon = Icons.Default.Email,
                                error = emailError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                enabled = !isLoading
                            )

                            GlassTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = "Teléfono (opcional)",
                                icon = Icons.Default.Phone,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                enabled = !isLoading
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                            Text("Seguridad", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                            GlassTextField(
                                value = password,
                                onValueChange = { password = it; passwordError = null },
                                label = "Contraseña *",
                                icon = Icons.Default.Lock,
                                error = passwordError,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null, tint = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                enabled = !isLoading
                            )

                            GlassTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; confirmPasswordError = null },
                                label = "Confirmar contraseña *",
                                icon = Icons.Default.LockOpen,
                                error = confirmPasswordError,
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null, tint = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                enabled = !isLoading
                            )

                            // Privacy checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = privacyAccepted,
                                    onCheckedChange = { privacyAccepted = it; privacyError = false },
                                    enabled = !isLoading,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = PetGold,
                                        uncheckedColor = if (privacyError) Color(0xFFFF8A80) else Color.White.copy(alpha = 0.7f),
                                        checkmarkColor = Color.Black
                                    )
                                )
                                Text(
                                    text = if (privacyError) "Debes aceptar el aviso de privacidad"
                                    else "Acepto el aviso de privacidad",
                                    color = if (privacyError) Color(0xFFFF8A80) else Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp
                                )
                            }

                            // Register button
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
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = GradientBottom
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = GradientBottom, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Crear cuenta", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
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
                        Text("¿Ya tienes cuenta? ", color = Color.White.copy(alpha = 0.8f))
                        TextButton(onClick = onBack, enabled = !isLoading, contentPadding = PaddingValues(4.dp)) {
                            Text("Inicia sesión", color = PetGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

