package com.hishab.app

import com.hishab.app.core.Dates
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DatesTest {
    @Test fun strictIsoValidation() {
        assertEquals(LocalDate.of(2026, 11, 14), Dates.requireDate("2026-11-14"))
        assertNull(Dates.parseDate("2026-02-30"))
        assertNull(Dates.parseDate("14-11-2026"))
        assertNull(Dates.parseDate(""))
        assertThrows(IllegalArgumentException::class.java) { Dates.requireDate("2026-13-01") }
    }
    @Test fun monthAndRelativeLabels() {
        val today = LocalDate.of(2026, 11, 14)
        assertEquals("২০৬-১১", com.hishab.app.core.Money.toBengaliDigits(Dates.monthKey(today)))
        assertEquals("নভেম্বর ২০২৬", Dates.formatMonth("2026-11", false))
        assertEquals("আজ", Dates.relativeLabel("2026-11-14", today))
        assertEquals("গতকাল", Dates.relativeLabel("2026-11-13", today))
        assertEquals("আগামীকাল", Dates.relativeLabel("2026-11-15", today))
        assertEquals("Today", Dates.relativeLabel("2026-11-14", today, false))
        assertEquals(1L, Dates.daysUntil("2026-11-15", today))
    }
    @Test fun deviceTimezoneIsUsed() {
        val dhaka = ZoneId.of("Asia/Dhaka")
        assertEquals(LocalDate.now(dhaka), Dates.today(dhaka))
    }
}
