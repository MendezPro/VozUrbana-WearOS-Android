package com.example.vozurbana.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.*
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.vozurbana.data.model.Report
import com.example.vozurbana.data.model.User
import com.example.vozurbana.presentation.theme.VozurbanaTheme
import com.example.vozurbana.presentation.viewmodel.MainViewModel
import com.example.vozurbana.service.NotificationService

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        // Inicializar ViewModel con contexto
        viewModel.initializeNotificationService(this)

        setContent {
            WearApp(viewModel)
        }

        println("🚀 MainActivity y servicio de notificaciones iniciados en Wear OS")
    }
}

@Composable
fun WearApp(viewModel: MainViewModel) {
    VozurbanaTheme {
        // Usar estados reales del ViewModel en lugar de datos simulados
        val uiState by viewModel.uiState.collectAsState()
        val reports by viewModel.newReports.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingScreen()
                }
                uiState.error != null -> {
                    // Solución para el smart cast error
                    val errorMessage = uiState.error
                    ErrorScreen(
                        error = errorMessage ?: "Error desconocido",
                        onRetry = { viewModel.refreshReports() }
                    )
                }
                else -> {
                    MainScreen(
                        reportsCount = uiState.reportsCount,
                        reports = reports, // Usar reportes reales
                        onRefresh = { viewModel.refreshReports() }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    reportsCount: Int,
    reports: List<Report>,
    onRefresh: () -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Indicador de reportes pendientes
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (reportsCount > 0) Color.Red else Color.Green
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = reportsCount.toString(),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (reportsCount > 0) "Reportes Pendientes" else "Sin Reportes",
                    color = MaterialTheme.colors.onBackground,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Mostrar reportes reales si existen
        if (reports.isNotEmpty()) {
            items(reports.take(3)) { report -> // Mostrar máximo 3 reportes
                ReportCard(
                    report = report,
                    onClick = {
                        // TODO: Implementar navegación a detalle del reporte
                        println("Clicked on report: ${report.titulo}")
                    }
                )
            }
        } else if (reportsCount == 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    onClick = { /* No action for empty state */ }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "✅ No hay reportes pendientes",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Green,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Todas las incidencias han sido resueltas",
                            fontSize = 10.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Actualizar")
            }
        }
    }
}

@Composable
fun ReportCard(
    report: Report,
    onClick: () -> Unit = {} // Parámetro onClick agregado con valor por defecto
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = report.titulo,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = report.prioridad.uppercase(),
                    fontSize = 10.sp,
                    color = when (report.prioridad) {
                        "alta" -> Color.Red
                        "media" -> Color.Yellow
                        else -> Color.Green
                    }
                )

                Text(
                    text = report.estado.replace("_", " ").uppercase(),
                    fontSize = 10.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            // Mostrar información adicional del reporte
            if (report.User != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Por: ${report.User.nombre}",
                    fontSize = 9.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
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
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cargando reportes...",
                color = MaterialTheme.colors.onBackground,
                fontSize = 12.sp
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
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "⚠️ Error",
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = error,
                color = MaterialTheme.colors.onBackground,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reintentar")
            }
        }
    }
}

// Función para preview con datos simulados
fun getSampleReports(): List<Report> {
    val sampleUser = User(1, "Usuario Demo", "demo@example.com")
    return listOf(
        Report(
            id = 1,
            titulo = "Bache en Avenida Principal",
            descripcion = "Bache peligroso que necesita reparación",
            categoriaId = 1,
            ubicacion = "Avenida Principal #123",
            latitud = 19.4326,
            longitud = -99.1332,
            estado = "pendiente",
            prioridad = "alta",
            imagenUrl = null,
            usuarioId = 1,
            asignadoA = null,
            fechaCreacion = "2025-07-30T10:00:00Z",
            fechaActualizacion = "2025-07-30T10:00:00Z",
            User = sampleUser
        )
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    VozurbanaTheme {
        MainScreen(
            reportsCount = 0,
            reports = emptyList(),
            onRefresh = {}
        )
    }
}