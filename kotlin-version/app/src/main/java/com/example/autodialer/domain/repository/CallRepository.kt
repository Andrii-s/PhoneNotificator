package com.example.autodialer.domain.repository

import com.example.autodialer.domain.model.CallLog
import com.example.autodialer.domain.model.CallReport
import kotlinx.coroutines.flow.Flow

interface CallRepository {
    fun getCallLogs(): Flow<List<CallLog>>
    suspend fun saveCallLog(callLog: CallLog)
    suspend fun sendCallReport(report: CallReport): Result<Unit>
}
