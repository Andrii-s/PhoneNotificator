package com.example.autodialer.domain.model

data class CallLog(
    val id: Long = 0,
    val phone: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val status: CallStatus
) {
    val durationFormatted: String
        get() {
            val min = durationSeconds / 60
            val sec = durationSeconds % 60
            return "%d:%02d".format(min, sec)
        }
}

enum class CallStatus(val displayName: String) {
    ANSWERED("Відповіли"),
    NO_ANSWER("Без відповіді"),
    FAILED("Помилка")
}
