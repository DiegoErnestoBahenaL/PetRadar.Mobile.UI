package com.petradar.mobileui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.petradar.mobileui.ui.EmailVerificationScreen
import com.petradar.mobileui.ui.theme.PetRadarTheme
import com.petradar.mobileui.utils.AuthManager
import com.petradar.mobileui.viewmodel.LoginViewModel

/**
 * Activity that hosts the email verification waiting screen.
 *
 * Shown after registration or after a login attempt on an unverified account.
 * The screen polls the API every 5 s and auto-redirects to [LoginActivity] once
 * [LoginViewModel.emailVerified] becomes `true`.
 *
 * Extras:
 *  - [EXTRA_EMAIL] (String) – the email address to display and poll for. Optional.
 */
class EmailVerificationActivity : ComponentActivity() {

    companion object {
        const val EXTRA_EMAIL = "extra_email"
    }

    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val email = intent.getStringExtra(EXTRA_EMAIL) ?: ""
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        setContent {
            PetRadarTheme {
                EmailVerificationScreen(
                    viewModel = viewModel,
                    email = email,
                    onVerified = { navigateToHome() },
                    onBack = {
                        // User cancelled verification — clear the token so LoginActivity
                        // doesn't treat the unverified session as active.
                        AuthManager.saveAuthToken(this, null, null)
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopPollingEmailVerification()
    }

    /**
     * Email verified — go directly to Home.
     * The token saved during the silent post-registration login is still valid
     * (the account is now verified), so no re-login is needed.
     */
    private fun navigateToHome() {
        startActivity(
            Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}

