package com.example.petradar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.ui.AdoptionAnimalsScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.utils.AuthManager
import com.example.petradar.viewmodel.AdoptionAnimalListViewModel

/**
 * AdoptionAnimalsActivity displays the list of animals available for adoption
 * and lets the user choose which one to adopt.
 *
 * Flow:
 *  - On launch, loads all adoption animals via GET /api/AdoptionAnimals.
 *  - The user confirms adoption for an available animal.
 *  - The list reloads on resume to reflect status changes.
 *
 * Delegates all UI to [AdoptionAnimalsScreen] (Jetpack Compose).
 */
class AdoptionAnimalsActivity : ComponentActivity() {

    private lateinit var viewModel: AdoptionAnimalListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val currentUserId = AuthManager.getUserId(this) ?: -1L

        viewModel = ViewModelProvider(this)[AdoptionAnimalListViewModel::class.java]
        // Initial load of adoption animals on screen open.
        viewModel.loadAnimals()

        setContent {
            PetRadarTheme {
                AdoptionAnimalsScreen(
                    viewModel = viewModel,
                    currentUserId = currentUserId,
                    onAddAnimal = {
                        val intent = Intent(this, AdoptionAnimalFormActivity::class.java)
                        startActivity(intent)
                    },
                    onAnimalClick = { animal ->
                        val intent = Intent(this, AdoptionAnimalDetailActivity::class.java)
                        intent.putExtra(AdoptionAnimalDetailActivity.EXTRA_ANIMAL_ID, animal.id)
                        startActivity(intent)
                    },
                    onEditAnimal = { animal ->
                        val intent = Intent(this, AdoptionAnimalFormActivity::class.java)
                        intent.putExtra(AdoptionAnimalFormActivity.EXTRA_ANIMAL_ID, animal.id)
                        startActivity(intent)
                    },
                    onDeleteAnimal = { animal ->
                        viewModel.deleteAnimal(animal.id)
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

