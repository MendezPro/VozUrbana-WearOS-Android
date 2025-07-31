package com.example.vozurbana

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

class VozUrbanaApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        // NO inicializar WorkManager aquí - se inicializa automáticamente
        println("🚀 VozUrbanaApplication iniciada correctamente")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}