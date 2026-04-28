package com.petradar.mobileui

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.petradar.mobileui.ui.ReportEditScreen
import com.petradar.mobileui.ui.theme.PetRadarTheme
import com.petradar.mobileui.viewmodel.ReportEditViewModel

class ReportEditActivity : ComponentActivity() {

    companion object {
        const val EXTRA_REPORT_ID = "extra_report_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val reportId = intent.getLongExtra(EXTRA_REPORT_ID, -1L)
        val viewModel = ViewModelProvider(this)[ReportEditViewModel::class.java]
        if (reportId > 0) viewModel.loadReport(reportId)
        setContent {
            PetRadarTheme {
                ReportEditScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onSaved = { setResult(RESULT_OK); finish() }
                )
            }
        }
    }
}
