package com.example.petradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.ui.PetDetailScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.utils.AuthManager
import com.example.petradar.viewmodel.PetDetailViewModel

class PetDetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PET_ID = "extra_pet_id"
        const val EXTRA_USER_ID = "extra_user_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val petId = intent.getLongExtra(EXTRA_PET_ID, -1L)
        val userId = intent.getLongExtra(EXTRA_USER_ID, -1L)
            .let { if (it <= 0) AuthManager.getUserId(this) ?: -1L else it }

        val viewModel = ViewModelProvider(this)[PetDetailViewModel::class.java]
        viewModel.currentPetId = petId
        viewModel.currentUserId = userId

        if (petId > 0) viewModel.loadPet(petId)

        // For NEW pets: once the pet is created the API returns 201 with no body,
        // so we can't get the new ID here. The photo is already handled in the
        // screen's save button for edit mode (currentPetId > 0).
        // For creates, we observe the pet list in PetsActivity.onResume and match by name.
        // As a simpler approach: store a "pending" URI tagged to userId+name in PetPhotoStore
        // after success — handled inside PetDetailScreen's save action for the edit case.
        // For new pets the photo will be re-associated when the user next edits that pet.

        setContent {
            PetRadarTheme {
                PetDetailScreen(
                    viewModel = viewModel,
                    isEditMode = petId > 0,
                    onBack = { finish() }
                )
            }
        }
    }
}
