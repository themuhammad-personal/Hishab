package com.hishab.app

import com.hishab.app.core.Money
import org.junit.Assert.*
import org.junit.Test

class MoneyTest {
    @Test fun parsesBengaliAndEnglish() {
        assertEquals(50000L, Money.parsePoisha("৫০০"))
        assertEquals(12345L, Money.parsePoisha("১২৩.৪৫"))
        assertEquals(100000L, Money.parsePoisha("৳১,০০০"))
        assertNull(Money.parsePoisha("12.345"))
        assertNull(Money.parsePoisha("abc"))
        assertNull(Money.parsePoisha("-1"))
    }
    @Test fun formatsPoisha() {
        assertEquals("৳১,২৩৪.৫৬", Money.formatPoisha(123456))
        assertEquals("৳৫০০", Money.formatPoisha(50000))
        assertEquals("-৳১০", Money.formatPoisha(-1000))
        assertEquals("৳1,234.56", Money.formatPoisha(123456, bengaliDigits = false))
    }
    @Test fun rejectsOutOfRangeAndBadPrecision() {
        assertNull(Money.parsePoisha("1000000000000.00"))
        assertNull(Money.parseQuantityMilli("1.2345"))
        assertEquals(500L, Money.totalPoisha(1500, 333)) // 499.5 poisha rounds half-up
        assertEquals("২.৫", Money.formatQuantity(2500))
    }
    @Test fun quantityTotal() {
        assertEquals(85000L, Money.totalPoisha(Money.parseQuantityMilli("২.৫"), Money.parsePoisha("৩৪০")))
        assertEquals(0L, Money.totalPoisha(1, 50)) // 0.001 × ৳0.50 = 0.05 poisha → 0 poisha
    }
}
