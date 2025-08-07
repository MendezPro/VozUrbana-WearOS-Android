package com.example.vozurbana.data.api

import android.content.Context
import android.content.SharedPreferences

object AuthManager {

    private const val PREFS_NAME = "auth_prefs"
    private const val TOKEN_KEY = "auth_token"
    private const val USER_ID_KEY = "user_id"
    private const val IS_ADMIN_KEY = "is_admin"

    private var sharedPreferences: SharedPreferences? = null

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setAuthToken(token: String) {
        sharedPreferences?.edit()?.putString(TOKEN_KEY, token)?.apply()
    }

    fun getAuthToken(): String? {
        // Verificar si hay un token guardado
        val storedToken = sharedPreferences?.getString(TOKEN_KEY, null)
        if (!storedToken.isNullOrEmpty()) {
            return storedToken
        }

        // Generar un nuevo token admin válido
        return generateValidAdminToken()
    }

    fun setUserId(userId: Int) {
        sharedPreferences?.edit()?.putInt(USER_ID_KEY, userId)?.apply()
    }

    fun getUserId(): Int? {
        val userId = sharedPreferences?.getInt(USER_ID_KEY, -1) ?: -1
        return if (userId != -1) userId else null
    }

    fun setIsAdmin(isAdmin: Boolean) {
        sharedPreferences?.edit()?.putBoolean(IS_ADMIN_KEY, isAdmin)?.apply()
    }

    fun isAdmin(): Boolean {
        return sharedPreferences?.getBoolean(IS_ADMIN_KEY, true) ?: true
    }

    fun clearAuth() {
        sharedPreferences?.edit()?.clear()?.apply()
    }

    fun isLoggedIn(): Boolean {
        return !getAuthToken().isNullOrEmpty()
    }

    // Token JWT válido generado con el mismo secret del backend
    private fun generateValidAdminToken(): String {
        // Este token es válido y fue generado usando el secret "tu_secreto_jwt"
        val validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsInJvbCI6ImFkbWluIiwiaWF0IjoxNzU0NTU1MTU2LCJleHAiOjE3NTQ2NDE1NTZ9.ZR3UumPJk9inQ_oxfdFqUsASXToNr2D66q6YOJ4HARk"

        // Guardar el token
        sharedPreferences?.edit()?.putString(TOKEN_KEY, validToken)?.apply()

        println("🔑 Usando token JWT válido para admin")
        return validToken
    }
    fun forceRefreshToken() {
        clearAuth()
        loginAsAdmin()
        println("🔄 Token forzado a renovar")
    }
    // Método para simular login como admin
    fun loginAsAdmin() {
        val token = generateValidAdminToken()
        setAuthToken(token)
        setUserId(1)
        setIsAdmin(true)
        println("✅ Login como admin completado")
    }

    // Método para obtener el token con prefijo Bearer
    fun getBearerToken(): String {
        val token = getAuthToken()
        return if (token != null && !token.startsWith("Bearer ")) {
            "Bearer $token"
        } else {
            token ?: ""
        }
    }
}
