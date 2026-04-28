package com.petradar.mobileui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.petradar.mobileui.ui.AppointmentsScreen
import com.petradar.mobileui.ui.theme.PetRadarTheme
import com.petradar.mobileui.utils.AuthManager
import com.petradar.mobileui.viewmodel.AppointmentViewModel

/**
 * AppointmentsActivity displays the user's veterinary appointment calendar
 * and allows creating or editing appointments.
 *
 * Flow:
 *  - On launch, loads the user's appointments via GET /api/VeterinaryAppointments/user/{userId}.
 *  - The calendar highlights days that have appointments.
 *  - Pressing "+" on a day navigates to [AppointmentFormActivity] in creation mode,
 *    passing the selected date as [com.petradar.mobileui.AppointmentFormActivity.Companion.EXTRA_INITIAL_DATE].
 *  - Tapping an existing appointment navigates to [AppointmentFormActivity] in edit mode,
 *    passing the appointmentId.
 *  - On returning from [AppointmentFormActivity] (onResume), reloads the appointments.
 *
 * Delegates all UI to [AppointmentsScreen] (Jetpack Compose).
 */
class AppointmentsActivity : ComponentActivity() {

    private lateinit var viewModel: AppointmentViewModel
    /** ID of the signed-in user; required to load and filter their appointments. */
    private var userId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Retrieve the user ID from the local session.
        userId = AuthManager.getUserId(this) ?: -1L
        if (userId <= 0) {
            Toast.makeText(this, "Error: user not identified", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[AppointmentViewModel::class.java]
        // Initial load of appointments on screen open.
        viewModel.loadByUser(userId)

        val activity = this
        setContent {
            PetRadarTheme {
                AppointmentsScreen(
                    viewModel = viewModel,
                    userId = userId,
                    // Navigate to create a new appointment; pass the date selected in the calendar.
                    onAddAppointment = { selectedDate ->
                        val intent = Intent(activity, AppointmentFormActivity::class.java)
                        intent.putExtra(AppointmentFormActivity.EXTRA_USER_ID, userId)
                        // The date is passed as an ISO String (yyyy-MM-dd) so the form
                        // pre-fills it and the user does not have to enter it manually.
                        intent.putExtra(AppointmentFormActivity.EXTRA_INITIAL_DATE, selectedDate.toString())
                        activity.startActivity(intent)
                    },
                    // Navigate to edit an existing appointment; pass the appointment ID.
                    onEditAppointment = { appt ->
                        val intent = Intent(activity, AppointmentFormActivity::class.java)
                        intent.putExtra(AppointmentFormActivity.EXTRA_APPOINTMENT_ID, appt.id)
                        intent.putExtra(AppointmentFormActivity.EXTRA_USER_ID, userId)
                        activity.startActivity(intent)
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    /**
     * Called every time this Activity returns to the foreground (e.g. after creating or editing
     * an appointment). Reloads appointments to reflect any changes made in [AppointmentFormActivity].
     */
    override fun onResume() {
        super.onResume()
        viewModel.loadByUser(userId)
    }
}
