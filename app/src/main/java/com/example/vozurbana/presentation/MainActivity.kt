package com.example.vozurbana.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.vozurbana.data.model.Report
import com.example.vozurbana.data.model.ReportPriority
import com.example.vozurbana.data.model.ReportStatus
import com.example.vozurbana.data.model.User
import com.example.vozurbana.presentation.theme.VozurbanaTheme
import com.example.vozurbana.presentation.viewmodel.MainViewModel
import com.example.vozurbana.presentation.viewmodel.ReportTab
import com.example.vozurbana.presentation.viewmodel.ReportAction

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        // Inicializar ViewModel con contexto
        viewModel.initializeNotificationService(this)

        setContent {
            VozurbanaTheme {
                WearApp(viewModel)
            }
        }

        println("🚀 MainActivity iniciada con gestión completa de reportes")
    }
}

@Composable
fun WearApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val currentReports by viewModel.currentReports.collectAsState()

    // Mostrar mensajes y errores
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            // Aquí puedes mostrar un toast o snackbar
            println("💬 Mensaje: $it")
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            println("❌ Error: $it")
            kotlinx.coroutines.delay(5000)
            viewModel.clearError()
        }
    }

    when {
        uiState.isLoading -> LoadingScreen()
        uiState.error != null -> ErrorScreen(
            error = uiState.error!!,
            onRetry = { viewModel.refreshReports() }
        )
        else -> MainDashboard(
            viewModel = viewModel,
            uiState = uiState,
            currentTab = currentTab,
            currentReports = currentReports
        )
    }
}

@Composable
fun MainDashboard(
    viewModel: MainViewModel,
    uiState: com.example.vozurbana.presentation.viewmodel.MainUiState,
    currentTab: ReportTab,
    currentReports: List<Report>
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        state = listState
    ) {
        // Header con título y estadísticas
        item {
            DashboardHeader(uiState = uiState)
        }

        // Pestañas de navegación
        item {
            TabSelector(
                currentTab = currentTab,
                uiState = uiState,
                onTabSelected = { viewModel.setCurrentTab(it) }
            )
        }

        // Lista de reportes de la pestaña actual
        if (currentReports.isEmpty() && !uiState.isLoading) {
            item {
                EmptyStateMessage(currentTab = currentTab)
            }
        } else {
            items(currentReports) { report ->
                ReportCard(
                    report = report,
                    viewModel = viewModel,
                    currentTab = currentTab
                )
            }
        }

        // Botón de refrescar
        item {
            RefreshButton(
                onRefresh = { viewModel.refreshReports() },
                isLoading = uiState.isLoading
            )
        }
    }
}

@Composable
fun DashboardHeader(uiState: com.example.vozurbana.presentation.viewmodel.MainUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = { /* No action needed for header */ }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Voz Urbana",
                style = MaterialTheme.typography.title3,
                color = MaterialTheme.colors.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    count = uiState.newReportsCount,
                    label = "Nuevos",
                    color = Color(0xFFE53E3E)
                )
                StatusBadge(
                    count = uiState.inProcessReportsCount,
                    label = "Proceso",
                    color = Color(0xFFD69E2E)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(count: Int, label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurface
        )
    }
}

@Composable
fun TabSelector(
    currentTab: ReportTab,
    uiState: com.example.vozurbana.presentation.viewmodel.MainUiState,
    onTabSelected: (ReportTab) -> Unit
) {
    val tabs = listOf(
        ReportTab.NEW to uiState.newReportsCount,
        ReportTab.IN_PROCESS to uiState.inProcessReportsCount,
        ReportTab.RESOLVED to uiState.resolvedReportsCount,
        ReportTab.CLOSED to uiState.closedReportsCount,
        ReportTab.REJECTED to uiState.rejectedReportsCount
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { /* No action needed for tab container */ }
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Categorías",
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            tabs.forEach { (tab, count) ->
                Chip(
                    onClick = { onTabSelected(tab) },
                    label = {
                        Text(
                            text = "${tab.displayName} ($count)",
                            style = MaterialTheme.typography.caption2
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = if (currentTab == tab) {
                        ChipDefaults.primaryChipColors()
                    } else {
                        ChipDefaults.secondaryChipColors()
                    }
                )
            }
        }
    }
}

@Composable
fun ReportCard(
    report: Report,
    viewModel: MainViewModel,
    currentTab: ReportTab
) {
    val nextAction = viewModel.getNextAction(report)
    val priority = ReportPriority.fromValue(report.prioridad)
    val status = ReportStatus.fromValue(report.estado)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        onClick = { /* Abrir detalles del reporte si es necesario */ }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Título y prioridad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.titulo,
                    style = MaterialTheme.typography.caption1,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                priority?.let {
                    val colorValue = try {
                        android.graphics.Color.parseColor(it.color)
                    } catch (e: Exception) {
                        android.graphics.Color.GRAY
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(colorValue))
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Descripción
            Text(
                text = report.descripcion,
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Estado actual
            Text(
                text = "Estado: ${status?.displayName ?: report.estado}",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.primary
            )

            // Botones de acción
            if (nextAction != null || currentTab == ReportTab.NEW) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Botón para avanzar estado
                    nextAction?.let { action ->
                        Chip(
                            onClick = {
                                viewModel.advanceReportStatus(report.id, report.estado)
                            },
                            label = {
                                Text(
                                    text = action.displayName,
                                    style = MaterialTheme.typography.caption3
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ChipDefaults.primaryChipColors()
                        )
                    }

                    // Botón de rechazar (solo para reportes nuevos)
                    if (currentTab == ReportTab.NEW) {
                        Chip(
                            onClick = {
                                viewModel.rejectReport(report.id)
                            },
                            label = {
                                Text(
                                    text = "Rechazar",
                                    style = MaterialTheme.typography.caption3
                                )
                            },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = Color(0xFFE53E3E)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateMessage(currentTab: ReportTab) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { /* No action needed for empty state */ }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sin reportes ${currentTab.displayName.lowercase()}",
                style = MaterialTheme.typography.caption1,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun RefreshButton(
    onRefresh: () -> Unit,
    isLoading: Boolean
) {
    Chip(
        onClick = onRefresh,
        label = {
            Text(
                text = if (isLoading) "Actualizando..." else "Refrescar",
                style = MaterialTheme.typography.caption2
            )
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ChipDefaults.secondaryChipColors(),
        enabled = !isLoading
    )
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cargando...",
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onSurface
            )
        }
    }
}

@Composable
fun ErrorScreen(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.title3,
                color = Color(0xFFE53E3E),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Chip(
                onClick = onRetry,
                label = { Text("Reintentar") },
                colors = ChipDefaults.primaryChipColors()
            )
        }
    }
}

// Preview functions
fun getSampleReports(): List<Report> {
    val sampleUser = User(1, "Admin Demo", "admin@example.com")
    return listOf(
        Report(
            id = 1,
            titulo = "Bache en avenida principal",
            descripcion = "Bache grande que puede causar accidentes",
            categoriaId = 1,
            ubicacion = "Av. Principal #123",
            latitud = 20.2745,
            longitud = -97.9557,
            estado = "nuevo",
            prioridad = "alta",
            imagenUrl = null,
            usuarioId = 1,
            asignadoA = null,
            fechaCreacion = "2024-01-15T10:30:00Z",
            fechaActualizacion = "2024-01-15T10:30:00Z",
            User = sampleUser
        ),
        Report(
            id = 2,
            titulo = "Semáforo dañado",
            descripcion = "El semáforo no está funcionando correctamente",
            categoriaId = 2,
            ubicacion = "Calle 5 esquina Av. Central",
            latitud = 20.2750,
            longitud = -97.9560,
            estado = "en_proceso",
            prioridad = "media",
            imagenUrl = null,
            usuarioId = 1,
            asignadoA = 1,
            fechaCreacion = "2024-01-14T09:15:00Z",
            fechaActualizacion = "2024-01-15T08:45:00Z",
            User = sampleUser
        )
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    VozurbanaTheme {
        // Preview con datos simulados
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                DashboardHeader(
                    uiState = com.example.vozurbana.presentation.viewmodel.MainUiState(
                        newReportsCount = 3,
                        inProcessReportsCount = 2,
                        resolvedReportsCount = 5,
                        closedReportsCount = 10,
                        rejectedReportsCount = 1
                    )
                )
            }
        }
    }
}
