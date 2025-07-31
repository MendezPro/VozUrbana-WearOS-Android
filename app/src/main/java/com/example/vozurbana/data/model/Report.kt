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
    val estado: String,
    val prioridad: String,
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