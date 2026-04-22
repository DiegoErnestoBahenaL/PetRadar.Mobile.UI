package com.example.petradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.api.ReportCreateModel
import com.example.petradar.ui.StrayReportScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.utils.AuthManager
import com.example.petradar.viewmodel.StrayReportViewModel

class StrayReportActivity : ComponentActivity() {

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_PHOTO_URI = "extra_photo_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val userId = intent.getLongExtra(EXTRA_USER_ID, -1L)
            .let { if (it <= 0) AuthManager.getUserId(this) ?: -1L else it }
        val initialPhotoUri = intent.getStringExtra(EXTRA_PHOTO_URI)

        val viewModel = ViewModelProvider(this)[StrayReportViewModel::class.java]

        setContent {
            PetRadarTheme {
                StrayReportScreen(
                    viewModel = viewModel,
                    initialPhotoUri = initialPhotoUri,
                    onBack = { finish() },
                    onSubmit = { form, mainPhotoUri, additionalPhotoUris ->
                        if (userId <= 0) return@StrayReportScreen

                        viewModel.createStrayReport(
                            request = ReportCreateModel(
                                userId = userId,
                                userPetId = null,
                                species = "NotSet",
                                reportType = "Stray",
                                reportStatus = "Active",
                                hasCollar = form.hasCollar,
                                hasTag = form.hasTag,
                                size = form.size,
                                incidentDate = form.incidentDateIso,
                                latitude = form.latitude,
                                longitude = form.longitude,
                                addressText = form.addressText,
                                searchRadiusMeters = 3000
                            ),
                            photoUri = mainPhotoUri,
                            additionalPhotoUris = additionalPhotoUris,
                            context = this
                        )
                    }
                )
            }
        }
    }
}
