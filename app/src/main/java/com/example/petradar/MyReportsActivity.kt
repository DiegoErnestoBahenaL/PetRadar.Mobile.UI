package com.example.petradar

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.ui.MyReportsScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.utils.AuthManager
import com.example.petradar.viewmodel.MyReportsViewModel

class MyReportsActivity : ComponentActivity() {

    private lateinit var viewModel: MyReportsViewModel
    private var userId: Long = -1L

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
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadReports(userId)
    }
}

