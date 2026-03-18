package com.example.petradar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.ui.LoginScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.utils.AuthManager
import com.example.petradar.viewmodel.LoginViewModel

/**
 * LoginActivity is the sign-in screen of PetRadar.
 *
 * It is the main launcher entry point (declared as LAUNCHER in the Manifest).
 * If the user already has an active session, it redirects directly to [HomeActivity].
 *
 * Flow:
 *  1. On launch, checks whether a saved token exists ([AuthManager.isAuthenticated]).
 *  2. If authenticated → navigates to [HomeActivity] without showing the screen.
 *  3. If not authenticated → shows [LoginScreen] where the user enters credentials.
 *  4. On successful login → [LoginViewModel] saves the token and signals success.
 *  5. On observing `loginSuccess == true` → navigates to [HomeActivity], clearing the back stack.
 *  6. On observing `emailNotVerified` with a non-null email → navigates to
 *     [EmailVerificationActivity] so the user can verify their account.
 *
 * Uses Jetpack Compose for the UI via [LoginScreen].
 */
class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (AuthManager.isAuthenticated(this)) {
            navigateToHome()
            return
        }

        val viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        viewModel.loginSuccess.observe(this) { success ->
            if (success == true) navigateToHome()
        }

        // When the account exists but email is not yet verified, redirect to verification.
        viewModel.emailNotVerified.observe(this) { email ->
            if (email != null) {
                viewModel.clearEmailNotVerified()
                navigateToEmailVerification(email)
            }
        }

        setContent {
            PetRadarTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { navigateToHome() },
                    onNavigateToRegister = {
                        startActivity(Intent(this, RegisterActivity::class.java))
                    }
                )
            }
        }
    }

    private fun navigateToHome() {
        startActivity(
            Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun navigateToEmailVerification(email: String) {
        startActivity(
            Intent(this, EmailVerificationActivity::class.java).apply {
                putExtra(EmailVerificationActivity.EXTRA_EMAIL, email)
            }
        )
    }
}
