package com.example.petradar.repository

import com.example.petradar.api.ReportCreateModel
import com.example.petradar.api.ReportViewModel
import com.example.petradar.api.RetrofitClient
import okhttp3.MultipartBody
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

    /** PUT /api/Reports/{id}/mainpicture */
    suspend fun uploadMainPicture(reportId: Long, filePart: MultipartBody.Part): Response<Unit> =
        api.uploadReportMainPicture(reportId, filePart)

    suspend fun getAdditionalPhotos(reportId: Long): Response<List<String>> =
        api.getReportAdditionalPhotos(reportId)

    suspend fun uploadAdditionalPhotos(reportId: Long, files: List<MultipartBody.Part>): Response<Unit> =
        api.uploadReportAdditionalPhotos(reportId, files)

    suspend fun deleteAdditionalPhoto(reportId: Long, photoName: String): Response<Unit> =
        api.deleteReportAdditionalPhoto(reportId, photoName)
}

