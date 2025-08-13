package com.example.vozurbana.service

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class NotificationService(private val context: Context) {

    private var webSocket: WebSocket? = null
    private var isConnected = false
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Callbacks para notificar cambios a la UI
    var onNewReport: ((reportId: Int, titulo: String) -> Unit)? = null
    var onStatusChange: ((reportId: Int, newStatus: String, oldStatus: String) -> Unit)? = null
    var onConnectionChange: ((isConnected: Boolean) -> Unit)? = null

    fun startNotificationListener() {
        println("🔄 Iniciando servicio de notificaciones WebSocket...")

        // Usar WorkManager para el trabajo en segundo plano
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val notificationWork = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setConstraints(constraints)
            .setInitialDelay(2, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(notificationWork)

        // También intentar conexión directa
        connectToWebSocket()
    }

    fun stopNotificationListener() {
        println("🛑 Deteniendo servicio de notificaciones...")
        webSocket?.close(1000, "Cerrando conexión")
        webSocket = null
        isConnected = false
        onConnectionChange?.invoke(false)

        // Cancelar trabajos de WorkManager
        WorkManager.getInstance(context).cancelAllWorkByTag("notification_websocket")
    }

    private fun connectToWebSocket() {
        val request = Request.Builder()
            .url("ws://192.168.0.103:3000/ws") // IP real de tu red
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                println("✅ WebSocket conectado correctamente")
                isConnected = true
                onConnectionChange?.invoke(true)

                // Suscribirse a notificaciones usando el formato que espera el backend
                val subscribeMessage = """
                    {
                        "type": "subscribe"
                    }
                """.trimIndent()

                println("📤 Suscribiéndose a notificaciones: $subscribeMessage")
                webSocket.send(subscribeMessage)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                println("📨 Mensaje WebSocket recibido: $text")
                handleWebSocketMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                println("📨 Mensaje binario WebSocket recibido")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                println("⚠️ WebSocket cerrándose: $code - $reason")
                isConnected = false
                onConnectionChange?.invoke(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                println("🔴 WebSocket cerrado: $code - $reason")
                isConnected = false
                onConnectionChange?.invoke(false)

                // Intentar reconectar después de 5 segundos
                CoroutineScope(Dispatchers.IO).launch {
                    delay(5000)
                    if (!isConnected) {
                        println("🔄 Intentando reconectar WebSocket...")
                        connectToWebSocket()
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("❌ Error en WebSocket: ${t.message}")
                isConnected = false
                onConnectionChange?.invoke(false)

                // Intentar reconectar después de 10 segundos
                CoroutineScope(Dispatchers.IO).launch {
                    delay(10000)
                    if (!isConnected) {
                        println("🔄 Reintentando conexión WebSocket tras error...")
                        connectToWebSocket()
                    }
                }
            }
        }

        webSocket = client.newWebSocket(request, listener)
    }

    private fun handleWebSocketMessage(message: String) {
        try {
            println("📨 Procesando mensaje: $message")

            when {
                message.contains("\"type\":\"new_report\"") -> {
                    // Estructura: { type: 'new_report', data: { reportId, titulo, ... } }
                    val reportId = extractNestedJsonValue(message, "data", "reportId")?.toIntOrNull() ?: 0
                    val titulo = extractNestedJsonValue(message, "data", "titulo") ?: "Nuevo reporte"

                    println("📋 Nuevo reporte detectado: ID=$reportId, Título=$titulo")
                    onNewReport?.invoke(reportId, titulo)
                }

                message.contains("\"type\":\"status_change\"") -> {
                    // Estructura: { type: 'status_change', data: { reportId, oldStatus, newStatus } }
                    val reportId = extractNestedJsonValue(message, "data", "reportId")?.toIntOrNull() ?: 0
                    val newStatus = extractNestedJsonValue(message, "data", "newStatus") ?: ""
                    val oldStatus = extractNestedJsonValue(message, "data", "oldStatus") ?: ""

                    println("📝 Cambio de estado detectado: ID=$reportId, $oldStatus -> $newStatus")
                    onStatusChange?.invoke(reportId, newStatus, oldStatus)
                }

                message.contains("\"type\":\"connected\"") -> {
                    println("🔗 Mensaje de conexión recibido del servidor")
                }

                message.contains("\"type\":\"pong\"") -> {
                    println("🏓 Pong recibido del servidor")
                }

                else -> {
                    println("📨 Mensaje WebSocket no reconocido: $message")
                }
            }
        } catch (e: Exception) {
            println("❌ Error procesando mensaje WebSocket: ${e.message}")
        }
    }

    // Función helper para extraer valores JSON (básica)
    private fun extractJsonValue(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"|\"$key\"\\s*:\\s*([^,}\\s]*)"
        val regex = Regex(pattern)
        val matchResult = regex.find(json)
        return matchResult?.groupValues?.let { groups ->
            groups[1].takeIf { it.isNotEmpty() } ?: groups[2].takeIf { it.isNotEmpty() }
        }
    }

    // Función helper para extraer valores JSON anidados
    private fun extractNestedJsonValue(json: String, parentKey: String, childKey: String): String? {
        // Buscar el objeto padre
        val parentPattern = "\"$parentKey\"\\s*:\\s*\\{([^}]*)"
        val parentRegex = Regex(parentPattern)
        val parentMatch = parentRegex.find(json)

        return parentMatch?.groupValues?.get(1)?.let { parentContent ->
            // Dentro del objeto padre, buscar la clave hija
            extractJsonValue("{ $parentContent }", childKey)
        }
    }

    fun isConnectedToWebSocket(): Boolean = isConnected
}

// Worker para manejar notificaciones en segundo plano
class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            println("🔄 NotificationWorker ejecutándose...")

            // Aquí puedes agregar lógica adicional para verificar el estado
            // de los reportes periódicamente o manejar notificaciones push

            delay(5000) // Simular trabajo

            println("✅ NotificationWorker completado")
            Result.success()
        } catch (e: Exception) {
            println("❌ Error en NotificationWorker: ${e.message}")
            Result.failure()
        }
    }
}

// Extensión para facilitar el uso del servicio
fun NotificationService.attachToViewModel(viewModel: com.example.vozurbana.presentation.viewmodel.MainViewModel) {
    this.onNewReport = { reportId, titulo ->
        println("🔔 Notificando nuevo reporte al ViewModel: $reportId - $titulo")
        // Recargar todos los reportes para mostrar el nuevo
        viewModel.refreshReports()
    }

    this.onStatusChange = { reportId, newStatus, oldStatus ->
        println("🔔 Notificando cambio de estado al ViewModel: $reportId $oldStatus -> $newStatus")
        // Recargar todos los reportes para reflejar el cambio de estado
        viewModel.refreshReports()
    }

    this.onConnectionChange = { isConnected ->
        println("🔔 Estado de conexión WebSocket: ${if (isConnected) "Conectado" else "Desconectado"}")
        // Aquí podrías actualizar el UI para mostrar el estado de conexión
    }
}
