package com.example.autodialer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CallReportTest {

    // -------------------------------------------------------------------------
    // durationFormatted
    // -------------------------------------------------------------------------

    @Test
    fun `durationFormatted formats seconds correctly`() {
        val report = CallReport(phone = "", startTime = 0L, endTime = 95_000L, durationSeconds = 95L)
        assertEquals("1:35", report.durationFormatted)
    }

    @Test
    fun `durationFormatted pads single-digit seconds with zero`() {
        val report = CallReport(phone = "", startTime = 0L, endTime = 65_000L, durationSeconds = 65L)
        assertEquals("1:05", report.durationFormatted)
    }

    @Test
    fun `durationFormatted handles zero duration`() {
        val report = CallReport(phone = "", startTime = 0L, endTime = 0L, durationSeconds = 0L)
        assertEquals("0:00", report.durationFormatted)
    }

    @Test
    fun `durationFormatted handles exactly one minute`() {
        val report = CallReport(phone = "", startTime = 0L, endTime = 60_000L, durationSeconds = 60L)
        assertEquals("1:00", report.durationFormatted)
    }

    @Test
    fun `durationFormatted handles large values spanning multiple hours`() {
        // 3600 seconds = 60 minutes, formatted as 60:00
        val report = CallReport(phone = "", startTime = 0L, endTime = 3_600_000L, durationSeconds = 3600L)
        assertEquals("60:00", report.durationFormatted)
    }

    @Test
    fun `durationFormatted handles 59 seconds`() {
        val report = CallReport(phone = "", startTime = 0L, endTime = 59_000L, durationSeconds = 59L)
        assertEquals("0:59", report.durationFormatted)
    }

    @Test
    fun `durationFormatted handles one second`() {
        val report = CallReport(phone = "", startTime = 0L, endTime = 1_000L, durationSeconds = 1L)
        assertEquals("0:01", report.durationFormatted)
    }

    // -------------------------------------------------------------------------
    // Data class properties
    // -------------------------------------------------------------------------

    @Test
    fun `CallReport stores phone correctly`() {
        val report = CallReport(phone = "+380991234567", startTime = 0L, endTime = 0L, durationSeconds = 0L)
        assertEquals("+380991234567", report.phone)
    }

    @Test
    fun `CallReport equality for identical instances`() {
        val r1 = CallReport(phone = "+380991234567", startTime = 1000L, endTime = 61_000L, durationSeconds = 60L)
        val r2 = CallReport(phone = "+380991234567", startTime = 1000L, endTime = 61_000L, durationSeconds = 60L)
        assertEquals(r1, r2)
    }

    @Test
    fun `CallReport copy preserves all fields`() {
        val original = CallReport(phone = "+380991234567", startTime = 1000L, endTime = 61_000L, durationSeconds = 60L)
        val copy = original.copy(phone = "+380997654321")
        assertEquals("+380997654321", copy.phone)
        assertEquals(original.startTime, copy.startTime)
        assertEquals(original.durationSeconds, copy.durationSeconds)
    }
}
