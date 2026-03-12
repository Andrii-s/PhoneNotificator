package com.example.autodialer.domain.model

data class CallReport(
    val phone: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long
) {
    val durationFormatted: String
        get() {
            val min = durationSeconds / 60
            val sec = durationSeconds % 60
            return "%d:%02d".format(min, sec)
        }
}
