package com.example.petradar

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.utils.AuthManager
import com.example.petradar.viewmodel.ProfileViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ProfileActivity : AppCompatActivity() {

    private lateinit var viewModel: ProfileViewModel

    // Views
    private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhoneNumber: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var etCity: TextInputEditText
    private lateinit var etCountry: TextInputEditText
    private lateinit var btnSaveProfile: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        setupViews()
        setupToolbar()
        setupViewModel()
        setupListeners()

        // Cargar el perfil del usuario con el userId guardado
        val userId = AuthManager.getUserId(this)
        if (userId == null || userId <= 0) {
            // Si no hay userId, intentar obtenerlo del email
            val email = AuthManager.getUserEmail(this)
            if (email != null) {
                Toast.makeText(
                    this,
                    "Cargando información del perfil...",
                    Toast.LENGTH_SHORT
                ).show()
                // Aquí podrías hacer una búsqueda por email, pero por ahora mostramos un mensaje
                Toast.makeText(
                    this,
                    "Por favor, cierra sesión y vuelve a iniciar para actualizar tu perfil",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Error: No se encontró información de usuario. Por favor, inicia sesión nuevamente.",
                    Toast.LENGTH_LONG
                ).show()
            }
            finish()
        } else {
            viewModel.loadUserProfile(userId)
        }
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        progressBar = findViewById(R.id.progressBar)
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        etAddress = findViewById(R.id.etAddress)
        etCity = findViewById(R.id.etCity)
        etCountry = findViewById(R.id.etCountry)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Mi Perfil"
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        // Observar cambios en el perfil
        viewModel.userProfile.observe(this) { profile ->
            profile?.let {
                etFirstName.setText(it.name)
                etLastName.setText(it.lastName ?: "")
                etEmail.setText(it.email)
                etPhoneNumber.setText(it.phoneNumber ?: "")
                // Nota: Los campos address, city, country no existen en la API de PetRadar
            }
        }

        // Observar estado de carga
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnSaveProfile.isEnabled = !isLoading
            setFieldsEnabled(!isLoading)
        }

        // Observar mensajes de error
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        // Observar éxito en actualización
        viewModel.updateSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Perfil actualizado exitosamente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun saveProfile() {
        val userId = AuthManager.getUserId(this)
        if (userId == null || userId <= 0) {
            Toast.makeText(this, "Error: No se encontró el ID de usuario", Toast.LENGTH_LONG).show()
            return
        }

        val name = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim().ifEmpty { null }
        val phoneNumber = etPhoneNumber.text.toString().trim().ifEmpty { null }

        if (name.isEmpty()) {
            etFirstName.error = "El nombre es requerido"
            return
        }

        viewModel.updateProfile(
            userId = userId,
            name = name,
            lastName = lastName,
            phoneNumber = phoneNumber
        )
    }

    private fun setFieldsEnabled(enabled: Boolean) {
        etFirstName.isEnabled = enabled
        etLastName.isEnabled = enabled
        etPhoneNumber.isEnabled = enabled
        etAddress.isEnabled = enabled
        etCity.isEnabled = enabled
        etCountry.isEnabled = enabled
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
