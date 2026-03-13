package com.example.autodialer.data.local.dao

import androidx.room.*
import com.example.autodialer.data.local.entity.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY startTime DESC")
    fun getAllLogs(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CallLogEntity): Long

    @Query("DELETE FROM call_logs")
    suspend fun clearAll()
}
