package com.hishab.app

import com.hishab.app.data.local.*
import com.hishab.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class AccountingTest {
    private val ts = 0L
    private fun tx(id: String, kind: EntryKind, amount: Long, paid: Long, date: String = "2026-11-14", due: DueDirection? = null) =
        TransactionEntity(id = id, kind = kind, title = id, amountPoisha = amount, paidPoisha = paid, categoryId = "cat", walletId = "w1", date = date, dueDirection = due, dueDate = if (due != null) date else null, createdAt = ts, updatedAt = ts)
    private fun pay(id: String, tx: String, amount: Long, wallet: String = "w1") = DuePaymentEntity(id, tx, wallet, amount, "2026-11-14", null, ts)
    private val wallets = listOf(WalletEntity("w1", "cash", "cash", 90000, "", "", false, ts, ts), WalletEntity("w2", "bank", "bank", 0, "", "", false, ts, ts))

    @Test fun dualAccounting() {
        val txs = listOf(tx("i", EntryKind.INCOME, 3500000, 3500000), tx("e1", EntryKind.EXPENSE, 1520000, 1520000), tx("e2", EntryKind.EXPENSE, 325000, 0, due = DueDirection.PAYABLE))
        val pays = listOf(pay("p1", "i", 3500000), pay("p2", "e1", 1520000))
        val s = Accounting.monthSummary("2026-11", txs, pays)
        assertEquals(1845000L, s.expenseTotal)
        assertEquals(1520000L, s.expensePaid)
        assertEquals(325000L, s.expenseUnpaid)
        assertEquals(325000L, s.payable)
        val balances = Accounting.walletBalances(wallets, txs, pays, emptyList())
        assertEquals(2070000L, balances.first { it.walletId == "w1" }.balance)
    }
    @Test fun transferNeutral() {
        val transfers = listOf(TransferEntity("t", "w1", "w2", 10000, "2026-11-14", null, ts))
        val b = Accounting.walletBalances(wallets, emptyList(), emptyList(), transfers)
        assertEquals(80000L, b.first { it.walletId == "w1" }.balance)
        assertEquals(10000L, b.first { it.walletId == "w2" }.balance)
        assertEquals(90000L, b.sumOf { it.balance })
    }
    @Test fun partialPaymentDoesNotChangeMonthExpense() {
        val txs = listOf(tx("e", EntryKind.EXPENSE, 300000, 75000, due = DueDirection.PAYABLE))
        val before = Accounting.monthSummary("2026-11", txs, listOf(pay("p", "e", 75000)))
        val after = Accounting.monthSummary("2026-11", txs, listOf(pay("p", "e", 75000), pay("p2", "e", 50000)))
        assertEquals(before.expenseTotal, after.expenseTotal)
        assertEquals(225000L, before.payable)
        assertEquals(175000L, after.payable)
    }
    @Test fun budgetWarnings() {
        assertEquals(BudgetWarning.EIGHTY, BudgetState(10000, 8000).warning)
        assertEquals(BudgetWarning.FULL, BudgetState(10000, 10000).warning)
        assertEquals(BudgetWarning.FULL, BudgetState(10000, 12000).warning)
        assertEquals(BudgetWarning.NONE, BudgetState(10000, 7999).warning)
    }
}
