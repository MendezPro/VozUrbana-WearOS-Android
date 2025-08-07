package com.example.vozurbana.data.api

import com.google.gson.annotations.SerializedName

data class ReportStatusRequest(
    @SerializedName("estado")
    val estado: String,

    @SerializedName("asignado_a")
    val asignado_a: Int? = null
)

data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("report")
    val report: T? = null,

    @SerializedName("error")
    val error: String? = null
)
