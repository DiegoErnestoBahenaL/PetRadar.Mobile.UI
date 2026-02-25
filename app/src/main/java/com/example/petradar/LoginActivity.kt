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

        // Navigate to home as soon as login succeeds (token already saved in ViewModel)
        viewModel.loginSuccess.observe(this) { success ->
            if (success == true) navigateToHome()
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
