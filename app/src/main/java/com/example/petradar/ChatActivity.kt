package com.example.petradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.ui.ChatScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.utils.AuthManager
import com.example.petradar.viewmodel.ChatViewModel

class ChatActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ADOPTION_ANIMAL_ID = "extra_adoption_animal_id"
        const val EXTRA_OTHER_USER_ID = "extra_other_user_id"
        const val EXTRA_ANIMAL_NAME = "extra_animal_name"
        const val EXTRA_OTHER_USER_NAME = "extra_other_user_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val adoptionAnimalId = intent.getLongExtra(EXTRA_ADOPTION_ANIMAL_ID, -1L)
        val otherUserId = intent.getLongExtra(EXTRA_OTHER_USER_ID, -1L)
        val animalName = intent.getStringExtra(EXTRA_ANIMAL_NAME)
        val otherUserName = intent.getStringExtra(EXTRA_OTHER_USER_NAME)
        val currentUserId = AuthManager.getUserId(this) ?: -1L

        val viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        setContent {
            PetRadarTheme {
                ChatScreen(
                    viewModel = viewModel,
                    currentUserId = currentUserId,
                    otherUserId = otherUserId,
                    adoptionAnimalId = adoptionAnimalId,
                    animalName = animalName,
                    otherUserName = otherUserName,
                    onBack = { finish() }
                )
            }
        }
    }
}
