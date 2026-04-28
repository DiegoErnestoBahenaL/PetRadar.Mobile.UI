package com.petradar.mobileui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.petradar.mobileui.ui.PetsScreen
import com.petradar.mobileui.ui.theme.PetRadarTheme
import com.petradar.mobileui.utils.AuthManager
import com.petradar.mobileui.viewmodel.PetViewModel

/**
 * PetsActivity displays the list of the user's registered pets
 * and allows navigation to create or edit each one.
 *
 * Flow:
 *  - On launch, loads the user's pets via GET /api/UserPets/user/{userId}.
 *  - Pressing "+" navigates to [PetDetailActivity] in creation mode, passing the userId.
 *  - Tapping a pet navigates to [PetDetailActivity] in edit mode, passing the petId.
 *  - On returning from [PetDetailActivity] (onResume), reloads the list automatically
 *    to reflect any changes (creation, edit or deletion).
 *
 * Delegates all UI to [PetsScreen] (Jetpack Compose).
 */
class PetsActivity : ComponentActivity() {

    private lateinit var viewModel: PetViewModel
    /** ID of the signed-in user; used to load/reload their pets. */
    private var userId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Retrieve the user ID from the local session.
        userId = AuthManager.getUserId(this) ?: -1L
        if (userId <= 0) {
            Toast.makeText(this, "Error: user not identified", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[PetViewModel::class.java]
        // Initial load of pets on screen open.
        viewModel.loadPets(userId)

        setContent {
            PetRadarTheme {
                PetsScreen(
                    viewModel = viewModel,
                    userId = userId,
                    // Creation mode: PetDetailActivity needs the userId to associate the pet.
                    onAddPet = {
                        val intent = Intent(this, PetDetailActivity::class.java).apply {
                            putExtra(PetDetailActivity.EXTRA_USER_ID, userId)
                        }
                        startActivity(intent)
                    },
                    // Edit mode: PetDetailActivity receives the ID of the pet to edit.
                    onEditPet = { pet ->
                        val intent = Intent(this, PetDetailActivity::class.java).apply {
                            putExtra(PetDetailActivity.EXTRA_PET_ID, pet.id)
                        }
                        startActivity(intent)
                    },
                    onReportLost = { pet ->
                        val intent = Intent(this, LostPetReportActivity::class.java).apply {
                            putExtra(LostPetReportActivity.EXTRA_PET_ID, pet.id)
                            putExtra(LostPetReportActivity.EXTRA_USER_ID, userId)
                        }
                        startActivity(intent)
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    /**
     * Called every time this Activity returns to the foreground (e.g. after returning
     * from [PetDetailActivity]). Reloads the list to show recent changes.
     */
    override fun onResume() {
        super.onResume()
        viewModel.loadPets(userId)
    }
}
