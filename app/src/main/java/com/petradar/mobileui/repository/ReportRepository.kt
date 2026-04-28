package com.petradar.mobileui.repository

import com.petradar.mobileui.api.ReportCreateModel
import com.petradar.mobileui.api.ReportUpdateModel
import com.petradar.mobileui.api.ReportViewModel
import com.petradar.mobileui.api.RetrofitClient
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

    /** GET /api/Reports/{id} */
    suspend fun getById(id: Long): Response<ReportViewModel> = api.getReportById(id)

    /** PUT /api/Reports/{id} */
    suspend fun update(id: Long, request: ReportUpdateModel): Response<Unit> = api.updateReport(id, request)

    /** DELETE /api/Reports/{id} */
    suspend fun delete(id: Long): Response<Unit> = api.deleteReport(id)
}

