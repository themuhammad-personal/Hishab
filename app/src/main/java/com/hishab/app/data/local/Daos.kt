package com.hishab.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY archived, createdAt") fun observe(): Flow<List<WalletEntity>>
    @Query("SELECT * FROM wallets WHERE id = :id") suspend fun get(id: String): WalletEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(value: WalletEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(values: List<WalletEntity>)
    @Query("DELETE FROM wallets") suspend fun clear()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY archived, sortOrder, createdAt") fun observe(): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM categories WHERE id = :id") suspend fun get(id: String): CategoryEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(value: CategoryEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(values: List<CategoryEntity>)
    @Query("DELETE FROM categories") suspend fun clear()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC") fun observe(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE id = :id") suspend fun get(id: String): TransactionEntity?
    @Query("SELECT * FROM transactions WHERE sourceTodoId = :todoId AND deletedAt IS NULL LIMIT 1") suspend fun byTodo(todoId: String): TransactionEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(value: TransactionEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(values: List<TransactionEntity>)
    @Query("DELETE FROM transactions") suspend fun clear()
}

@Dao
interface DuePaymentDao {
    @Query("SELECT * FROM due_payments ORDER BY date, createdAt") fun observe(): Flow<List<DuePaymentEntity>>
    @Query("SELECT * FROM due_payments WHERE transactionId = :txId ORDER BY date, createdAt") suspend fun forTransaction(txId: String): List<DuePaymentEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(value: DuePaymentEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(values: List<DuePaymentEntity>)
    @Query("DELETE FROM due_payments WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM due_payments") suspend fun clear()
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY date DESC, createdAt DESC") fun observe(): Flow<List<TransferEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(value: TransferEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(values: List<TransferEntity>)
    @Query("DELETE FROM transfers") suspend fun clear()
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY done, createdAt DESC") fun observe(): Flow<List<TodoEntity>>
    @Query("SELECT * FROM todos WHERE id = :id") suspend fun get(id: String): TodoEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(value: TodoEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(values: List<TodoEntity>)
    @Query("DELETE FROM todos") suspend fun clear()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets") fun observe(): Flow<List<BudgetEntity>>
    @Query("SELECT * FROM category_budgets") fun observeCategories(): Flow<List<CategoryBudgetEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(value: BudgetEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCategory(value: CategoryBudgetEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(values: List<BudgetEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAllCategories(values: List<CategoryBudgetEntity>)
    @Query("DELETE FROM budgets") suspend fun clear()
    @Query("DELETE FROM category_budgets") suspend fun clearCategories()
}
