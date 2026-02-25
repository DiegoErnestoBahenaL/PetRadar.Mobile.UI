package com.example.petradar

import android.app.Application
import com.example.petradar.api.RetrofitClient

class PetRadarApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicializar el cliente Retrofit con el contexto de la aplicación
        RetrofitClient.init(this)
    }
}

