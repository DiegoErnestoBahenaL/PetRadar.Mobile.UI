package com.example.petradar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.petradar.ui.HomeScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.utils.AuthManager

class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            PetRadarTheme {
                HomeScreen(
                    userName = AuthManager.getUserName(this) ?: "",
                    userEmail = AuthManager.getUserEmail(this) ?: "",
                    onNavigateToProfile = {
                        startActivity(Intent(this, ProfileActivity::class.java))
                    },
                    onNavigateToPets = {
                        startActivity(Intent(this, PetsActivity::class.java))
                    },
                    onNavigateToAppointments = {
                        startActivity(Intent(this, AppointmentsActivity::class.java))
                    },
                    onLogout = {
                        AuthManager.logout(this)
                        val intent = Intent(this, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}
