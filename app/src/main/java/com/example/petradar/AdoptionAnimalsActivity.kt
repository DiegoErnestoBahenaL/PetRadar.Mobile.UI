package com.example.petradar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.ui.AdoptionAnimalsScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.viewmodel.AdoptionAnimalListViewModel

/**
 * AdoptionAnimalsActivity displays the list of animals available for adoption
 * and allows navigation to create or edit each one.
 *
 * Flow:
 *  - On launch, loads all adoption animals via GET /api/AdoptionAnimals.
 *  - Pressing "+" navigates to [AdoptionAnimalFormActivity] in creation mode.
 *  - Tapping an animal navigates to [AdoptionAnimalFormActivity] in edit mode,
 *    passing the animalId.
 *  - On returning from [AdoptionAnimalFormActivity] (onResume), reloads the list
 *    automatically to reflect any changes (creation, edit or deletion).
 *
 * Delegates all UI to [AdoptionAnimalsScreen] (Jetpack Compose).
 */
class AdoptionAnimalsActivity : ComponentActivity() {

    private lateinit var viewModel: AdoptionAnimalListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[AdoptionAnimalListViewModel::class.java]
        // Initial load of adoption animals on screen open.
        viewModel.loadAnimals()

        setContent {
            PetRadarTheme {
                AdoptionAnimalsScreen(
                    viewModel = viewModel,
                    // Creation mode: navigate to form with no animal ID.
                    onAddAnimal = {
                        val intent = Intent(this, AdoptionAnimalFormActivity::class.java)
                        startActivity(intent)
                    },
                    // Edit mode: pass the animal ID to the form.
                    onEditAnimal = { animal ->
                        val intent = Intent(this, AdoptionAnimalFormActivity::class.java).apply {
                            putExtra(AdoptionAnimalFormActivity.EXTRA_ANIMAL_ID, animal.id)
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
     * from [AdoptionAnimalFormActivity]). Reloads the list to show recent changes.
     */
    override fun onResume() {
        super.onResume()
        viewModel.loadAnimals()
    }
}

