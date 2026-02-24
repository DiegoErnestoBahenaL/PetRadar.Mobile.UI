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

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Already logged in → go straight to Home
        if (AuthManager.isAuthenticated(this)) {
            navigateToHome()
            return
        }

        val viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        // Observe profile once token is retrieved and save user info
        viewModel.loginResult.observe(this) { response ->
            response?.let { AuthManager.saveAuthToken(this, it.token, it.refreshToken) }
        }
        viewModel.userProfile.observe(this) { profile ->
            profile?.let {
                val fullName = "${it.name} ${it.lastName ?: ""}".trim()
                AuthManager.saveUserInfo(this, it.id ?: 0L, it.email, fullName)
                navigateToHome()
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
}
