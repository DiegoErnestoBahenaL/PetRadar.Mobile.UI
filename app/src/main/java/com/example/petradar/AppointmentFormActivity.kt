package com.example.petradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.ui.AppointmentFormScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.utils.AuthManager
import com.example.petradar.viewmodel.AppointmentViewModel
import java.time.LocalDate

class AppointmentFormActivity : ComponentActivity() {

    companion object {
        const val EXTRA_APPOINTMENT_ID = "extra_appointment_id"
        const val EXTRA_PET_ID = "extra_pet_id"
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_INITIAL_DATE = "extra_initial_date"  // ISO yyyy-MM-dd
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appointmentId = intent.getLongExtra(EXTRA_APPOINTMENT_ID, -1L)
        val petId = intent.getLongExtra(EXTRA_PET_ID, -1L)
        val userId = intent.getLongExtra(EXTRA_USER_ID, -1L)
            .let { if (it <= 0) AuthManager.getUserId(this) ?: -1L else it }

        // Parse the pre-selected calendar date (null = use today as default)
        val initialDate: LocalDate? = intent.getStringExtra(EXTRA_INITIAL_DATE)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        val viewModel = ViewModelProvider(this)[AppointmentViewModel::class.java]
        if (appointmentId > 0) viewModel.loadById(appointmentId)

        setContent {
            PetRadarTheme {
                AppointmentFormScreen(
                    viewModel = viewModel,
                    isEditMode = appointmentId > 0,
                    petId = petId,
                    userId = userId,
                    initialDate = initialDate,
                    onBack = { finish() }
                )
            }
        }
    }
}
