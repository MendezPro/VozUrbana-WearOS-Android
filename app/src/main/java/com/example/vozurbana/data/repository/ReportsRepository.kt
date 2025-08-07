package com.example.vozurbana.data.repository

import com.example.vozurbana.data.api.AuthManager
import com.example.vozurbana.data.api.VozUrbanaAPI
import com.example.vozurbana.data.api.ReportStatusRequest
import com.example.vozurbana.data.api.ApiResponse
import com.example.vozurbana.data.model.Report
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ReportsRepository {

    // Crear la instancia de la API directamente
    private val api: VozUrbanaAPI by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.0.102:3000/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VozUrbanaAPI::class.java)
    }

    suspend fun getAllReports(): Flow<List<Report>> = flow {
        try {
            val token = AuthManager.getAuthToken()
            if (token.isNullOrEmpty()) {
                throw Exception("Token no disponible")
            }

            val response = api.getAllReports("Bearer $token")
            if (response.isSuccessful) {
                val reports = response.body()
                if (reports != null) {
                    emit(reports)
                } else {
                    emit(emptyList<Report>())
                }
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            println("❌ Error obteniendo reportes: ${e.message}")
            emit(emptyList<Report>())
        }
    }

    suspend fun getReportById(id: Int): Report? {
        return try {
            val token = AuthManager.getAuthToken()
            if (token.isNullOrEmpty()) {
                throw Exception("Token no disponible")
            }

            val response = api.getReportById(id, "Bearer $token")
            if (response.isSuccessful) {
                response.body()
            } else {
                println("❌ Error obteniendo reporte $id: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            println("❌ Error obteniendo reporte $id: ${e.message}")
            null
        }
    }

    suspend fun updateReportStatus(reportId: Int, newStatus: String): Flow<Result<Report>> = flow {
        try {
            val token = AuthManager.getAuthToken()
            if (token.isNullOrEmpty()) {
                println("❌ Token no disponible para actualizar estado")
                throw Exception("Token no disponible")
            }

            println("🔄 Actualizando reporte $reportId a estado: $newStatus")
            println("🔑 Usando token: ${token.take(20)}...")

            val request = ReportStatusRequest(estado = newStatus)
            val response = api.updateReportStatusAdmin(reportId, request, "Bearer $token")

            if (response.isSuccessful) {
                println("✅ Estado actualizado exitosamente")
                val responseBody = response.body()
                println("📋 Respuesta del servidor: ${responseBody?.message}")

                // Crear un reporte actualizado con el nuevo estado
                val updatedReport = Report(
                    id = reportId,
                    titulo = "Reporte actualizado",
                    descripcion = "Estado actualizado a $newStatus",
                    categoriaId = 1,
                    ubicacion = "",
                    latitud = 0.0,
                    longitud = 0.0,
                    estado = newStatus,
                    prioridad = "media",
                    imagenUrl = null,
                    usuarioId = 1,
                    asignadoA = null,
                    fechaCreacion = "",
                    fechaActualizacion = "",
                    User = null
                )
                emit(Result.success(updatedReport))
            } else {
                val errorBody = response.errorBody()?.string()
                println("❌ Error actualizando estado: ${response.code()} - $errorBody")
                emit(Result.failure(Exception("Error actualizando estado: ${response.code()}")))
            }
        } catch (e: Exception) {
            println("❌ Excepción actualizando estado: ${e.message}")
            emit(Result.failure(e))
        }
    }

    suspend fun rejectReport(reportId: Int): Flow<Result<Report>> = flow {
        try {
            val token = AuthManager.getAuthToken()
            if (token.isNullOrEmpty()) {
                throw Exception("Token no disponible")
            }

            println("🔄 Rechazando reporte $reportId")

            // Usar el endpoint específico de actualización de estado con "no_aprobado"
            val request = ReportStatusRequest(estado = "no_aprobado")
            val response = api.updateReportStatusAdmin(reportId, request, "Bearer $token")

            if (response.isSuccessful) {
                println("✅ Reporte rechazado exitosamente")
                val responseBody = response.body()
                println("📋 Respuesta del servidor: ${responseBody?.message}")

                // Crear un reporte actualizado con estado rechazado
                val updatedReport = Report(
                    id = reportId,
                    titulo = "Reporte rechazado",
                    descripcion = "Reporte rechazado por el administrador",
                    categoriaId = 1,
                    ubicacion = "",
                    latitud = 0.0,
                    longitud = 0.0,
                    estado = "no_aprobado",
                    prioridad = "media",
                    imagenUrl = null,
                    usuarioId = 1,
                    asignadoA = null,
                    fechaCreacion = "",
                    fechaActualizacion = "",
                    User = null
                )
                emit(Result.success(updatedReport))
            } else {
                val errorBody = response.errorBody()?.string()
                println("❌ Error rechazando reporte: ${response.code()} - $errorBody")
                emit(Result.failure(Exception("Error rechazando reporte: ${response.code()}")))
            }
        } catch (e: Exception) {
            println("❌ Excepción rechazando reporte: ${e.message}")
            emit(Result.failure(e))
        }
    }

    suspend fun getReportsByStatus(status: String): Flow<List<Report>> = flow {
        try {
            val token = AuthManager.getAuthToken()
            if (token.isNullOrEmpty()) {
                throw Exception("Token no disponible")
            }

            val response = api.getReportsByStatus(status, "Bearer $token")
            if (response.isSuccessful) {
                val reports = response.body()
                if (reports != null) {
                    emit(reports)
                } else {
                    emit(emptyList<Report>())
                }
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            println("❌ Error obteniendo reportes por estado $status: ${e.message}")
            emit(emptyList<Report>())
        }
    }

    suspend fun advanceReportStatus(reportId: Int, currentStatus: String): Flow<Result<Report>> = flow {
        try {
            val nextStatus = when (currentStatus) {
                "nuevo" -> "en_proceso"
                "en_proceso" -> "resuelto"
                "resuelto" -> "cerrado"
                else -> throw Exception("No hay siguiente estado para: $currentStatus")
            }

            println("🔄 Avanzando reporte $reportId de $currentStatus a $nextStatus")

            val token = AuthManager.getAuthToken()
            if (token.isNullOrEmpty()) {
                throw Exception("Token no disponible")
            }

            val request = ReportStatusRequest(estado = nextStatus)
            val response = api.updateReportStatusAdmin(reportId, request, "Bearer $token")

            if (response.isSuccessful) {
                println("✅ Reporte avanzado exitosamente a $nextStatus")
                val responseBody = response.body()
                println("📋 Respuesta del servidor: ${responseBody?.message}")

                // Crear un reporte actualizado
                val updatedReport = Report(
                    id = reportId,
                    titulo = "Reporte avanzado",
                    descripcion = "Estado avanzado de $currentStatus a $nextStatus",
                    categoriaId = 1,
                    ubicacion = "",
                    latitud = 0.0,
                    longitud = 0.0,
                    estado = nextStatus,
                    prioridad = "media",
                    imagenUrl = null,
                    usuarioId = 1,
                    asignadoA = null,
                    fechaCreacion = "",
                    fechaActualizacion = "",
                    User = null
                )
                emit(Result.success(updatedReport))
            } else {
                val errorBody = response.errorBody()?.string()
                println("❌ Error avanzando estado: ${response.code()} - $errorBody")
                emit(Result.failure(Exception("Error avanzando estado: ${response.code()}")))
            }
        } catch (e: Exception) {
            println("❌ Excepción avanzando reporte: ${e.message}")
            emit(Result.failure(e))
        }
    }
}
