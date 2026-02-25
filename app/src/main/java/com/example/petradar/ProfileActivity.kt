package com.example.petradar

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.ui.ProfileScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.utils.AuthManager
import com.example.petradar.viewmodel.ProfileViewModel

class ProfileActivity : ComponentActivity() {

    private lateinit var viewModel: ProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val userId = AuthManager.getUserId(this)
        if (userId == null || userId <= 0) {
            Toast.makeText(this, "Error: inicia sesión nuevamente", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        viewModel.loadUserProfile(userId)

        setContent {
            PetRadarTheme {
                ProfileScreen(
                    viewModel = viewModel,
                    userId = userId,
                    onBack = { finish() }
                )
            }
        }
    }
}
