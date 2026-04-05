package com.example.petradar.repository

import com.example.petradar.api.ReportCreateModel
import com.example.petradar.api.ReportViewModel
import com.example.petradar.api.RetrofitClient
import retrofit2.Response

/** Repository for Reports API operations. */
class ReportRepository {

    private val api = RetrofitClient.apiService

    /** POST /api/Reports */
    suspend fun create(request: ReportCreateModel): Response<Unit> =
        api.createReport(request)

    /** GET /api/Reports/user/{userId} */
    suspend fun getByUserId(userId: Long): Response<List<ReportViewModel>> =
        api.getReportsByUserId(userId)
}

