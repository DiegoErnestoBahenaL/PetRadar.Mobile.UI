package com.example.petradar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.ui.RegisterScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.viewmodel.LoginViewModel

class RegisterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        // Auto-login after register: login() now saves token internally
        viewModel.registerCredentials.observe(this) { credentials ->
            credentials?.let { (email, password) ->
                viewModel.login(email, password)
                viewModel.clearRegisterCredentials()
            }
        }
        // Navigate to home as soon as login succeeds
        viewModel.loginSuccess.observe(this) { success ->
            if (success == true) navigateToHome()
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
