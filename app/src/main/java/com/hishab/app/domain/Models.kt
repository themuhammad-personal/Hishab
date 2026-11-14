package com.hishab.app.domain

enum class EntryKind { INCOME, EXPENSE }
enum class DueDirection { PAYABLE, RECEIVABLE }
enum class PaymentStatus { PAID, PARTIAL, UNPAID }
enum class TodoPriority { LOW, NORMAL, HIGH }
enum class ThemeMode { LIGHT, DARK, SYSTEM }

fun paymentStatus(amount: Long, paid: Long): PaymentStatus = when {
    paid <= 0 -> PaymentStatus.UNPAID
    paid >= amount -> PaymentStatus.PAID
    else -> PaymentStatus.PARTIAL
}

data class MonthSummary(
    val monthKey: String,
    val expenseTotal: Long,
    val expensePaid: Long,
    val expenseUnpaid: Long,
    val incomeTotal: Long,
    val incomeReceived: Long,
    val incomeUnreceived: Long,
    val payable: Long,
    val receivable: Long
)

data class WalletBalance(val walletId: String, val balance: Long)
data class CategorySpend(val categoryId: String, val amount: Long, val paid: Long, val unpaid: Long)
data class ItemInsight(val title: String, val count: Int, val amount: Long, val average: Long)
data class BudgetState(val limit: Long, val spent: Long) {
    val remaining: Long get() = limit - spent
    val percent: Int get() = if (limit <= 0) 0 else ((spent * 100.0) / limit).toInt().coerceAtLeast(0)
    val warning: BudgetWarning get() = when {
        limit <= 0 -> BudgetWarning.NONE
        spent >= limit -> BudgetWarning.FULL
        spent * 100 >= limit * 80 -> BudgetWarning.EIGHTY
        else -> BudgetWarning.NONE
    }
}
enum class BudgetWarning { NONE, EIGHTY, FULL }
data class ChartSlice(val label: String, val value: Long, val percent: Int)
