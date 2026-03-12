package com.example.autodialer.data.repository

import com.example.autodialer.data.local.dao.CallLogDao
import com.example.autodialer.data.local.entity.CallLogEntity
import com.example.autodialer.data.remote.ApiService
import com.example.autodialer.data.remote.dto.CallReportDto
import com.example.autodialer.domain.model.CallLog
import com.example.autodialer.domain.model.CallReport
import com.example.autodialer.domain.model.CallStatus
import com.example.autodialer.domain.repository.CallRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRepositoryImpl @Inject constructor(
    private val callLogDao: CallLogDao,
    private val apiService: ApiService
) : CallRepository {

    private val iso8601Formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /** Returns a live [Flow] of all call logs ordered by most-recent first. */
    override fun getCallLogs(): Flow<List<CallLog>> =
        callLogDao.getAllLogs().map { entities ->
            entities.map { it.toDomain() }
        }

    /** Persists a [CallLog] to Room. */
    override suspend fun saveCallLog(callLog: CallLog) {
        callLogDao.insert(callLog.toEntity())
    }

    /**
     * Sends a [CallReport] to the remote API.
     * Timestamps are serialised as ISO 8601 strings (UTC).
     *
     * @return [Result.success] on HTTP 2xx; [Result.failure] with the
     *         underlying exception otherwise.
     */
    override suspend fun sendCallReport(report: CallReport): Result<Unit> {
        return try {
            val dto = CallReportDto(
                phone             = report.phone,
                startTime         = iso8601Formatter.format(Date(report.startTime)),
                endTime           = iso8601Formatter.format(Date(report.endTime)),
                durationSeconds   = report.durationSeconds,
                durationFormatted = report.durationFormatted
            )
            val response = apiService.sendCallReport(dto)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception("Send report failed: HTTP ${response.code()} ${response.message()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error sending call report: ${e.message}", e))
        }
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private fun CallLogEntity.toDomain() = CallLog(
        id              = id,
        phone           = phone,
        startTime       = startTime,
        endTime         = endTime,
        durationSeconds = durationSeconds,
        status          = CallStatus.entries.firstOrNull { it.name == status } ?: CallStatus.FAILED
    )

    private fun CallLog.toEntity() = CallLogEntity(
        id              = id,
        phone           = phone,
        startTime       = startTime,
        endTime         = endTime,
        durationSeconds = durationSeconds,
        status          = status.name
    )
}
