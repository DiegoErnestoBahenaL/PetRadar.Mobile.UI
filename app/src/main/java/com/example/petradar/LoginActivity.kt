package com.example.petradar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.ui.LoginScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.utils.AuthManager
import com.example.petradar.viewmodel.LoginViewModel

/**
 * LoginActivity is the sign-in screen of PetRadar.
 *
 * It is the main launcher entry point (declared as LAUNCHER in the Manifest).
 * On first launch (or when permissions are not yet granted), it requests all runtime
 * permissions before showing any UI. Once permissions are handled (granted or denied),
 * it proceeds with the normal authentication flow.
 *
 * Flow:
 *  1. On launch, requests any missing runtime permissions.
 *  2. After permissions are handled → checks [AuthManager.isAuthenticated].
 *  3. If authenticated → navigates to [HomeActivity] without showing the screen.
 *  4. If not authenticated → shows [LoginScreen] where the user enters credentials.
 *  5. On successful login → [LoginViewModel] saves the token and signals success.
 *  6. On observing `loginSuccess == true` → navigates to [HomeActivity], clearing the back stack.
 *  7. On observing `emailNotVerified` with a non-null email → navigates to
 *     [EmailVerificationActivity] so the user can verify their account.
 *
 * Uses Jetpack Compose for the UI via [LoginScreen].
 */
class LoginActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Proceed regardless of which permissions were granted or denied.
        // Individual screens guard their features with checkSelfPermission.
        continueAppFlow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val missing = runtimePermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            continueAppFlow()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    /** Returns the list of runtime permissions the app needs, adjusted for the current API level. */
    private fun runtimePermissions(): List<String> = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            @Suppress("DEPRECATION")
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun continueAppFlow() {
        if (AuthManager.isAuthenticated(this)) {
            navigateToHome()
            return
        }

        val viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        viewModel.loginSuccess.observe(this) { success ->
            if (success == true) navigateToHome()
        }

        // When the account exists but email is not yet verified, redirect to verification.
        viewModel.emailNotVerified.observe(this) { email ->
            if (email != null) {
                viewModel.clearEmailNotVerified()
                navigateToEmailVerification(email)
            }
        }

        setContent {
            PetRadarTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { navigateToHome() },
                    onNavigateToRegister = {
                        startActivity(Intent(this, RegisterActivity::class.java))
                    }
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

    private fun navigateToEmailVerification(email: String) {
        startActivity(
            Intent(this, EmailVerificationActivity::class.java).apply {
                putExtra(EmailVerificationActivity.EXTRA_EMAIL, email)
            }
        )
    }
}
