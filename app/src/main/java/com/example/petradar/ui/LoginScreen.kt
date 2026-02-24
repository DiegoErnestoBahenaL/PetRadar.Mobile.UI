package com.example.petradar.ui

import android.util.Patterns
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.petradar.R
import com.example.petradar.ui.theme.*
import com.example.petradar.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val isLoadingRaw by viewModel.isLoading.observeAsState(false)
    val isLoading = isLoadingRaw ?: false
    val errorMessage by viewModel.errorMessage.observeAsState()
    val userProfile by viewModel.userProfile.observeAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(userProfile) {
        if (userProfile != null) onLoginSuccess()
    }

    LaunchedEffect(errorMessage) {
        val msg = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
    }

    fun validate(): Boolean {
        var ok = true
        if (email.isBlank()) { emailError = "El email es requerido"; ok = false }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { emailError = "Email inválido"; ok = false }
        if (password.isBlank()) { passwordError = "La contraseña es requerida"; ok = false }
        else if (password.length < 6) { passwordError = "Mínimo 6 caracteres"; ok = false }
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
                        0.45f to GradientMid,
                        1f to GradientBottom
                    )
                )
                .padding(innerPadding)
        ) {
            // ── Decorative glossy circles ──────────────────────────
            GlossyCircle(size = 260.dp, offsetX = (-80).dp, offsetY = (-60).dp, color = Color.White.copy(alpha = 0.07f))
            GlossyCircle(size = 180.dp, offsetX = 220.dp, offsetY = 120.dp,   color = PetGold.copy(alpha = 0.10f))
            GlossyCircle(size = 140.dp, offsetX = (-40).dp, offsetY = 500.dp,  color = Color.White.copy(alpha = 0.05f))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
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
                        // Glossy logo container
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.10f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.petradar_logo),
                                contentDescription = "PetRadar logo",
                                modifier = Modifier.size(64.dp),
                                tint = Color.Unspecified
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = "PetRadar",
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Cuida a quienes más amas 🐾",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))

                // ── Glass card ────────────────────────────────────
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(500, 150)) + slideInVertically(tween(500, 150)) { 80 }
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.15f)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Iniciar sesión",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )

                            // Email field
                            GlassTextField(
                                value = email,
                                onValueChange = { email = it; emailError = null },
                                label = "Correo electrónico",
                                icon = Icons.Default.Email,
                                error = emailError,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                enabled = !isLoading
                            )

                            // Password field
                            GlassTextField(
                                value = password,
                                onValueChange = { password = it; passwordError = null },
                                label = "Contraseña",
                                icon = Icons.Default.Lock,
                                error = passwordError,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f)
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
                                enabled = !isLoading
                            )

                            // Login button
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    if (validate()) viewModel.login(email, password)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = GradientMid
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = GradientMid,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Entrar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Register link ──────────────────────────────────
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(500, 300))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("¿Aún no tienes cuenta? ", color = Color.White.copy(alpha = 0.8f))
                        TextButton(
                            onClick = onNavigateToRegister,
                            enabled = !isLoading,
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text(
                                "Regístrate",
                                color = PetGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/** Frosted-glass style OutlinedTextField */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    error: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.8f)) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.8f)) },
        trailingIcon = trailingIcon,
        isError = error != null,
        supportingText = error?.let { err -> { Text(err, color = Color(0xFFFF8A80)) } },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        enabled = enabled,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            disabledTextColor = Color.White.copy(alpha = 0.5f),
            focusedBorderColor = Color.White.copy(alpha = 0.9f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
            errorBorderColor = Color(0xFFFF8A80),
            cursorColor = Color.White,
            focusedLabelColor = Color.White,
            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
        ),
        shape = RoundedCornerShape(14.dp)
    )
}

/** Decorative blurry circle */
@Composable
fun GlossyCircle(
    size: androidx.compose.ui.unit.Dp,
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
    color: Color
) {
    Box(
        modifier = Modifier
            .size(size)
            .offset(x = offsetX, y = offsetY)
            .blur(60.dp)
            .clip(CircleShape)
            .background(color)
    )
}

