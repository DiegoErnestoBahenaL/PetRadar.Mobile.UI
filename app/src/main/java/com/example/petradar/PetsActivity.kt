package com.example.petradar

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.petradar.ui.PetsScreen
import com.example.petradar.ui.theme.PetRadarTheme
import com.example.petradar.utils.AuthManager
import com.example.petradar.viewmodel.PetViewModel

class PetsActivity : ComponentActivity() {

    private lateinit var viewModel: PetViewModel
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

        viewModel = ViewModelProvider(this)[PetViewModel::class.java]
        viewModel.loadPets(userId)

        setContent {
            PetRadarTheme {
                PetsScreen(
                    viewModel = viewModel,
                    userId = userId,
                    onAddPet = {
                        val intent = Intent(this, PetDetailActivity::class.java).apply {
                            putExtra(PetDetailActivity.EXTRA_USER_ID, userId)
                        }
                        startActivity(intent)
                    },
                    onEditPet = { pet ->
                        val intent = Intent(this, PetDetailActivity::class.java).apply {
                            putExtra(PetDetailActivity.EXTRA_PET_ID, pet.id)
                        }
                        startActivity(intent)
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload pets when returning from PetDetailActivity
        viewModel.loadPets(userId)
    }
}
