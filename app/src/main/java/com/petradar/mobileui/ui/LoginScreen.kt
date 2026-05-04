package com.petradar.mobileui.ui

import android.util.Patterns
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petradar.mobileui.R
import com.petradar.mobileui.viewmodel.LoginViewModel

/**
 * PetRadar sign-in screen.
 *
 * Contains:
 *  - Logo and slogan animated with fade + slide on entry.
 *  - Email field with format validation.
 *  - Password field with visibility toggle.
 *  - "Sign In" button that triggers [LoginViewModel.login].
 *  - Link to the registration screen.
 *  - Snackbar for displaying authentication errors.
 *
 * Validation is performed locally before calling the ViewModel, showing
 * error messages below each invalid field.
 *
 * @param viewModel           ViewModel that executes the login and exposes the state.
 * @param onLoginSuccess      Callback invoked when login succeeds; navigates to Home.
 * @param onNavigateToRegister Callback to open [com.petradar.mobileui.RegisterActivity].
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val isLoadingRaw by viewModel.isLoading.observeAsState(false)
    val isLoading = isLoadingRaw
    val errorMessage by viewModel.errorMessage.observeAsState()
    val loginSuccess by viewModel.loginSuccess.observeAsState()
    val recoverPasswordSent by viewModel.recoverPasswordSent.observeAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }

    var showRecoverDialog by remember { mutableStateOf(false) }
    var recoverEmail by remember { mutableStateOf("") }
    var recoverEmailError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(loginSuccess) {
        if (loginSuccess == true) onLoginSuccess()
    }

    LaunchedEffect(errorMessage) {
        val msg = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
    }

    LaunchedEffect(recoverPasswordSent) {
        if (recoverPasswordSent == true) {
            showRecoverDialog = false
            recoverEmail = ""
            recoverEmailError = null
            snackbarHostState.showSnackbar("Correo de recuperación enviado. Revisa tu bandeja.")
            viewModel.clearRecoverPasswordSent()
        }
    }

    if (showRecoverDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isLoading) {
                    showRecoverDialog = false
                    recoverEmail = ""
                    recoverEmailError = null
                }
            },
            icon = { Icon(Icons.Default.Email, contentDescription = null) },
            title = { Text("Recuperar contraseña") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ingresa tu correo y te enviaremos instrucciones para restablecer tu contraseña.")
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = recoverEmail,
                        onValueChange = { recoverEmail = it; recoverEmailError = null },
                        label = { Text("Correo electrónico") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        isError = recoverEmailError != null,
                        supportingText = recoverEmailError?.let { err -> { Text(err) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (recoverEmail.isBlank()) {
                                recoverEmailError = "El correo es requerido"
                            } else if (!Patterns.EMAIL_ADDRESS.matcher(recoverEmail).matches()) {
                                recoverEmailError = "Correo inválido"
                            } else {
                                viewModel.recoverPassword(recoverEmail)
                            }
                        }),
                        enabled = !isLoading,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (recoverEmail.isBlank()) {
                            recoverEmailError = "El correo es requerido"
                        } else if (!Patterns.EMAIL_ADDRESS.matcher(recoverEmail).matches()) {
                            recoverEmailError = "Correo inválido"
                        } else {
                            viewModel.recoverPassword(recoverEmail)
                        }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Enviar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRecoverDialog = false
                        recoverEmail = ""
                        recoverEmailError = null
                    },
                    enabled = !isLoading
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    fun validate(): Boolean {
        var ok = true
        if (email.isBlank()) { emailError = "El email es requerido"; ok = false }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { emailError = "Email inválido"; ok = false }
        if (password.isBlank()) { passwordError = "La contraseña es requerida"; ok = false }
        return ok
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // ── Logo + Title ───────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -60 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.petradar_logo),
                        contentDescription = "PetRadar logo",
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "PetRadar",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Cuida a quienes más amas 🐾",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // ── Login Form ────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, 150)) + slideInVertically(tween(500, 150)) { 80 }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; emailError = null },
                        label = { Text("Correo electrónico") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        isError = emailError != null,
                        supportingText = emailError?.let { err -> { Text(err) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        enabled = !isLoading,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; passwordError = null },
                        label = { Text("Contraseña") },
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (validate()) viewModel.login(email, password)
                        }),
                        enabled = !isLoading,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (validate()) viewModel.login(email, password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Entrar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    TextButton(
                        onClick = { showRecoverDialog = true },
                        enabled = !isLoading,
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(
                            "¿Olvidaste tu contraseña?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Register link ──────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, 300))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("¿Aún no tienes cuenta? ")
                    TextButton(
                        onClick = onNavigateToRegister,
                        enabled = !isLoading,
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(
                            "Regístrate",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}