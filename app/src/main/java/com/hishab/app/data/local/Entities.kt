package com.hishab.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hishab.app.domain.DueDirection
import com.hishab.app.domain.EntryKind
import com.hishab.app.domain.TodoPriority
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val openingBalancePoisha: Long,
    val color: String,
    val icon: String,
    val archived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: EntryKind,
    val color: String,
    val icon: String,
    val sortOrder: Int,
    val archived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
@Entity(tableName = "transactions", indices = [Index("date"), Index("categoryId"), Index("sourceTodoId")])
data class TransactionEntity(
    @PrimaryKey val id: String,
    val kind: EntryKind,
    val title: String,
    val amountPoisha: Long,
    val paidPoisha: Long = 0,
    val categoryId: String,
    val walletId: String? = null,
    val date: String,
    val dueDirection: DueDirection? = null,
    val dueDate: String? = null,
    val shopOrPerson: String? = null,
    val quantityMilli: Long? = null,
    val unit: String? = null,
    val unitPricePoisha: Long? = null,
    val note: String? = null,
    val sourceTodoId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Serializable
@Entity(tableName = "due_payments", indices = [Index("transactionId"), Index("walletId")])
data class DuePaymentEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val walletId: String,
    val amountPoisha: Long,
    val date: String,
    val note: String? = null,
    val createdAt: Long
)

@Serializable
@Entity(tableName = "transfers", indices = [Index("fromWalletId"), Index("toWalletId")])
data class TransferEntity(
    @PrimaryKey val id: String,
    val fromWalletId: String,
    val toWalletId: String,
    val amountPoisha: Long,
    val date: String,
    val note: String? = null,
    val createdAt: Long,
    val deletedAt: Long? = null
)

@Serializable
@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val quantityMilli: Long? = null,
    val unit: String? = null,
    val estimatedPoisha: Long? = null,
    val targetCategoryId: String,
    val priority: TodoPriority = TodoPriority.NORMAL,
    val dueDate: String? = null,
    val done: Boolean = false,
    val convertedTransactionId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Serializable
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val monthKey: String,
    val overallLimitPoisha: Long,
    val updatedAt: Long
)

@Serializable
@Entity(tableName = "category_budgets", primaryKeys = ["monthKey", "categoryId"])
data class CategoryBudgetEntity(
    val monthKey: String,
    val categoryId: String,
    val limitPoisha: Long,
    val updatedAt: Long
)
