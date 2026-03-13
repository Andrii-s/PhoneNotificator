package com.example.autodialer.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CallReportDto(
    @SerializedName("phone")              val phone: String,
    @SerializedName("start_time")         val startTime: String,
    @SerializedName("end_time")           val endTime: String,
    @SerializedName("duration_seconds")   val durationSeconds: Long,
    @SerializedName("duration_formatted") val durationFormatted: String
)
