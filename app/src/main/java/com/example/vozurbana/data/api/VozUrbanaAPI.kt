package com.example.vozurbana.data.api

import com.example.vozurbana.data.model.Report
import retrofit2.Response
import retrofit2.http.*

interface VozUrbanaAPI {

    @GET("reports")
    suspend fun getAllReports(
        @Header("Authorization") authorization: String
    ): Response<List<Report>>

    @GET("reports/{id}")
    suspend fun getReportById(
        @Path("id") id: Int,
        @Header("Authorization") authorization: String
    ): Response<Report>

    @PATCH("reports/admin/status/{id}")
    suspend fun updateReportStatusAdmin(
        @Path("id") reportId: Int,
        @Body request: ReportStatusRequest,
        @Header("Authorization") authorization: String
    ): Response<ApiResponse<Report>>

    @PUT("reports/{id}")
    suspend fun updateReport(
        @Path("id") id: Int,
        @Body report: Report,
        @Header("Authorization") authorization: String
    ): Response<ApiResponse<Report>>

    @DELETE("reports/{id}")
    suspend fun deleteReport(
        @Path("id") id: Int,
        @Header("Authorization") authorization: String
    ): Response<ApiResponse<Any>>

    // Endpoint específico para rechazar reportes
    @PATCH("reports/admin/reject/{id}")
    suspend fun rejectReport(
        @Path("id") reportId: Int,
        @Header("Authorization") authorization: String
    ): Response<ApiResponse<Report>>

    // Endpoint para obtener reportes por estado
    @GET("reports/status/{status}")
    suspend fun getReportsByStatus(
        @Path("status") status: String,
        @Header("Authorization") authorization: String
    ): Response<List<Report>>
}
