package com.example.autodialer.data.remote

import com.example.autodialer.data.remote.dto.CallReportDto
import com.example.autodialer.data.remote.dto.DebtorDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("debetors")
    suspend fun getDebtors(@Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap()): Response<List<DebtorDto>>

    @POST("debetor_report")
    suspend fun sendCallReport(@Body report: CallReportDto): Response<Unit>
}
