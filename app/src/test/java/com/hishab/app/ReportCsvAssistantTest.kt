package com.hishab.app

import com.hishab.app.ai.LocalAssistant
import com.hishab.app.data.local.CategoryEntity
import com.hishab.app.data.local.TransactionEntity
import com.hishab.app.data.local.WalletEntity
import com.hishab.app.domain.CsvExporter
import com.hishab.app.domain.EntryKind
import com.hishab.app.data.local.DuePaymentEntity
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
    @Test fun csvHasBomAnd17Columns() {
        val csv = CsvExporter.transactions(emptyList(), emptyList(), emptyList())
        assertTrue(csv.startsWith("\uFEFF"))
        assertEquals(17, csv.lineSequence().first().split("\",\"").size)
        assertEquals("\"'\t=cmd\"", CsvExporter.cell("\t=cmd"))
    }
    @Test fun assistantDoesNotWriteAndAsksWhenAmbiguous() {
        val assistant = LocalAssistant()
        val cats = listOf(CategoryEntity("c", "বাজার সদাই", EntryKind.EXPENSE, "", "basket", 0, false, 0, 0))
        val wallets = listOf(WalletEntity("w", "নগদ", "cash", 0, "", "cash", false, 0, 0))
        val reply = assistant.parse("আজ বাজারে ৫০ টাকা খরচ", cats, wallets, emptyList(), "2026-11-14")
        assertNotNull(reply.draft)
        val ambiguous = assistant.parse("বাকি পরিশোধ করো", cats, wallets, emptyList(), "2026-11-14")
        assertNull(ambiguous.draft)
        val manyWallets = wallets + WalletEntity("w2", "বিকাশ", "bkash", 0, "", "smartphone", false, 0, 0)
        assertNull(assistant.parse("আজ বাজারে ৫০ টাকা খরচ", cats, manyWallets, emptyList(), "2026-11-14").draft)
        assertNull(assistant.parse("আজ ৫০ টাকা খরচ", cats + CategoryEntity("c2", "যাতায়াত", EntryKind.EXPENSE, "", "bus", 1, false, 0, 0), wallets, emptyList(), "2026-11-14").draft)
        assertNull(assistant.parse("আজ বাজারে ৫০ টাকা অথবা ৬০ টাকা খরচ", cats, wallets, emptyList(), "2026-11-14").draft)
    }
    @Test fun assistantPayAndDeleteRequireUniqueTarget() {
        val assistant = LocalAssistant()
        val cats = listOf(CategoryEntity("c", "বাজার সদাই", EntryKind.EXPENSE, "", "basket", 0, false, 0, 0))
        val wallets = listOf(WalletEntity("w", "নগদ", "cash", 0, "", "cash", false, 0, 0))
        fun tx(id: String, title: String, shop: String) = TransactionEntity(id = id, kind = EntryKind.EXPENSE, title = title, amountPoisha = 10000, paidPoisha = 2000, categoryId = "c", walletId = "w", date = "2026-11-01", dueDirection = com.hishab.app.domain.DueDirection.PAYABLE, dueDate = "2026-11-20", shopOrPerson = shop, createdAt = 0, updatedAt = 0)
        val txs = listOf(tx("t1", "বাজার বাকি", "রহিম স্টোর"), tx("t2", "বাজার বাকি", "করিম স্টোর"))
        assertNull(assistant.parse("বাজার বাকি পরিশোধ করো", cats, wallets, txs, "2026-11-14").draft)
        val pay = assistant.parse("রহিম স্টোরের ৫০ টাকা পরিশোধ করো", cats, wallets, txs, "2026-11-14")
        assertTrue(pay.draft is com.hishab.app.ai.AssistantDraft.PayDue)
        val delete = assistant.parse("করিম স্টোরের হিসাব মুছে দাও", cats, wallets, txs, "2026-11-14")
        assertTrue(delete.draft is com.hishab.app.ai.AssistantDraft.DeleteTransaction)
        assertEquals(2, txs.size) // parser did not mutate input ledger
    }
}
