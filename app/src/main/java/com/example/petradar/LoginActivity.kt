package com.example.petradar

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.utils.AuthManager
import com.example.petradar.viewmodel.LoginViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: LoginViewModel

    // Views
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoToRegister: MaterialButton
    private lateinit var tvForgotPassword: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Verificar si ya está autenticado
            if (AuthManager.isAuthenticated(this)) {
                navigateToHome()
                return
            }

            setContentView(R.layout.activity_login)

            setupViews()
            setupViewModel()
            setupListeners()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al iniciar: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupViews() {
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etEmail = findViewById(R.id.etLoginEmail)
        etPassword = findViewById(R.id.etLoginPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoToRegister = findViewById(R.id.btnGoToRegister)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        // Observar resultado del login
        viewModel.loginResult.observe(this) { response ->
            response?.let {
                // Guardar token
                AuthManager.saveAuthToken(this, it.token, it.refreshToken)

                Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
            }
        }

        // Observar perfil de usuario (después del login)
        viewModel.userProfile.observe(this) { userProfile ->
            userProfile?.let {
                // Guardar información completa del usuario
                val fullName = "${it.name} ${it.lastName ?: ""}".trim()
                AuthManager.saveUserInfo(this, it.id ?: 0L, it.email, fullName)

                // Navegar a HomeActivity después de guardar todo
                navigateToHome()
            }
        }

        // Observar estado de carga
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnLogin.isEnabled = !isLoading
            btnGoToRegister.isEnabled = !isLoading
            etEmail.isEnabled = !isLoading
            etPassword.isEnabled = !isLoading
        }

        // Observar mensajes de error
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            attemptLogin()
        }

        btnGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Funcionalidad próximamente", Toast.LENGTH_SHORT).show()
        }

        // Limpiar errores al escribir
        etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tilEmail.error = null
            }
        }

        etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tilPassword.error = null
            }
        }
    }

    private fun attemptLogin() {
        // Limpiar errores previos
        tilEmail.error = null
        tilPassword.error = null

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        // Validar email
        if (email.isEmpty()) {
            tilEmail.error = "El email es requerido"
            etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Email inválido"
            etEmail.requestFocus()
            return
        }

        // Validar contraseña
        if (password.isEmpty()) {
            tilPassword.error = "La contraseña es requerida"
            etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            tilPassword.error = "La contraseña debe tener al menos 6 caracteres"
            etPassword.requestFocus()
            return
        }

        // Realizar login usando email como username
        viewModel.login(email, password)
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // No permitir volver atrás desde el login
        @Suppress("DEPRECATION")
        super.onBackPressed()
        finishAffinity()
    }
}
