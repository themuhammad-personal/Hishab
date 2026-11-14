package com.hishab.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [WalletEntity::class, CategoryEntity::class, TransactionEntity::class, DuePaymentEntity::class, TransferEntity::class, TodoEntity::class, BudgetEntity::class, CategoryBudgetEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class HishabDatabase : RoomDatabase() {
    abstract fun wallets(): WalletDao
    abstract fun categories(): CategoryDao
    abstract fun transactions(): TransactionDao
    abstract fun payments(): DuePaymentDao
    abstract fun transfers(): TransferDao
    abstract fun todos(): TodoDao
    abstract fun budgets(): BudgetDao

    companion object {
        fun create(context: Context): HishabDatabase =
            Room.databaseBuilder(context.applicationContext, HishabDatabase::class.java, "hishab.db").build()
    }
}
