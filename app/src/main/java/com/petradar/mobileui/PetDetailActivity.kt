package com.petradar.mobileui

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.petradar.mobileui.ui.PetDetailScreen
import com.petradar.mobileui.ui.theme.PetRadarTheme
import com.petradar.mobileui.utils.AuthManager
import com.petradar.mobileui.utils.MedicationScheduler
import com.petradar.mobileui.utils.MedicationStore
import com.petradar.mobileui.utils.PetNameStore
import com.petradar.mobileui.utils.PetPhotoStore
import com.petradar.mobileui.viewmodel.PetDetailViewModel

/**
 * PetDetailActivity serves for both creating a new pet and editing an existing one.
 *
 * Edit mode   → [EXTRA_PET_ID] is provided with an id > 0.
 *               The ViewModel loads the current data via GET /api/UserPets/{id}.
 * Create mode → [EXTRA_USER_ID] is provided (or read from [AuthManager]).
 *               The ViewModel builds a POST /api/UserPets request.
 *
 * Note on photos:
 *  - The selected photo is uploaded to the backend via PUT /api/UserPets/{id}/mainpicture.
 *  - A local copy is also kept in [com.petradar.mobileui.utils.PetPhotoStore]
 *    so the image can still be shown immediately if the API response is delayed.
 *
 * Delegates all UI to [PetDetailScreen] (Jetpack Compose).
 */
class PetDetailActivity : ComponentActivity() {

    companion object {
        /** Extra holding the ID of the pet to edit. If not provided, creation mode is assumed. */
        const val EXTRA_PET_ID = "extra_pet_id"
        /** Extra holding the owner user ID for a new pet (creation mode). */
        const val EXTRA_USER_ID = "extra_user_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Read Intent extras. petId == -1 indicates creation mode.
        val petId = intent.getLongExtra(EXTRA_PET_ID, -1L)
        // Use the userId from the Intent; fall back to the saved session if missing.
        val userId = intent.getLongExtra(EXTRA_USER_ID, -1L)
            .let { if (it <= 0) AuthManager.getUserId(this) ?: -1L else it }

        val viewModel = ViewModelProvider(this)[PetDetailViewModel::class.java]
        // Configure the ViewModel context so it knows whether to create or update.
        viewModel.currentPetId = petId
        viewModel.currentUserId = userId

        // Only in edit mode do we load the existing pet data and additional photos.
        if (petId > 0) {
            viewModel.loadPet(petId)
            viewModel.loadAdditionalPhotos(petId)
        }

        // Observe saveSuccess so we can schedule alarms once we know the pet ID.
        // For new pets the petId is unknown until after the server responds;
        // PetDetailViewModel.currentPetId is -1 on creation — we use the petId
        // from edit mode directly.
        viewModel.saveSuccess.observe(this) { success ->
            if (!success) return@observe
            val savedPetId = viewModel.currentPetId  // valid only in edit mode
            if (savedPetId > 0) {
                val petName = viewModel.pet.value?.name ?: ""
                if (petName.isNotBlank()) PetNameStore.save(this, savedPetId, petName)
                val reminders = MedicationStore.getAll(this, savedPetId)
                MedicationScheduler.cancelAll(this, savedPetId)
                reminders.filter { it.isActive }.forEach { reminder ->
                    MedicationScheduler.schedule(this, savedPetId, petName, reminder)
                }
            }
        }

        // After creation the API returns no body, so we resolve the new pet's ID
        // by re-fetching the list. Once we have it, persist the pending photo.
        viewModel.createdPetId.observe(this) { newPetId ->
            val uri = viewModel.pendingPhotoUri
            if (newPetId != null && newPetId > 0 && !uri.isNullOrBlank()) {
                PetPhotoStore.save(this, newPetId, uri)
            }
        }

        viewModel.deleteSuccess.observe(this) { deleted ->
            if (!deleted) return@observe
            if (petId > 0) {
                // Cancel all medication alarms for this pet
                MedicationScheduler.cancelAll(this, petId)
                // Remove all local data for this pet
                MedicationStore.deleteAll(this, petId)
                PetPhotoStore.delete(this, petId)
                PetNameStore.remove(this, petId)
            }
            setResult(RESULT_OK)
            finish()
        }

        setContent {
            PetRadarTheme {
                PetDetailScreen(
                    viewModel = viewModel,
                    isEditMode = petId > 0,
                    onBack = { finish() },
                    onDelete = { viewModel.deletePet() },
                    onReportLost = {
                        if (petId > 0) {
                            val intent = Intent(this, LostPetReportActivity::class.java).apply {
                                putExtra(LostPetReportActivity.EXTRA_PET_ID, petId)
                                putExtra(LostPetReportActivity.EXTRA_USER_ID, userId)
                            }
                            startActivity(intent)
                        }
                    },
                    onSaveMedications = { reminders ->
                        if (petId > 0) {
                            val petName = viewModel.pet.value?.name ?: ""
                            if (petName.isNotBlank()) PetNameStore.save(this, petId, petName)
                            MedicationStore.saveAll(this, petId, reminders)
                            MedicationScheduler.cancelAll(this, petId)
                            reminders.filter { it.isActive }.forEach { reminder ->
                                MedicationScheduler.schedule(this, petId, petName, reminder)
                            }
                        }
                    }
                )
            }
        }
    }
}
