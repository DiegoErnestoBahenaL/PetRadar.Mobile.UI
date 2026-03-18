package com.example.petradar.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.petradar.viewmodel.LoginViewModel

/**
 * Screen shown after registration (or after a login attempt on an unverified account).
 *
 * The server sends a verification link to the user's inbox.
 * Tapping that link verifies the account on the server side — no token needs to
 * be entered here.
 *
 * This screen:
 *  - Informs the user that a link has been sent to [email].
 *  - Polls the API every 5 s in the background (via [LoginViewModel.startPollingEmailVerification]).
 *  - Automatically calls [onVerified] as soon as [LoginViewModel.emailVerified] becomes `true`.
 *  - Offers a "Ya verifiqué mi cuenta" button for an immediate manual check.
 *
 * @param viewModel  Shared [LoginViewModel].
 * @param email      The email address displayed for user context.
 * @param onVerified Callback invoked when the account is confirmed verified.
 * @param onBack     Callback to return to the login screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    viewModel: LoginViewModel,
    email: String,
    onVerified: () -> Unit,
    onBack: () -> Unit
) {
    val isLoading by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    val emailVerified by viewModel.emailVerified.observeAsState()

    var visible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Pulsing animation for the email icon while waiting
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val iconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconAlpha"
    )

    // Start polling when the screen enters composition, stop when it leaves.
    DisposableEffect(email) {
        viewModel.startPollingEmailVerification(email)
        onDispose { viewModel.stopPollingEmailVerification() }
    }

    LaunchedEffect(Unit) { visible = true }

    // Auto-redirect as soon as the polling detects verification.
    LaunchedEffect(emailVerified) {
        if (emailVerified == true) {
            viewModel.clearEmailVerified()
            onVerified()
        }
    }

    LaunchedEffect(errorMessage) {
        val msg = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verificar email") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
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
            Spacer(Modifier.height(56.dp))

            // ── Animated email icon ───────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -60 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailUnread,
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = iconAlpha)
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Revisa tu bandeja de entrada",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Hemos enviado un enlace de verificación a:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── Waiting indicator ─────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 200))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Esperando verificación…",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Esta pantalla se actualizará automáticamente en cuanto " +
                                   "hagas clic en el enlace del email.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Instructions card ─────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 350))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "¿Qué hacer?",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "1. Abre la app de email en tu dispositivo.\n" +
                                   "2. Busca un email de PetRadar (revisa también el spam).\n" +
                                   "3. Toca el enlace de verificación que encontrarás en el email.\n" +
                                   "4. ¡Listo! Esta pantalla te redirigirá automáticamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.6
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Manual check button ───────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 450))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { viewModel.checkEmailVerifiedNow(email) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Ya verifiqué mi cuenta", fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(onClick = onBack) {
                        Text(
                            "Volver al inicio de sesión",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
