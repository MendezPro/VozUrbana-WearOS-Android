package com.example.vozurbana.data.api

import com.example.vozurbana.data.model.Report
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface VozUrbanaAPI {

    @GET("reports")
    suspend fun getReports(
        @Query("estado") estado: String? = null
    ): Response<List<Report>>

    @GET("reports")
    suspend fun getNewReports(
        @Query("estado") estado: String = "pendiente"
    ): Response<List<Report>>

    @GET("reports")
    suspend fun getPendingReports(
        @Query("estado") estado: String = "pendiente"
    ): Response<List<Report>>
}