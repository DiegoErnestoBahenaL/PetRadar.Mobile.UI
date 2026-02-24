package com.example.petradar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.ui.RegisterScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.utils.AuthManager
import com.example.petradar.viewmodel.LoginViewModel

class RegisterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        // Auto-login after register: save token + user info then navigate
        viewModel.loginResult.observe(this) { response ->
            response?.let { AuthManager.saveAuthToken(this, it.token, it.refreshToken) }
        }
        viewModel.registerCredentials.observe(this) { credentials ->
            credentials?.let { (email, password) ->
                viewModel.login(email, password)
                viewModel.clearRegisterCredentials()
            }
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
                RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSuccess = { navigateToHome() },
                    onBack = { finish() }
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
