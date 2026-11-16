package com.serratocreations.phovo.feature.photos.util

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewerDateFormatterTest {

    @Test
    fun date_inCurrentYear_omitsYear() {
        assertEquals("May 2", LocalDateTime(2026, 5, 2, 10, 31).toViewerDateString(currentYear = 2026))
    }

    @Test
    fun date_inAnotherYear_includesYear() {
        assertEquals(
            "Nov 15, 2024",
            LocalDateTime(2024, 11, 15, 10, 31).toViewerDateString(currentYear = 2026)
        )
    }

    @Test
    fun time_morning() {
        assertEquals("10:31 AM", LocalDateTime(2026, 5, 2, 10, 31).toViewerTimeString())
    }

    @Test
    fun time_afternoon_padsMinutesNotHours() {
        assertEquals("3:05 PM", LocalDateTime(2026, 5, 2, 15, 5).toViewerTimeString())
    }

    @Test
    fun time_midnightAndNoon_showTwelve() {
        assertEquals("12:00 AM", LocalDateTime(2026, 5, 2, 0, 0).toViewerTimeString())
        assertEquals("12:00 PM", LocalDateTime(2026, 5, 2, 12, 0).toViewerTimeString())
    }
}
