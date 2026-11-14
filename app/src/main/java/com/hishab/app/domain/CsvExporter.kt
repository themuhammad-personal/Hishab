package com.hishab.app.domain

import com.hishab.app.core.Money
import com.hishab.app.data.local.CategoryEntity
import com.hishab.app.data.local.TransactionEntity
import com.hishab.app.data.local.WalletEntity

object CsvExporter {
    fun cell(value: String): String {
        val first = value.firstOrNull()
        val trimmedFirst = value.trimStart().firstOrNull()
        val dangerous = first in setOf('=', '+', '-', '@', '\t', '\r') || trimmedFirst in setOf('=', '+', '-', '@')
        val safe = if (dangerous) "'$value" else value
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
    fun transactions(transactions: List<TransactionEntity>, categories: List<CategoryEntity>, wallets: List<WalletEntity>, bengaliDigits: Boolean = false): String {
        val cat = categories.associateBy { it.id }
        val wal = wallets.associateBy { it.id }
        val sb = StringBuilder("\uFEFF")
        sb.appendLine(listOf("id","date","kind","title","category","wallet","amount_bdt","paid_bdt","status","due_direction","due_date","shop_or_person","quantity","unit","unit_price_bdt","note","source_todo_id").joinToString(",") { cell(it) })
        transactions.filter { it.deletedAt == null }.sortedBy { it.date }.forEach { t ->
            val paid = t.paidPoisha.coerceAtMost(t.amountPoisha)
            sb.appendLine(listOf(
                t.id, t.date, t.kind.name, t.title, cat[t.categoryId]?.name ?: t.categoryId, wal[t.walletId]?.name ?: (t.walletId ?: ""),
                Money.formatPoisha(t.amountPoisha, bengaliDigits, false), Money.formatPoisha(paid, bengaliDigits, false),
                paymentStatus(t.amountPoisha, paid).name, t.dueDirection?.name ?: "", t.dueDate ?: "", t.shopOrPerson ?: "",
                t.quantityMilli?.let { Money.formatQuantity(it, false) } ?: "", t.unit ?: "",
                t.unitPricePoisha?.let { Money.formatPoisha(it, false, false) } ?: "", t.note ?: "", t.sourceTodoId ?: ""
            ).joinToString(",") { cell(it) })
        }
        return sb.toString()
    }
}
