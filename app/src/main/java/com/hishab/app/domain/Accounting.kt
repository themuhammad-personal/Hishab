package com.hishab.app.domain

import com.hishab.app.data.local.CategoryBudgetEntity
import com.hishab.app.data.local.CategoryEntity
import com.hishab.app.data.local.DuePaymentEntity
import com.hishab.app.data.local.TransactionEntity
import com.hishab.app.data.local.TransferEntity
import com.hishab.app.data.local.WalletEntity
import com.hishab.app.core.Dates

object Accounting {
    fun monthSummary(monthKey: String, transactions: List<TransactionEntity>, payments: List<DuePaymentEntity>): MonthSummary {
        val txs = transactions.filter { it.deletedAt == null && Dates.monthKey(Dates.requireDate(it.date)) == monthKey }
        val paidByTx = payments.groupBy { it.transactionId }.mapValues { e -> e.value.sumOf { it.amountPoisha } }
        fun paid(t: TransactionEntity) = paidByTx[t.id] ?: 0L
        val expenses = txs.filter { it.kind == EntryKind.EXPENSE }
        val incomes = txs.filter { it.kind == EntryKind.INCOME }
        val expenseTotal = expenses.sumOf { it.amountPoisha }
        val expensePaid = expenses.sumOf { paid(it).coerceAtMost(it.amountPoisha) }
        val incomeTotal = incomes.sumOf { it.amountPoisha }
        val incomeReceived = incomes.sumOf { paid(it).coerceAtMost(it.amountPoisha) }
        return MonthSummary(
            monthKey, expenseTotal, expensePaid, expenseTotal - expensePaid,
            incomeTotal, incomeReceived, incomeTotal - incomeReceived,
            payable = expenses.filter { it.dueDirection == DueDirection.PAYABLE }.sumOf { (it.amountPoisha - paid(it)).coerceAtLeast(0) },
            receivable = incomes.filter { it.dueDirection == DueDirection.RECEIVABLE }.sumOf { (it.amountPoisha - paid(it)).coerceAtLeast(0) }
        )
    }

    fun walletBalances(wallets: List<WalletEntity>, transactions: List<TransactionEntity>, payments: List<DuePaymentEntity>, transfers: List<TransferEntity>): List<WalletBalance> {
        val txKind = transactions.filter { it.deletedAt == null }.associate { it.id to it.kind }
        return wallets.map { w ->
            var balance = w.openingBalancePoisha
            payments.forEach { p ->
                when (txKind[p.transactionId]) {
                    EntryKind.INCOME -> if (p.walletId == w.id) balance += p.amountPoisha
                    EntryKind.EXPENSE -> if (p.walletId == w.id) balance -= p.amountPoisha
                    null -> Unit
                }
            }
            transfers.filter { it.deletedAt == null }.forEach { t ->
                if (t.fromWalletId == w.id) balance -= t.amountPoisha
                if (t.toWalletId == w.id) balance += t.amountPoisha
            }
            WalletBalance(w.id, balance)
        }
    }

    fun categorySpend(monthKey: String, transactions: List<TransactionEntity>, payments: List<DuePaymentEntity>): List<CategorySpend> {
        val paidByTx = payments.groupBy { it.transactionId }.mapValues { e -> e.value.sumOf { it.amountPoisha } }
        return transactions.filter { it.deletedAt == null && it.kind == EntryKind.EXPENSE && Dates.monthKey(Dates.requireDate(it.date)) == monthKey }
            .groupBy { it.categoryId }.map { (id, list) ->
                val total = list.sumOf { it.amountPoisha }
                val paid = list.sumOf { (paidByTx[it.id] ?: 0L).coerceAtMost(it.amountPoisha) }
                CategorySpend(id, total, paid, total - paid)
            }.sortedByDescending { it.amount }
    }

    fun itemInsights(categoryId: String, monthKey: String, transactions: List<TransactionEntity>): List<ItemInsight> =
        transactions.filter { it.deletedAt == null && it.categoryId == categoryId && Dates.monthKey(Dates.requireDate(it.date)) == monthKey }
            .groupBy { it.title.trim().ifBlank { "শিরোনামহীন" } }
            .map { (title, list) ->
                val total = list.sumOf { it.amountPoisha }
                ItemInsight(title, list.size, total, if (list.isEmpty()) 0 else total / list.size)
            }.sortedByDescending { it.amount }

    fun overallBudget(monthKey: String, limit: Long, transactions: List<TransactionEntity>, payments: List<DuePaymentEntity>): BudgetState =
        BudgetState(limit, monthSummary(monthKey, transactions, payments).expenseTotal)

    fun categoryBudget(monthKey: String, category: CategoryEntity, limit: Long, transactions: List<TransactionEntity>, payments: List<DuePaymentEntity>): BudgetState {
        val spent = categorySpend(monthKey, transactions, payments).firstOrNull { it.categoryId == category.id }?.amount ?: 0L
        return BudgetState(limit, spent)
    }
}
