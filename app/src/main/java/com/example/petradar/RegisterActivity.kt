package com.example.petradar

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.utils.AuthManager
import com.example.petradar.viewmodel.LoginViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {

    private lateinit var viewModel: LoginViewModel

    // Views
    private lateinit var toolbar: Toolbar
    private lateinit var tilFirstName: TextInputLayout
    private lateinit var tilLastName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var cbAcceptPrivacy: CheckBox
    private lateinit var tvOpenPrivacyNotice: TextView
    private lateinit var tvRegisterError: TextView
    private lateinit var btnRegister: MaterialButton
    private lateinit var tvGoToLogin: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        setupViews()
        setupToolbar()
        setupViewModel()
        setupListeners()
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        tilFirstName = findViewById(R.id.tilFirstName)
        tilLastName = findViewById(R.id.tilLastName)
        tilEmail = findViewById(R.id.tilEmail)
        tilPhone = findViewById(R.id.tilPhone)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        cbAcceptPrivacy = findViewById(R.id.cbAcceptPrivacy)
        tvOpenPrivacyNotice = findViewById(R.id.tvOpenPrivacyNotice)
        tvRegisterError = findViewById(R.id.tvRegisterError)
        btnRegister = findViewById(R.id.btnRegister)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Registro"
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        // Observar éxito del registro
        viewModel.registerSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(
                    this,
                    "¡Cuenta creada exitosamente! Iniciando sesión...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Observar credenciales para login automático
        viewModel.registerCredentials.observe(this) { credentials ->
            credentials?.let { (email, password) ->
                // Hacer login automático después del registro
                viewModel.login(email, password)
                viewModel.clearRegisterCredentials()
            }
        }

        // Observar resultado del login automático
        viewModel.loginResult.observe(this) { response ->
            response?.let {
                // Guardar token
                AuthManager.saveAuthToken(this, it.token, it.refreshToken)

                Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
            }
        }

        // Observar perfil de usuario (después del login automático)
        viewModel.userProfile.observe(this) { userProfile ->
            userProfile?.let {
                // Guardar info completa del usuario
                val fullName = "${it.name} ${it.lastName ?: ""}".trim()
                AuthManager.saveUserInfo(this, it.id ?: 0L, it.email, fullName)

                // Navegar a HomeActivity después de guardar todo
                navigateToHome()
            }
        }

        // Observar estado de carga
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnRegister.isEnabled = !isLoading
            setFieldsEnabled(!isLoading)
        }

        // Observar mensajes de error
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                tvRegisterError.text = it
                tvRegisterError.visibility = View.VISIBLE
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupListeners() {
        btnRegister.setOnClickListener {
            attemptRegister()
        }

        tvGoToLogin.setOnClickListener {
            finish() // Volver al login
        }

        tvOpenPrivacyNotice.setOnClickListener {
            // Aquí podrías abrir un diálogo o actividad con el aviso de privacidad
            Toast.makeText(this, "Aviso de privacidad - Próximamente", Toast.LENGTH_SHORT).show()
        }

        // Limpiar errores al enfocar
        setupFocusListeners()
    }

    private fun setupFocusListeners() {
        etFirstName.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tilFirstName.error = null
                tvRegisterError.visibility = View.GONE
            }
        }
        etLastName.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tilLastName.error = null
                tvRegisterError.visibility = View.GONE
            }
        }
        etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tilEmail.error = null
                tvRegisterError.visibility = View.GONE
            }
        }
        etPhone.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tilPhone.error = null
                tvRegisterError.visibility = View.GONE
            }
        }
        etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tilPassword.error = null
                tvRegisterError.visibility = View.GONE
            }
        }
        etConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tilConfirmPassword.error = null
                tvRegisterError.visibility = View.GONE
            }
        }
    }

    private fun attemptRegister() {
        // Limpiar errores previos
        clearAllErrors()

        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim().ifEmpty { null }
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        // Validar nombre
        if (firstName.isEmpty()) {
            tilFirstName.error = "El nombre es requerido"
            etFirstName.requestFocus()
            showGeneralError()
            return
        }

        // Validar apellido
        if (lastName.isEmpty()) {
            tilLastName.error = "El apellido es requerido"
            etLastName.requestFocus()
            showGeneralError()
            return
        }

        // Validar email
        if (email.isEmpty()) {
            tilEmail.error = "El email es requerido"
            etEmail.requestFocus()
            showGeneralError()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Email inválido"
            etEmail.requestFocus()
            showGeneralError()
            return
        }

        // Validar contraseña
        if (password.isEmpty()) {
            tilPassword.error = "La contraseña es requerida"
            etPassword.requestFocus()
            showGeneralError()
            return
        }

        if (password.length < 6) {
            tilPassword.error = "La contraseña debe tener al menos 6 caracteres"
            etPassword.requestFocus()
            showGeneralError()
            return
        }

        // Validar confirmación de contraseña
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.error = "Confirma tu contraseña"
            etConfirmPassword.requestFocus()
            showGeneralError()
            return
        }

        if (password != confirmPassword) {
            tilConfirmPassword.error = "Las contraseñas no coinciden"
            etConfirmPassword.requestFocus()
            showGeneralError()
            return
        }

        // Validar checkbox de privacidad
        if (!cbAcceptPrivacy.isChecked) {
            tvRegisterError.text = "Debes aceptar el aviso de privacidad para continuar"
            tvRegisterError.visibility = View.VISIBLE
            Toast.makeText(this, "Debes aceptar el aviso de privacidad", Toast.LENGTH_SHORT).show()
            return
        }

        // Realizar registro
        viewModel.register(firstName, lastName, email, password, phone)
    }

    private fun clearAllErrors() {
        tilFirstName.error = null
        tilLastName.error = null
        tilEmail.error = null
        tilPhone.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null
        tvRegisterError.visibility = View.GONE
    }

    private fun showGeneralError() {
        tvRegisterError.text = "Corrige los campos marcados antes de continuar."
        tvRegisterError.visibility = View.VISIBLE
    }

    private fun setFieldsEnabled(enabled: Boolean) {
        etFirstName.isEnabled = enabled
        etLastName.isEnabled = enabled
        etEmail.isEnabled = enabled
        etPhone.isEnabled = enabled
        etPassword.isEnabled = enabled
        etConfirmPassword.isEnabled = enabled
        cbAcceptPrivacy.isEnabled = enabled
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

