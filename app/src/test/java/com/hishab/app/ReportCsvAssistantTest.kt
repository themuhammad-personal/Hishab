package com.hishab.app

import com.hishab.app.ai.LocalAssistant
import com.hishab.app.data.local.CategoryEntity
import com.hishab.app.data.local.TransactionEntity
import com.hishab.app.data.local.WalletEntity
import com.hishab.app.domain.CsvExporter
import com.hishab.app.domain.EntryKind
import com.hishab.app.domain.ReportMath
import org.junit.Assert.*
import org.junit.Test

class ReportCsvAssistantTest {
    @Test fun percentagesSum100() {
        val slices = ReportMath.percentLabels(listOf("a" to 10200L, "b" to 4500L, "c" to 2100L, "d" to 1650L))
        assertEquals(100, slices.sumOf { it.percent })
    }
    @Test fun csvEscapesFormula() {
        assertEquals("\"'=cmd\"", CsvExporter.cell("=cmd"))
        assertEquals("\"'@x\"", CsvExporter.cell("@x"))
        assertEquals("\"a\"\"b\"", CsvExporter.cell("a\"b"))
    }
    @Test fun assistantDoesNotWriteAndAsksWhenAmbiguous() {
        val assistant = LocalAssistant()
        val cats = listOf(CategoryEntity("c", "বাজার সদাই", EntryKind.EXPENSE, "", "basket", 0, false, 0, 0))
        val wallets = listOf(WalletEntity("w", "নগদ", "cash", 0, "", "cash", false, 0, 0))
        val reply = assistant.parse("আজ বাজারে ৫০ টাকা খরচ", cats, wallets, emptyList(), "2026-11-14")
        assertNotNull(reply.draft)
        val ambiguous = assistant.parse("বাকি পরিশোধ করো", cats, wallets, emptyList(), "2026-11-14")
        assertNull(ambiguous.draft)
    }
}
