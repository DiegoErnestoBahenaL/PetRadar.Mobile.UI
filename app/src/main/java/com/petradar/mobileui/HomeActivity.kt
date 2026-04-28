package com.petradar.mobileui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.petradar.mobileui.ui.HomeScreen
import com.petradar.mobileui.ui.theme.PetRadarTheme
import com.petradar.mobileui.utils.AuthManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    // Pull-to-refresh state.
    private var isRefreshing    by mutableStateOf(false)
    // Changing this key forces Coil to refetch the profile picture even when the URL is the same.
    private var imageRefreshKey by mutableLongStateOf(System.currentTimeMillis())

    /**
     * Entry point of the Activity. Called when Android creates the screen.
     *
     * @param savedInstanceState Previous saved state (may be null if first time).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        refreshUserInfo()

        setContent {
            PetRadarTheme {
                HomeScreen(
                    userName        = userName,
                    userEmail       = userEmail,
                    profilePhotoUrl = profilePhotoUrl,
                    isRefreshing    = isRefreshing,
                    imageRefreshKey = imageRefreshKey,
                    onRefresh       = { onRefresh() },

                    onNavigateToProfile      = { startActivity(Intent(this, ProfileActivity::class.java)) },
                    onNavigateToPets         = { startActivity(Intent(this, PetsActivity::class.java)) },
                    onNavigateToAppointments = { startActivity(Intent(this, AppointmentsActivity::class.java)) },
                    onNavigateToAdoptions    = { startActivity(Intent(this, AdoptionAnimalsActivity::class.java)) },
                    onNavigateToReports      = { startActivity(Intent(this, MyReportsActivity::class.java)) },

                    onQuickReportPhotoCaptured = { photoUri ->
                        val intent = Intent(this, StrayReportActivity::class.java).apply {
                            putExtra(StrayReportActivity.EXTRA_USER_ID, AuthManager.getUserId(this@HomeActivity) ?: -1L)
                            putExtra(StrayReportActivity.EXTRA_PHOTO_URI, photoUri)
                        }
                        startActivity(intent)
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

    /**
     * Called every time HomeActivity comes back to the foreground (e.g. after returning
     * from ProfileActivity). Re-reads user info and bumps [imageRefreshKey] so the
     * profile picture is always re-fetched from the server.
     */
    override fun onResume() {
        super.onResume()
        refreshUserInfo()
        imageRefreshKey = System.currentTimeMillis()
    }

    private fun onRefresh() {
        isRefreshing = true
        imageRefreshKey = System.currentTimeMillis()
        refreshUserInfo()
        lifecycleScope.launch {
            delay(600)
            isRefreshing = false
        }
    }

    private fun refreshUserInfo() {
        userName        = AuthManager.getUserName(this)        ?: "User"
        userEmail       = AuthManager.getUserEmail(this)       ?: ""
        profilePhotoUrl = AuthManager.getProfilePictureUrl(this)
    }
}
