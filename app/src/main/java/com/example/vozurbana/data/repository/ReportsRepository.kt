package com.example.vozurbana.data.repository

import com.example.vozurbana.data.api.ApiClient
import com.example.vozurbana.data.model.Report
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ReportsRepository {

    private val api = ApiClient.api

    fun getNewReports(): Flow<List<Report>> = flow {
        try {
            val response = api.getNewReports()
            if (response.isSuccessful) {
                emit(response.body() ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            println("Error getting new reports: ${e.message}")
            emit(emptyList())
        }
    }

    suspend fun getReportsCount(): Int {
        return try {
            val response = api.getNewReports()
            if (response.isSuccessful) {
                response.body()?.size ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            println("Error getting reports count: ${e.message}")
            0
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ReportsRepository? = null

        fun getInstance(): ReportsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReportsRepository().also { INSTANCE = it }
            }
        }
    }
}