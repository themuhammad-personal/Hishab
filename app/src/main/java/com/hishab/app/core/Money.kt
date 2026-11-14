package com.hishab.app.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Integer poisha avoids binary floating-point drift. */
object Money {
    const val MAX_POISHA: Long = 99_999_999_999_99L // ৳99,99,99,99,999.99
    private val bnDigits = charArrayOf('০','১','২','৩','৪','৫','৬','৭','৮','৯')

    fun parsePoisha(input: String): Long? {
        val normalized = normalizeDigits(input).trim()
            .replace("৳", "").replace(",", "").replace(" ", "")
        if (normalized.isEmpty() || normalized.count { it == '.' } > 1) return null
        if (!normalized.matches(Regex("\\d+(\\.\\d{1,2})?"))) return null
        return try {
            val bd = BigDecimal(normalized).movePointRight(2).setScale(0, RoundingMode.UNNECESSARY)
            val value = bd.longValueExact()
            if (value < 0 || value > MAX_POISHA) null else value
        } catch (_: Exception) { null }
    }

    fun parseQuantityMilli(input: String): Long? {
        val normalized = normalizeDigits(input).trim().replace(",", "")
        if (!normalized.matches(Regex("\\d+(\\.\\d{1,3})?"))) return null
        return try {
            val v = BigDecimal(normalized).movePointRight(3).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
            if (v < 0 || v > 999_999_999L) null else v
        } catch (_: Exception) { null }
    }

    fun totalPoisha(quantityMilli: Long?, unitPricePoisha: Long?): Long? {
        if (quantityMilli == null || unitPricePoisha == null) return null
        return try {
            BigDecimal(quantityMilli).multiply(BigDecimal(unitPricePoisha))
                .divide(BigDecimal(1000), 0, RoundingMode.HALF_UP).longValueExact()
                .takeIf { it in 0..MAX_POISHA }
        } catch (_: Exception) { null }
    }

    fun normalizeDigits(input: String): String = buildString {
        input.forEach { c ->
            val i = bnDigits.indexOf(c)
            append(if (i >= 0) '0' + i else c)
        }
    }

    fun toBengaliDigits(input: String): String = buildString {
        input.forEach { c -> append(if (c in '0'..'9') bnDigits[c - '0'] else c) }
    }

    fun formatPoisha(poisha: Long, bengaliDigits: Boolean = true, symbol: Boolean = true): String {
        val negative = poisha < 0
        val abs = kotlin.math.abs(poisha)
        val whole = abs / 100
        val fraction = abs % 100
        val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ','; decimalSeparator = '.' }
        val grouped = DecimalFormat("#,##0", symbols).format(whole)
        val base = if (fraction == 0L) grouped else grouped + "." + fraction.toString().padStart(2, '0')
        val digits = if (bengaliDigits) toBengaliDigits(base) else base
        return (if (negative) "-" else "") + (if (symbol) "৳" else "") + digits
    }

    fun formatQuantity(milli: Long, bengaliDigits: Boolean = true): String {
        val whole = milli / 1000
        val frac = (milli % 1000).toString().padStart(3, '0').trimEnd('0')
        val s = if (frac.isEmpty()) whole.toString() else "$whole.$frac"
        return if (bengaliDigits) toBengaliDigits(s) else s
    }
}
