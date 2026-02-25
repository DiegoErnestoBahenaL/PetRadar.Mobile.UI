package com.example.petradar

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.ui.AppointmentsScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.utils.AuthManager
import com.example.petradar.viewmodel.AppointmentViewModel

class AppointmentsActivity : ComponentActivity() {

    private lateinit var viewModel: AppointmentViewModel
    private var userId: Long = -1L

    companion object {
        const val EXTRA_APPOINTMENT_ID = "extra_appointment_id"
        const val EXTRA_PET_ID = "extra_pet_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        userId = AuthManager.getUserId(this) ?: -1L
        if (userId <= 0) {
            Toast.makeText(this, "Error: usuario no identificado", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[AppointmentViewModel::class.java]
        viewModel.loadByUser(userId)

        val activity = this
        setContent {
            PetRadarTheme {
                AppointmentsScreen(
                    viewModel = viewModel,
                    userId = userId,
                    onAddAppointment = { selectedDate ->
                        val intent = Intent(activity, AppointmentFormActivity::class.java)
                        intent.putExtra(AppointmentFormActivity.EXTRA_USER_ID, userId)
                        intent.putExtra(AppointmentFormActivity.EXTRA_INITIAL_DATE, selectedDate.toString())
                        activity.startActivity(intent)
                    },
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

    override fun onResume() {
        super.onResume()
        viewModel.loadByUser(userId)
    }
}
