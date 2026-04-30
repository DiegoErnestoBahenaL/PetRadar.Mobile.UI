package com.petradar.mobileui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.petradar.mobileui.ui.ChatScreen
import com.petradar.mobileui.ui.theme.PetRadarTheme
import com.petradar.mobileui.utils.AuthManager
import com.petradar.mobileui.viewmodel.ChatViewModel

class ChatActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ADOPTION_ANIMAL_ID = "extra_adoption_animal_id"
        const val EXTRA_MATCH_ID = "extra_match_id"
        const val EXTRA_OTHER_USER_ID = "extra_other_user_id"
        const val EXTRA_ANIMAL_NAME = "extra_animal_name"
        const val EXTRA_OTHER_USER_NAME = "extra_other_user_name"
        const val EXTRA_LOST_REPORT_ID = "extra_lost_report_id"
        const val EXTRA_LOST_PET_LABEL = "extra_lost_pet_label"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val adoptionAnimalId = intent.getLongExtra(EXTRA_ADOPTION_ANIMAL_ID, -1L)
        val matchId = intent.getLongExtra(EXTRA_MATCH_ID, -1L)
        val otherUserId = intent.getLongExtra(EXTRA_OTHER_USER_ID, -1L)
        val animalName = intent.getStringExtra(EXTRA_ANIMAL_NAME)
        val otherUserName = intent.getStringExtra(EXTRA_OTHER_USER_NAME)
        val lostReportId = intent.getLongExtra(EXTRA_LOST_REPORT_ID, -1L)
        val lostPetLabel = intent.getStringExtra(EXTRA_LOST_PET_LABEL)
        val currentUserId = AuthManager.getUserId(this) ?: -1L

        val viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        setContent {
            PetRadarTheme {
                ChatScreen(
                    viewModel = viewModel,
                    currentUserId = currentUserId,
                    otherUserId = otherUserId,
                    adoptionAnimalId = if (adoptionAnimalId > 0) adoptionAnimalId else null,
                    matchId = if (matchId > 0) matchId else null,
                    lostReportId = if (lostReportId > 0) lostReportId else null,
                    lostPetLabel = lostPetLabel,
                    title = buildString {
                        if (!animalName.isNullOrBlank()) append(animalName)
                        if (!otherUserName.isNullOrBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(otherUserName)
                        }
                        if (isEmpty()) append("Chat")
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}
