package com.example.autodialer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phone: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val status: String   // "ANSWERED", "NO_ANSWER", "FAILED"
)
