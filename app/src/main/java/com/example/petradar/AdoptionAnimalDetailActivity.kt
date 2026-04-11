package com.example.petradar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.petradar.ui.AdoptionAnimalDetailScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.utils.AuthManager
import com.example.petradar.viewmodel.AdoptionAnimalDetailViewModel
import com.example.petradar.viewmodel.AdoptionAnimalListViewModel

class AdoptionAnimalDetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ANIMAL_ID = "extra_animal_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val animalId = intent.getLongExtra(EXTRA_ANIMAL_ID, -1L)
        val currentUserId = AuthManager.getUserId(this) ?: -1L

        val detailViewModel = ViewModelProvider(this)[AdoptionAnimalDetailViewModel::class.java]
        detailViewModel.currentAnimalId = animalId
        if (animalId > 0) {
            detailViewModel.loadAnimal(animalId)
            detailViewModel.loadAdditionalPhotos(animalId)
        }

        val listViewModel = ViewModelProvider(this)[AdoptionAnimalListViewModel::class.java]

        setContent {
            PetRadarTheme {
                val adoptSuccess by listViewModel.adoptSuccess.observeAsState(null)
                AdoptionAnimalDetailScreen(
                    viewModel = detailViewModel,
                    currentUserId = currentUserId,
                    adoptSuccess = adoptSuccess,
                    onAdoptAnimal = { animal ->
                        if (currentUserId > 0) {
                            listViewModel.adoptAnimal(animal.id, currentUserId)
                        }
                    },
                    onEditAnimal = {
                        val intent = Intent(this, AdoptionAnimalFormActivity::class.java)
                        intent.putExtra(AdoptionAnimalFormActivity.EXTRA_ANIMAL_ID, animalId)
                        startActivity(intent)
                    },
                    onDeleteAnimal = {
                        listViewModel.deleteAnimal(animalId)
                        finish()
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}
