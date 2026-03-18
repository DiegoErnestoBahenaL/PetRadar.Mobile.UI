package com.example.petradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.ui.AdoptionAnimalFormScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.utils.AuthManager
import com.example.petradar.viewmodel.AdoptionAnimalDetailViewModel

/**
 * AdoptionAnimalFormActivity is the form for creating or editing an adoption animal.
 *
 * Create mode → [EXTRA_ANIMAL_ID] is not provided (or is -1).
 *               May receive [EXTRA_SHELTER_ID] to associate the animal with a shelter.
 *
 * Edit mode   → [EXTRA_ANIMAL_ID] is provided with an id > 0.
 *               The ViewModel loads the animal data via GET /api/AdoptionAnimals/{id}.
 *
 * On save:
 *  - Create mode → POST /api/AdoptionAnimals
 *  - Edit mode   → PUT  /api/AdoptionAnimals/{id}
 *
 * Delegates all UI to [AdoptionAnimalFormScreen] (Jetpack Compose).
 */
class AdoptionAnimalFormActivity : ComponentActivity() {

    companion object {
        /** Extra holding the adoption animal ID to edit. Absent or -1 = creation mode. */
        const val EXTRA_ANIMAL_ID = "extra_animal_id"
        /** Extra holding the shelter ID for a new animal (creation mode). */
        const val EXTRA_SHELTER_ID = "extra_shelter_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Read parameters from the Intent.
        val animalId = intent.getLongExtra(EXTRA_ANIMAL_ID, -1L)
        // Use the shelter ID from the Intent; fall back to user ID as shelter ID if missing.
        val shelterId = intent.getLongExtra(EXTRA_SHELTER_ID, -1L)
            .let { if (it <= 0) AuthManager.getUserId(this) ?: -1L else it }

        val viewModel = ViewModelProvider(this)[AdoptionAnimalDetailViewModel::class.java]
        // Configure the ViewModel context so it knows whether to create or update.
        viewModel.currentAnimalId = animalId
        viewModel.currentShelterId = shelterId

        // Only in edit mode do we load the existing animal data.
        if (animalId > 0) viewModel.loadAnimal(animalId)

        setContent {
            PetRadarTheme {
                AdoptionAnimalFormScreen(
                    viewModel = viewModel,
                    isEditMode = animalId > 0, // true = edit, false = create
                    onBack = { finish() }
                )
            }
        }
    }
}

