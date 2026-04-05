package com.example.petradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.api.ReportCreateModel
import com.example.petradar.ui.LostPetReportScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.utils.AuthManager
import com.example.petradar.viewmodel.LostPetReportViewModel

class LostPetReportActivity : ComponentActivity() {

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

        val viewModel = ViewModelProvider(this)[LostPetReportViewModel::class.java]
        if (petId > 0) viewModel.loadPet(petId)

        setContent {
            PetRadarTheme {
                LostPetReportScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onSubmit = { form, pet ->
                        if (userId <= 0) return@LostPetReportScreen

                        viewModel.createLostReport(
                            ReportCreateModel(
                                userId = userId,
                                userPetId = petId.takeIf { it > 0 },
                                species = pet?.species ?: "Dog",
                                breed = pet?.breed,
                                color = pet?.color,
                                sex = pet?.sex,
                                size = pet?.size,
                                approximateAge = pet?.approximateAge,
                                weight = pet?.weight,
                                description = form.description ?: pet?.description,
                                isNeutered = pet?.isNeutered,
                                reportType = "Lost",
                                reportStatus = "Active",
                                hasCollar = form.hasCollar,
                                hasTag = form.hasTag,
                                incidentDate = form.incidentDateIso,
                                latitude = form.latitude,
                                longitude = form.longitude,
                                addressText = form.addressText,
                                searchRadiusMeters = form.searchRadiusMeters,
                                useAlternateContact = form.useAlternateContact,
                                contactName = form.contactName,
                                contactPhone = form.contactPhone,
                                contactEmail = form.contactEmail,
                                offersReward = form.offersReward,
                                rewardAmount = form.rewardAmount
                            )
                        )
                    }
                )
            }
        }
    }
}

