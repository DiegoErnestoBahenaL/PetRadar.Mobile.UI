package com.example.petradar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.petradar.ui.HomeScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.utils.AuthManager

/**
 * HomeActivity is the main screen of the application, reached after a successful login.
 *
 * From here the user can navigate to the main sections:
 *  - My Profile    → [ProfileActivity]
 *  - My Pets       → [PetsActivity]
 *  - Appointments  → [AppointmentsActivity]
 *  - Sign out      → returns to [LoginActivity]
 *
 * Uses Jetpack Compose to render the UI via [HomeScreen].
 */
class HomeActivity : ComponentActivity() {

    // Mutable state so Compose reacts when values change in onResume.
    private var userName        by mutableStateOf("User")
    private var userEmail       by mutableStateOf("")
    private var profilePhotoUrl by mutableStateOf<String?>(null)

    /**
     * Entry point of the Activity. Called when Android creates the screen.
     *
     * @param savedInstanceState Previous saved state (may be null if first time).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // Enables edge-to-edge mode so that content occupies the entire screen,
        // including the notification bar and navigation bar areas.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        refreshUserInfo()

        // Sets the screen content using Jetpack Compose.
        setContent {
            // Applies the visual theme of PetRadar (colors, typography, shapes).
            PetRadarTheme {
                // Main composable of the home screen.
                HomeScreen(
                    userName  = userName,
                    userEmail = userEmail,
                    profilePhotoUrl = profilePhotoUrl,

                    // Navigates to the user's profile screen.
                    onNavigateToProfile      = { startActivity(Intent(this, ProfileActivity::class.java)) },

                    // Navigates to the user's pets screen.
                    onNavigateToPets         = { startActivity(Intent(this, PetsActivity::class.java)) },

                    // Navigates to the veterinary appointments screen.
                    onNavigateToAppointments = { startActivity(Intent(this, AppointmentsActivity::class.java)) },

                    // Navigates to the adoption animals screen.
                    onNavigateToAdoptions    = { startActivity(Intent(this, AdoptionAnimalsActivity::class.java)) },

                    // Navigates to the user's reports screen.
                    onNavigateToReports      = { startActivity(Intent(this, MyReportsActivity::class.java)) },

                    // Logs out the user:
                    // 1. Clears the locally saved session data.
                    // 2. Redirects to LoginActivity, clearing the navigation history,
                    //    so the user cannot go back with the "Back" button.
                    onLogout = {
                        AuthManager.logout(this)
                        val intent = Intent(this, LoginActivity::class.java).apply {
                            // FLAG_ACTIVITY_NEW_TASK + FLAG_ACTIVITY_CLEAR_TASK clear
                            // the back stack, leaving LoginActivity as the only active screen.
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }

    /**
     * Called every time HomeActivity comes back to the foreground (e.g. after returning
     * from ProfileActivity). Re-reads name and email from AuthManager so any profile
     * changes are reflected instantly without requiring a re-login.
     */
    override fun onResume() {
        super.onResume()
        refreshUserInfo()
    }

    private fun refreshUserInfo() {
        userName        = AuthManager.getUserName(this)         ?: "User"
        userEmail       = AuthManager.getUserEmail(this)        ?: ""
        profilePhotoUrl = AuthManager.getProfilePictureUrl(this)
    }
}
