package com.petradar.mobileui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.petradar.mobileui.ui.AdoptionAnimalDetailScreen
import com.petradar.mobileui.ui.theme.PetRadarTheme
import com.petradar.mobileui.utils.AuthManager
import com.petradar.mobileui.viewmodel.AdoptionAnimalDetailViewModel
import com.petradar.mobileui.viewmodel.AdoptionAnimalListViewModel

class AdoptionAnimalDetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ANIMAL_ID = "extra_animal_id"
    }

    private var animalId = -1L
    private lateinit var detailViewModel: AdoptionAnimalDetailViewModel

    // Reload detail only when the form reports a successful save (RESULT_OK).
    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && animalId > 0) {
            detailViewModel.loadAnimal(animalId)
            detailViewModel.loadAdditionalPhotos(animalId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        animalId = intent.getLongExtra(EXTRA_ANIMAL_ID, -1L)
        val currentUserId = AuthManager.getUserId(this) ?: -1L

        detailViewModel = ViewModelProvider(this)[AdoptionAnimalDetailViewModel::class.java]
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
                    onAdoptAnimal = { animal, request ->
                        if (currentUserId > 0) {
                            listViewModel.submitAdoptionRequest(
                                animalId = animal.id,
                                request = request
                            )
                        }
                    },
                    onApproveRequest = { adopterId ->
                        listViewModel.approveAdoptionRequest(animalId, adopterId)
                        if (animalId > 0) detailViewModel.loadAnimal(animalId)
                    },
                    onChatWithRequester = { userId, userName ->
                        val intent = Intent(this, ChatActivity::class.java).apply {
                            putExtra(ChatActivity.EXTRA_ADOPTION_ANIMAL_ID, animalId)
                            putExtra(ChatActivity.EXTRA_OTHER_USER_ID, userId)
                            putExtra(ChatActivity.EXTRA_ANIMAL_NAME, detailViewModel.animal.value?.name)
                            putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, userName)
                        }
                        startActivity(intent)
                    },
                    onChatWithOwner = {
                        val animal = detailViewModel.animal.value ?: return@AdoptionAnimalDetailScreen
                        val intent = Intent(this, ChatActivity::class.java).apply {
                            putExtra(ChatActivity.EXTRA_ADOPTION_ANIMAL_ID, animalId)
                            putExtra(ChatActivity.EXTRA_OTHER_USER_ID, animal.shelterId)
                            putExtra(ChatActivity.EXTRA_ANIMAL_NAME, animal.name)
                        }
                        startActivity(intent)
                    },
                    onEditAnimal = {
                        val intent = Intent(this, AdoptionAnimalFormActivity::class.java)
                        intent.putExtra(AdoptionAnimalFormActivity.EXTRA_ANIMAL_ID, animalId)
                        editLauncher.launch(intent)
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
