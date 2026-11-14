package com.hishab.app

import com.hishab.app.data.local.*
import com.hishab.app.data.repository.BackupDocument
import com.hishab.app.data.settings.AppSettings
import com.hishab.app.domain.DueDirection
import com.hishab.app.domain.EntryKind
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import android.app.Application

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class BackupValidationTest {
    private fun doc() = BackupDocument(
        exportedAt = 1L,
        wallets = listOf(WalletEntity("w", "নগদ", "cash", 0, "#145443", "cash", false, 1, 1)),
        categories = listOf(CategoryEntity("c", "বাজার", EntryKind.EXPENSE, "#C99B48", "basket", 0, false, 1, 1)),
        transactions = listOf(TransactionEntity(id = "t", kind = EntryKind.EXPENSE, title = "বাজার", amountPoisha = 10000, paidPoisha = 4000, categoryId = "c", walletId = "w", date = "2026-11-14", dueDirection = DueDirection.PAYABLE, dueDate = "2026-11-20", createdAt = 1, updatedAt = 1)),
        duePayments = listOf(DuePaymentEntity("p", "t", "w", 4000, "2026-11-14", null, 1)),
        transfers = emptyList(), todos = emptyList(), budgets = emptyList(), categoryBudgets = emptyList(),
        settings = AppSettings(initialized = true)
    )
    private lateinit var db: HishabDatabase
    private lateinit var manager: com.hishab.app.data.repository.BackupManager
    @org.junit.Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, HishabDatabase::class.java).allowMainThreadQueries().build()
        manager = com.hishab.app.data.repository.BackupManager(db, com.hishab.app.data.settings.SettingsRepository(context))
    }
    @org.junit.After fun teardown() { db.close() }
    @Test fun validDocumentPasses() { manager.validate(doc()) }
    @Test fun rejectsPaidMismatch() {
        assertThrows(IllegalArgumentException::class.java) { manager.validate(doc().copy(transactions = doc().transactions.map { it.copy(paidPoisha = 5000) })) }
    }
    @Test fun rejectsOrphanPayment() {
        assertThrows(IllegalArgumentException::class.java) { manager.validate(doc().copy(duePayments = doc().duePayments.map { it.copy(transactionId = "missing") })) }
    }
    @Test fun rejectsDuplicateIds() {
        val d = doc(); assertThrows(IllegalArgumentException::class.java) { manager.validate(d.copy(wallets = d.wallets + d.wallets)) }
    }
    @Test fun rejectsCategoryKindMismatch() {
        val d = doc(); assertThrows(IllegalArgumentException::class.java) { manager.validate(d.copy(categories = d.categories.map { it.copy(kind = EntryKind.INCOME) })) }
    }
    @Test fun rejectsUnknownSchemaAndApp() {
        assertThrows(IllegalArgumentException::class.java) { manager.validate(doc().copy(schemaVersion = 2)) }
        assertThrows(IllegalArgumentException::class.java) { manager.validate(doc().copy(app = "Other")) }
    }
}
