package com.example.vozurbana

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.vozurbana.data.api.AuthManager

class VozUrbanaApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
// Inicializar AuthManager con el contexto
        AuthManager.initialize(this)
// Login automático como admin para pruebas
        AuthManager.loginAsAdmin()
        // Configurar token de autenticación si está disponible
        // En una implementación real, esto vendría de SharedPreferences o base de datos local
        setupAuthToken()

        println("🚀 VozUrbanaApplication iniciada correctamente")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private fun setupAuthToken() {
        // Aquí deberías cargar el token del almacenamiento local
        // Por ahora, usaremos un token de ejemplo para admin
        // TODO: Implementar sistema de autenticación completo

        // Ejemplo de token (en la realidad vendría del login)
        val exampleAdminToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.example.admin.token"

        // Solo configurar token si existe y es válido
        // AuthManager.setAuthToken(exampleAdminToken)

        println("🔐 Sistema de autenticación inicializado")
    }
}
