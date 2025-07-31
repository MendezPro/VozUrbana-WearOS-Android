package com.example.vozurbana.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.util.concurrent.TimeUnit

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            createNotificationChannel()
            connectToWebSocket()
            Result.success()
        } catch (e: Exception) {
            println("Worker error: ${e.message}")
            Result.retry()
        }
    }

    private fun connectToWebSocket() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("ws://10.0.2.2:3000/ws") // IP correcta para emulador Android
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                println("✅ WebSocket conectado desde Wear OS")
                // Suscribirse a notificaciones
                val subscribeMessage = """{"type": "subscribe"}"""
                webSocket.send(subscribeMessage)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                println("📨 Mensaje recibido en Wear OS: $text")
                handleNotification(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("❌ WebSocket error: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                println("❌ WebSocket closed: $code - $reason")
            }
        }

        val webSocket = client.newWebSocket(request, listener)

        // Mantener la conexión viva por un tiempo
        Thread.sleep(30000) // 30 segundos para prueba
        webSocket.close(1000, "Trabajo completado")
    }

    private fun handleNotification(message: String) {
        try {
            val gson = Gson()
            val jsonObject = gson.fromJson(message, JsonObject::class.java)

            when (jsonObject.get("type")?.asString) {
                "new_report" -> {
                    val data = jsonObject.getAsJsonObject("data")
                    val title = data.get("titulo")?.asString ?: "Nuevo Reporte"
                    showNotification("🚨 $title", "Nuevo reporte creado en Voz Urbana")
                }
                "status_change" -> {
                    val data = jsonObject.getAsJsonObject("data")
                    val title = data.get("titulo")?.asString ?: "Reporte"
                    val status = data.get("newStatus")?.asString ?: "actualizado"
                    showNotification("📝 $title", "Estado cambiado a: $status")
                }
                "pending_reports" -> {
                    val data = jsonObject.getAsJsonObject("data")
                    val count = data.get("count")?.asInt ?: 0
                    showNotification("⏰ Reportes Pendientes", "$count reportes requieren atención")
                }
            }
        } catch (e: Exception) {
            println("Error procesando notificación: ${e.message}")
            showNotification("📨 Nueva Notificación", "Mensaje recibido de Voz Urbana")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voz Urbana Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de reportes de Voz Urbana"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, content: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "voz_urbana_channel"
    }
}

class NotificationService(
    private val context: Context
) {

    fun startNotificationListener() {
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            15, TimeUnit.MINUTES // Verificar cada 15 minutos
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "notification_worker",
            ExistingPeriodicWorkPolicy.REPLACE, // Cambié de KEEP a REPLACE
            workRequest
        )
    }

    fun startImmediateNotificationTest() {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    fun stopNotificationListener() {
        WorkManager.getInstance(context).cancelUniqueWork("notification_worker")
    }
}