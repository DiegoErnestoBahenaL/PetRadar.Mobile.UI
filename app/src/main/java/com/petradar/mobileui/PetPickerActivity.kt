package com.petradar.mobileui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.petradar.mobileui.ui.PetPickerScreen
import com.petradar.mobileui.ui.theme.PetRadarTheme
import com.petradar.mobileui.utils.AuthManager
import com.petradar.mobileui.viewmodel.PetViewModel

/**
 * Shows the user's pet list so they can pick the pet they want to report as lost.
 * After selecting a pet, launches [LostPetReportActivity] with the pet ID.
 * If the report is created successfully (RESULT_OK), this Activity also finishes,
 * returning the user to [MyReportsActivity].
 */
class PetPickerActivity : ComponentActivity() {

    private var userId: Long = -1L

    // Closes this Activity too when the report was saved successfully.
    private val reportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            finish()
        }
        // If result is not OK (user backed out), stay on the picker.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        userId = AuthManager.getUserId(this) ?: -1L
        if (userId <= 0) {
            Toast.makeText(this, "Error: usuario no identificado", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val viewModel = ViewModelProvider(this)[PetViewModel::class.java]
        viewModel.loadPets(userId)

        setContent {
            PetRadarTheme {
                PetPickerScreen(
                    viewModel = viewModel,
                    onPetSelected = { pet ->
                        val intent = Intent(this, LostPetReportActivity::class.java).apply {
                            putExtra(LostPetReportActivity.EXTRA_PET_ID, pet.id)
                            putExtra(LostPetReportActivity.EXTRA_USER_ID, userId)
                        }
                        reportLauncher.launch(intent)
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}
