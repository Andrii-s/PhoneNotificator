package com.example.autodialer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CallLogTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun callLog(
        durationSeconds: Long,
        status: CallStatus = CallStatus.ANSWERED,
    ) = CallLog(
        id = 1L,
        phone = "+380991234567",
        startTime = 0L,
        endTime = durationSeconds * 1_000L,
        durationSeconds = durationSeconds,
        status = status,
    )

    // -------------------------------------------------------------------------
    // durationFormatted
    // -------------------------------------------------------------------------

    @Test
    fun `durationFormatted formats seconds correctly`() {
        assertEquals("1:35", callLog(95L).durationFormatted)
    }

    @Test
    fun `durationFormatted pads single-digit seconds with zero`() {
        assertEquals("1:05", callLog(65L).durationFormatted)
    }

    @Test
    fun `durationFormatted handles zero duration`() {
        assertEquals("0:00", callLog(0L, CallStatus.NO_ANSWER).durationFormatted)
    }

    @Test
    fun `durationFormatted handles exactly one minute`() {
        assertEquals("1:00", callLog(60L).durationFormatted)
    }

    @Test
    fun `durationFormatted handles large values`() {
        // 3600 s = 60 min → "60:00"
        assertEquals("60:00", callLog(3600L).durationFormatted)
    }

    @Test
    fun `durationFormatted handles 59 seconds`() {
        assertEquals("0:59", callLog(59L).durationFormatted)
    }

    @Test
    fun `durationFormatted handles one second`() {
        assertEquals("0:01", callLog(1L).durationFormatted)
    }

    // -------------------------------------------------------------------------
    // CallStatus.displayName
    // -------------------------------------------------------------------------

    @Test
    fun `CallStatus ANSWERED has correct Ukrainian displayName`() {
        assertEquals("Відповіли", CallStatus.ANSWERED.displayName)
    }

    @Test
    fun `CallStatus NO_ANSWER has correct Ukrainian displayName`() {
        assertEquals("Без відповіді", CallStatus.NO_ANSWER.displayName)
    }

    @Test
    fun `CallStatus FAILED has correct Ukrainian displayName`() {
        assertEquals("Помилка", CallStatus.FAILED.displayName)
    }

    @Test
    fun `all CallStatus values have non-blank displayNames`() {
        CallStatus.entries.forEach { status ->
            assert(status.displayName.isNotBlank()) {
                "displayName for $status must not be blank"
            }
        }
    }

    @Test
    fun `all CallStatus displayNames are unique`() {
        val names = CallStatus.entries.map { it.displayName }
        assertEquals("Expected all displayNames to be unique", names.size, names.toSet().size)
    }

    // -------------------------------------------------------------------------
    // Data class behaviour
    // -------------------------------------------------------------------------

    @Test
    fun `CallLog equality based on all fields`() {
        val log1 = callLog(60L)
        val log2 = callLog(60L)
        assertEquals(log1, log2)
    }

    @Test
    fun `CallLog instances with different status are not equal`() {
        val answered = callLog(60L, CallStatus.ANSWERED)
        val failed   = callLog(60L, CallStatus.FAILED)
        assertNotEquals(answered, failed)
    }

    @Test
    fun `CallLog copy updates only specified field`() {
        val original = callLog(60L, CallStatus.ANSWERED)
        val copy = original.copy(status = CallStatus.NO_ANSWER)
        assertEquals(CallStatus.NO_ANSWER, copy.status)
        assertEquals(original.phone, copy.phone)
        assertEquals(original.durationSeconds, copy.durationSeconds)
    }

    @Test
    fun `CallLog default id is 0`() {
        val log = CallLog(
            phone = "+380991234567",
            startTime = 0L,
            endTime = 0L,
            durationSeconds = 0L,
            status = CallStatus.NO_ANSWER,
        )
        assertEquals(0L, log.id)
    }
}
