package com.example.vozurbana.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vozurbana.data.api.ApiClient
import com.example.vozurbana.data.model.Report
import com.example.vozurbana.service.NotificationService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val api = ApiClient.api
    private var notificationService: NotificationService? = null

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _newReports = MutableStateFlow<List<Report>>(emptyList())
    val newReports: StateFlow<List<Report>> = _newReports.asStateFlow()

    fun initializeNotificationService(context: android.content.Context) {
        notificationService = NotificationService(context)
        notificationService?.startNotificationListener()
        // Cargar reportes iniciales
        loadNewReports()
    }

    fun loadNewReports() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Buscar reportes con estado 'pendiente' que es lo que usa tu backend
                val response = api.getPendingReports()
                if (response.isSuccessful) {
                    val reports = response.body() ?: emptyList()
                    _newReports.value = reports
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        reportsCount = reports.size,
                        error = null
                    )
                    println("✅ Cargados ${reports.size} reportes pendientes desde el backend")
                } else {
                    val errorMsg = "Error del servidor: ${response.code()}"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                    println("❌ Error de API: $errorMsg")
                }
            } catch (e: Exception) {
                val errorMsg = "Sin conexión al servidor: ${e.message}"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMsg
                )
                println("❌ Error de conexión: $errorMsg")

                // Si hay error de conexión, mostrar 0 reportes (pantalla vacía)
                _newReports.value = emptyList()
                _uiState.value = _uiState.value.copy(reportsCount = 0)
            }
        }
    }

    fun refreshReports() {
        println("🔄 Actualizando reportes...")
        loadNewReports()
    }

    // Método para actualizar reportes cuando llegue una notificación WebSocket
    fun onNewReportNotification(report: Report) {
        viewModelScope.launch {
            val currentReports = _newReports.value.toMutableList()
            currentReports.add(0, report) // Agregar al inicio
            _newReports.value = currentReports
            _uiState.value = _uiState.value.copy(
                reportsCount = currentReports.size,
                hasNewNotification = true
            )
            println("📨 Nuevo reporte agregado via WebSocket: ${report.titulo}")
        }
    }

    // Método para manejar cambios de estado via WebSocket
    fun onReportStatusChanged(reportId: Int, newStatus: String) {
        viewModelScope.launch {
            val currentReports = _newReports.value.toMutableList()
            val updatedReports = currentReports.filter { it.id != reportId } // Remover si ya no está pendiente
            _newReports.value = updatedReports
            _uiState.value = _uiState.value.copy(
                reportsCount = updatedReports.size
            )
            println("📝 Reporte $reportId cambió a $newStatus y fue removido de pendientes")
        }
    }

    override fun onCleared() {
        super.onCleared()
        notificationService?.stopNotificationListener()
    }
}

data class MainUiState(
    val isLoading: Boolean = false,
    val reportsCount: Int = 0,
    val error: String? = null,
    val hasNewNotification: Boolean = false
)