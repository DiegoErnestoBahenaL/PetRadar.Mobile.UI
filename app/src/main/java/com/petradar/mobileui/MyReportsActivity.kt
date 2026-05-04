package com.petradar.mobileui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.petradar.mobileui.ui.MyReportsScreen
import com.petradar.mobileui.ui.theme.PetRadarTheme
import com.petradar.mobileui.utils.AuthManager
import com.petradar.mobileui.viewmodel.MyReportsViewModel

class MyReportsActivity : ComponentActivity() {

    private lateinit var viewModel: MyReportsViewModel
    private var userId: Long = -1L

    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.loadReports(userId)
        }
    }

    private val newReportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.loadReports(userId)
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

        viewModel = ViewModelProvider(this)[MyReportsViewModel::class.java]
        viewModel.loadReports(userId)

        setContent {
            PetRadarTheme {
                MyReportsScreen(
                    viewModel = viewModel,
                    userId = userId,
                    onBack = { finish() },
                    onNewReport = {
                        val intent = Intent(this, PetPickerActivity::class.java)
                        newReportLauncher.launch(intent)
                    },
                    onEditReport = { report ->
                        val intent = Intent(this, ReportEditActivity::class.java)
                        intent.putExtra(ReportEditActivity.EXTRA_REPORT_ID, report.id)
                        editLauncher.launch(intent)
                    },
                    onDeleteReport = { reportId ->
                        viewModel.deleteReport(reportId)
                    },
                    onDismissReport = { reportId ->
                        viewModel.dismissReport(reportId)
                    },
                    onOpenMatchChat = { matchId, otherUserId, matchTitle, lostReportId, lostPetLabel, strayReportId ->
                        val intent = Intent(this, ChatActivity::class.java).apply {
                            putExtra(ChatActivity.EXTRA_MATCH_ID, matchId)
                            putExtra(ChatActivity.EXTRA_OTHER_USER_ID, otherUserId)
                            putExtra(ChatActivity.EXTRA_ANIMAL_NAME, matchTitle)
                            putExtra(ChatActivity.EXTRA_LOST_REPORT_ID, lostReportId)
                            putExtra(ChatActivity.EXTRA_LOST_PET_LABEL, lostPetLabel)
                            putExtra(ChatActivity.EXTRA_STRAY_REPORT_ID, strayReportId)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}
