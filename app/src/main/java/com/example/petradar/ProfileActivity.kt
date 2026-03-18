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

/**
 * ProfileActivity displays and allows editing of the authenticated user's profile.
 *
 * On start, it reads the userId from [AuthManager] and asks [ProfileViewModel]
 * to load the data from GET /api/Users/{id}.
 *
 * If the userId is unavailable (corrupted or expired session), it shows an error
 * Toast and closes safely.
 *
 * Delegates all UI to [ProfileScreen] (Jetpack Compose).
 */
class ProfileActivity : ComponentActivity() {

    private lateinit var viewModel: ProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Retrieve the user ID from the local session.
        // If null or invalid, the app cannot display the profile.
        val userId = AuthManager.getUserId(this)
        if (userId == null || userId <= 0) {
            Toast.makeText(this, "Error: please sign in again", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        // Load the user profile from the API on launch.
        viewModel.loadUserProfile(userId)

        // When the profile is loaded or refreshed, persist the latest photo URL locally
        // so HomeActivity can show it in the drawer without an extra API call.
        viewModel.userProfile.observe(this) { profile ->
            profile?.let { AuthManager.saveProfilePhotoURL(this, it.profilePhotoURL) }
        }

        setContent {
            PetRadarTheme {
                ProfileScreen(
                    viewModel = viewModel,
                    userId = userId,
                    onBack = { finish() } // Returns to HomeActivity
                )
            }
        }
    }
}
