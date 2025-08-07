package com.example.vozurbana.data.model

import com.google.gson.annotations.SerializedName

data class Report(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    @SerializedName("categoria_id") val categoriaId: Int,
    val ubicacion: String?,
    val latitud: Double,
    val longitud: Double,
    val estado: String, // nuevo, en_proceso, resuelto, cerrado, no_aprobado
    val prioridad: String, // baja, media, alta
    @SerializedName("imagen_url") val imagenUrl: String?,
    @SerializedName("usuario_id") val usuarioId: Int,
    @SerializedName("asignado_a") val asignadoA: Int?,
    @SerializedName("fecha_creacion") val fechaCreacion: String,
    @SerializedName("fecha_actualizacion") val fechaActualizacion: String,
    val User: User?
)

data class User(
    val id: Int,
    val nombre: String,
    val email: String
)

data class NotificationData(
    val reportId: Int,
    val titulo: String,
    val prioridad: String,
    val fechaCreacion: String
)

// Nuevos modelos para manejo de estados
data class UpdateStatusRequest(
    val estado: String,
    @SerializedName("asignado_a") val asignadoA: Int? = null
)

data class UpdateStatusResponse(
    val message: String,
    val report: Report
)

// Enum para los estados del reporte
enum class ReportStatus(val value: String, val displayName: String) {
    NUEVO("nuevo", "Nuevo"),
    EN_PROCESO("en_proceso", "En Proceso"),
    RESUELTO("resuelto", "Resuelto"),
    CERRADO("cerrado", "Cerrado"),
    NO_APROBADO("no_aprobado", "Rechazado");

    companion object {
        fun fromValue(value: String): ReportStatus? {
            return values().find { it.value == value }
        }

        fun getNextStatus(currentStatus: String): ReportStatus? {
            return when (currentStatus) {
                "nuevo" -> EN_PROCESO
                "en_proceso" -> RESUELTO
                "resuelto" -> CERRADO
                else -> null
            }
        }
    }
}

// Enum para prioridades
enum class ReportPriority(val value: String, val displayName: String, val color: String) {
    BAJA("baja", "Baja", "#28a745"),
    MEDIA("media", "Media", "#ffc107"),
    ALTA("alta", "Alta", "#dc3545");

    companion object {
        fun fromValue(value: String): ReportPriority? {
            return values().find { it.value == value }
        }
    }
}
