package com.petradar.mobileui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.petradar.mobileui.ui.RegisterScreen
import com.petradar.mobileui.ui.theme.PetRadarTheme
import com.petradar.mobileui.utils.AuthManager
import com.petradar.mobileui.viewmodel.LoginViewModel
import com.petradar.mobileui.viewmodel.ProfileViewModel

/**
 * RegisterActivity handles the account-creation flow in PetRadar.
 *
 * Flow:
 *  1. [LoginViewModel.register] calls POST /api/Users (201 Created).
 *  2. Credentials are stored in [LoginViewModel.registerCredentials].
 *  3. This Activity performs a silent login with those credentials so that the JWT
 *     token and userId are saved to [com.petradar.mobileui.utils.AuthManager].
 *  4. [LoginViewModel.login] → fetchUserIdByEmail detects emailVerified=false
 *     and emits [LoginViewModel.emailNotVerified].
 *  5. This Activity navigates to [EmailVerificationActivity].
 *     The polling there can now call GET /api/Users/{id} with the saved token.
 */
class RegisterActivity : ComponentActivity() {

    /** URI of the photo selected in the form (may be null if skipped). */
    private var pendingPhotoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        val profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        // After registration, silently log in to obtain JWT and userId.
        viewModel.registerCredentials.observe(this) { credentials ->
            credentials?.let { (email, password) ->
                viewModel.clearRegisterCredentials()
                viewModel.login(email, password)
            }
        }

        // Normal post-registration path: email not yet verified → upload photo then go to verification.
        viewModel.emailNotVerified.observe(this) { email ->
            if (email != null) {
                viewModel.clearEmailNotVerified()
                uploadPendingPhotoThenNavigate(profileViewModel) {
                    navigateToEmailVerification(email)
                }
            }
        }

        // Edge case: account already verified (admin-created) → upload photo then go to Home.
        viewModel.loginSuccess.observe(this) { success ->
            if (success == true) {
                uploadPendingPhotoThenNavigate(profileViewModel) {
                    navigateToHome()
                }
            }
        }

        setContent {
            PetRadarTheme {
                RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSuccess = { /* handled via observers */ },
                    onBack = { finish() },
                    onPhotoSelected = { uri -> pendingPhotoUri = uri }
                )
            }
        }
    }

    /**
     * If the user selected a photo, uploads it using [ProfileViewModel] and then
     * calls [onDone]. If no photo was selected, calls [onDone] immediately.
     */
    private fun uploadPendingPhotoThenNavigate(
        profileViewModel: ProfileViewModel,
        onDone: () -> Unit
    ) {
        val uri = pendingPhotoUri
        val userId = AuthManager.getUserId(this)
        if (uri != null && userId != null && userId > 0L) {
            // Upload runs in a coroutine inside the ViewModel; observe completion.
            profileViewModel.photoUploadSuccess.observe(this) { result ->
                if (result != null) {
                    profileViewModel.clearPhotoUploadSuccess()
                    onDone()
                }
            }
            profileViewModel.uploadProfilePicture(userId, uri, this)
        } else {
            onDone()
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

    private fun navigateToEmailVerification(email: String) {
        startActivity(
            Intent(this, EmailVerificationActivity::class.java).apply {
                putExtra(EmailVerificationActivity.EXTRA_EMAIL, email)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        )
        finish()
    }
}
