package com.example.vozurbana.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vozurbana.data.model.Report
import com.example.vozurbana.data.model.ReportStatus
import com.example.vozurbana.data.repository.ReportsRepository
import com.example.vozurbana.service.NotificationService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repository = ReportsRepository()
    private var notificationService: NotificationService? = null

    // Estados UI
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Reportes por categoría
    private val _newReports = MutableStateFlow<List<Report>>(emptyList())
    val newReports: StateFlow<List<Report>> = _newReports.asStateFlow()

    private val _inProcessReports = MutableStateFlow<List<Report>>(emptyList())
    val inProcessReports: StateFlow<List<Report>> = _inProcessReports.asStateFlow()

    private val _resolvedReports = MutableStateFlow<List<Report>>(emptyList())
    val resolvedReports: StateFlow<List<Report>> = _resolvedReports.asStateFlow()

    private val _closedReports = MutableStateFlow<List<Report>>(emptyList())
    val closedReports: StateFlow<List<Report>> = _closedReports.asStateFlow()

    private val _rejectedReports = MutableStateFlow<List<Report>>(emptyList())
    val rejectedReports: StateFlow<List<Report>> = _rejectedReports.asStateFlow()

    // Estado actual de la vista (qué pestaña/categoría se está viendo)
    private val _currentTab = MutableStateFlow(ReportTab.NEW)
    val currentTab: StateFlow<ReportTab> = _currentTab.asStateFlow()

    // Reportes actuales basados en la pestaña seleccionada
    private val _currentReports = MutableStateFlow<List<Report>>(emptyList())
    val currentReports: StateFlow<List<Report>> = _currentReports.asStateFlow()

    fun initializeNotificationService(context: android.content.Context) {
        notificationService = NotificationService(context)
        notificationService?.startNotificationListener()
        // Cargar todos los reportes inicialmente
        loadAllReports()
    }

    fun setCurrentTab(tab: ReportTab) {
        _currentTab.value = tab
        updateCurrentReports()
        println("📋 Cambiando a pestaña: ${tab.displayName}")
    }

    private fun updateCurrentReports() {
        val tab = _currentTab.value
        _currentReports.value = when (tab) {
            ReportTab.NEW -> _newReports.value
            ReportTab.IN_PROCESS -> _inProcessReports.value
            ReportTab.RESOLVED -> _resolvedReports.value
            ReportTab.CLOSED -> _closedReports.value
            ReportTab.REJECTED -> _rejectedReports.value
        }
    }

    // Cargar reportes de todas las categorías
    fun loadAllReports() {
        println("🔄 Cargando todos los reportes...")
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        // Cargar reportes nuevos
        loadReportsByStatus(ReportStatus.NUEVO.value) { reports ->
            _newReports.value = reports
            updateCurrentReports()
        }

        // Cargar reportes en proceso
        loadReportsByStatus(ReportStatus.EN_PROCESO.value) { reports ->
            _inProcessReports.value = reports
            updateCurrentReports()
        }

        // Cargar reportes resueltos
        loadReportsByStatus(ReportStatus.RESUELTO.value) { reports ->
            _resolvedReports.value = reports
            updateCurrentReports()
        }

        // Cargar reportes cerrados
        loadReportsByStatus(ReportStatus.CERRADO.value) { reports ->
            _closedReports.value = reports
            updateCurrentReports()
        }

        // Cargar reportes rechazados
        loadReportsByStatus(ReportStatus.NO_APROBADO.value) { reports ->
            _rejectedReports.value = reports
            updateCurrentReports()
        }
    }

    private fun loadReportsByStatus(statusValue: String, onSuccess: (List<Report>) -> Unit) {
        viewModelScope.launch {
            try {
                repository.getReportsByStatus(statusValue).collect { reports ->
                    onSuccess(reports)
                    println("✅ Cargados ${reports.size} reportes con estado: $statusValue")
                    updateUIState()
                }
            } catch (e: Exception) {
                handleError("Error cargando reportes $statusValue: ${e.message}")
            }
        }

    }

    // Actualizar estado de un reporte
    fun updateReportStatus(reportId: Int, newStatusValue: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.updateReportStatus(reportId, newStatusValue).collect { result ->
                if (result.isSuccess) {
                    val updatedReport = result.getOrNull()
                    println("✅ Reporte $reportId actualizado a estado: $newStatusValue")
                    // Recargar todos los reportes para reflejar el cambio
                    loadAllReports()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Estado actualizado exitosamente"
                    )
                } else {
                    val error = result.exceptionOrNull()
                    handleError("Error actualizando reporte: ${error?.message}")
                }
            }
        }
    }

    // Avanzar al siguiente estado
    fun advanceReportStatus(reportId: Int, currentStatusValue: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.advanceReportStatus(reportId, currentStatusValue).collect { result ->
                if (result.isSuccess) {
                    val updatedReport = result.getOrNull()
                    println("✅ Reporte $reportId avanzado desde $currentStatusValue a ${updatedReport?.estado}")
                    loadAllReports()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Reporte actualizado a: ${updatedReport?.estado ?: "nuevo estado"}"
                    )
                } else {
                    val error = result.exceptionOrNull()
                    handleError("Error avanzando reporte: ${error?.message}")
                }
            }
        }
    }

    // Rechazar reporte
    fun rejectReport(reportId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.rejectReport(reportId).collect { result ->
                if (result.isSuccess) {
                    val updatedReport = result.getOrNull()
                    println("✅ Reporte $reportId rechazado exitosamente")
                    loadAllReports()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Reporte rechazado exitosamente"
                    )
                } else {
                    val error = result.exceptionOrNull()
                    handleError("Error rechazando reporte: ${error?.message}")
                }
            }
        }
    }

    // Obtener siguiente acción disponible para un reporte
    fun getNextAction(report: Report): ReportAction? {
        return when (report.estado) {
            ReportStatus.NUEVO.value -> ReportAction.MARK_IN_PROCESS
            ReportStatus.EN_PROCESO.value -> ReportAction.MARK_RESOLVED
            ReportStatus.RESUELTO.value -> ReportAction.MARK_CLOSED
            ReportStatus.CERRADO.value -> null // No hay más acciones
            ReportStatus.NO_APROBADO.value -> null // No hay más acciones
            else -> null
        }
    }

    // Refrescar reportes
    fun refreshReports() {
        println("🔄 Refrescando reportes...")
        loadAllReports()
    }

    private fun updateUIState() {
        val totalNew = _newReports.value.size
        val totalInProcess = _inProcessReports.value.size
        val totalResolved = _resolvedReports.value.size
        val totalClosed = _closedReports.value.size
        val totalRejected = _rejectedReports.value.size

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            newReportsCount = totalNew,
            inProcessReportsCount = totalInProcess,
            resolvedReportsCount = totalResolved,
            closedReportsCount = totalClosed,
            rejectedReportsCount = totalRejected,
            hasNewNotification = totalNew > 0
        )
    }

    private fun handleError(errorMessage: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = errorMessage
        )
        println("❌ $errorMessage")
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        notificationService?.stopNotificationListener()
    }
}

// Estados de UI
data class MainUiState(
    val isLoading: Boolean = false,
    val newReportsCount: Int = 0,
    val inProcessReportsCount: Int = 0,
    val resolvedReportsCount: Int = 0,
    val closedReportsCount: Int = 0,
    val rejectedReportsCount: Int = 0,
    val error: String? = null,
    val message: String? = null,
    val hasNewNotification: Boolean = false
)

// Enum para las pestañas/categorías de reportes
enum class ReportTab(val displayName: String, val statusValue: String) {
    NEW("Nuevos", ReportStatus.NUEVO.value),
    IN_PROCESS("En Proceso", ReportStatus.EN_PROCESO.value),
    RESOLVED("Resueltos", ReportStatus.RESUELTO.value),
    CLOSED("Cerrados", ReportStatus.CERRADO.value),
    REJECTED("Rechazados", ReportStatus.NO_APROBADO.value)
}

// Enum para las acciones disponibles en cada reporte
enum class ReportAction(val displayName: String, val newStatusValue: String) {
    MARK_IN_PROCESS("Marcar en Proceso", ReportStatus.EN_PROCESO.value),
    MARK_RESOLVED("Marcar Resuelto", ReportStatus.RESUELTO.value),
    MARK_CLOSED("Marcar Cerrado", ReportStatus.CERRADO.value),
    REJECT("Rechazar", ReportStatus.NO_APROBADO.value)
}
