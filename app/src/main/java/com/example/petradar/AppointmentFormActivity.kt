package com.example.petradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.ui.AppointmentFormScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.utils.AuthManager
import com.example.petradar.utils.NotificationHelper
import com.example.petradar.viewmodel.AppointmentViewModel
import java.time.LocalDate

/**
 * AppointmentFormActivity is the form for creating or editing a veterinary appointment.
 *
 * Create mode → [EXTRA_APPOINTMENT_ID] is not provided (or is -1).
 *               May receive [EXTRA_INITIAL_DATE] (pre-selected date from the calendar)
 *               and [EXTRA_USER_ID] to associate the appointment with the user.
 *
 * Edit mode   → [EXTRA_APPOINTMENT_ID] is provided with an id > 0.
 *               The ViewModel loads the appointment data via GET /api/VeterinaryAppointments/{id}.
 *
 * On save:
 *  - Create mode → POST /api/VeterinaryAppointments
 *  - Edit mode   → PUT  /api/VeterinaryAppointments/{id}
 *
 * Delegates all UI to [AppointmentFormScreen] (Jetpack Compose).
 */
class AppointmentFormActivity : ComponentActivity() {

    companion object {
        /** Extra holding the appointment ID to edit. Absent or -1 = creation mode. */
        const val EXTRA_APPOINTMENT_ID = "extra_appointment_id"
        /** Extra holding a pre-selected pet ID (for future use). */
        const val EXTRA_PET_ID = "extra_pet_id"
        /** Extra holding the owner user ID. */
        const val EXTRA_USER_ID = "extra_user_id"
        /** Extra holding the initial date in ISO yyyy-MM-dd format, coming from the calendar. */
        const val EXTRA_INITIAL_DATE = "extra_initial_date"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Read parameters from the Intent.
        val appointmentId = intent.getLongExtra(EXTRA_APPOINTMENT_ID, -1L)
        val petId = intent.getLongExtra(EXTRA_PET_ID, -1L)
        // Fallback: if userId is not in the Intent, read it from the saved session.
        val userId = intent.getLongExtra(EXTRA_USER_ID, -1L)
            .let { if (it <= 0) AuthManager.getUserId(this) ?: -1L else it }

        // Parse the initial date received from the calendar (null = use today).
        // runCatching guards against malformed strings.
        val initialDate: LocalDate? = intent.getStringExtra(EXTRA_INITIAL_DATE)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        val viewModel = ViewModelProvider(this)[AppointmentViewModel::class.java]
        // In edit mode, pre-load the existing appointment data.
        if (appointmentId > 0) viewModel.loadById(appointmentId)

        setContent {
            PetRadarTheme {
                AppointmentFormScreen(
                    viewModel = viewModel,
                    isEditMode = appointmentId > 0, // true = edit, false = create
                    petId = petId,
                    userId = userId,
                    // Date pre-selected from the calendar; the form shows it in the date
                    // field so the user does not have to enter it manually.
                    initialDate = initialDate,
                    onBack = { finish() },
                    onAfterSave = { appointmentDateTime, remindDayBefore, petName, reason ->
                        // Cancel any existing reminders for this appointment before rescheduling
                        val idForAlarm = if (appointmentId > 0) appointmentId
                                         else System.currentTimeMillis() // new appointments use timestamp as temporary ID
                        if (appointmentId > 0) {
                            NotificationHelper.cancelReminders(this, appointmentId)
                        }
                        NotificationHelper.scheduleReminders(
                            context = this,
                            appointmentId = idForAlarm,
                            petName = petName.ifBlank { "tu mascota" },
                            reason = reason.ifBlank { "cita veterinaria" },
                            appointmentTime = appointmentDateTime,
                            remindDayBefore = remindDayBefore
                        )
                    }
                )
            }
        }
    }
}
