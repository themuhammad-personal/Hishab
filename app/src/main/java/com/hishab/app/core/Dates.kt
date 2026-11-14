package com.hishab.app.core

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Dates {
    private val bnMonths = arrayOf("জানুয়ারি","ফেব্রুয়ারি","মার্চ","এপ্রিল","মে","জুন","জুলাই","আগস্ট","সেপ্টেম্বর","অক্টোবর","নভেম্বর","ডিসেম্বর")
    val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    fun today(zone: ZoneId = ZoneId.systemDefault()): LocalDate = LocalDate.now(zone)
    fun parseDate(value: String?): LocalDate? = try {
        if (value.isNullOrBlank()) null else LocalDate.parse(value, ISO)
    } catch (_: Exception) { null }
    fun requireDate(value: String): LocalDate = parseDate(value) ?: throw IllegalArgumentException("তারিখ YYYY-MM-DD ফরম্যাটে হতে হবে")
    fun parseMonth(key: String): YearMonth? = try { YearMonth.parse(key) } catch (_: Exception) { null }
    fun monthKey(date: LocalDate): String = YearMonth.from(date).toString()
    fun formatMonth(key: String, bengaliDigits: Boolean = true): String {
        val ym = parseMonth(key) ?: return key
        val s = "${bnMonths[ym.monthValue - 1]} ${ym.year}"
        return if (bengaliDigits) Money.toBengaliDigits(s) else s
    }
    fun formatDate(date: String, bengaliDigits: Boolean = true): String {
        val d = parseDate(date) ?: return date
        val s = "${d.dayOfMonth} ${bnMonths[d.monthValue - 1]} ${d.year}"
        return if (bengaliDigits) Money.toBengaliDigits(s) else s
    }
    fun formatDayMonth(date: String, bengaliDigits: Boolean = true): String {
        val d = parseDate(date) ?: return date
        val s = "${d.dayOfMonth} ${bnMonths[d.monthValue - 1]}"
        return if (bengaliDigits) Money.toBengaliDigits(s) else s
    }
    fun relativeLabel(date: String, today: LocalDate = today(), bengaliDigits: Boolean = true): String {
        val d = parseDate(date) ?: return date
        return when (d) {
            today -> if (bengaliDigits) "আজ" else "Today"
            today.minusDays(1) -> if (bengaliDigits) "গতকাল" else "Yesterday"
            today.plusDays(1) -> if (bengaliDigits) "আগামীকাল" else "Tomorrow"
            else -> formatDayMonth(date, bengaliDigits)
        }
    }
    fun daysUntil(date: String, today: LocalDate = today()): Long? = parseDate(date)?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it) }
}
