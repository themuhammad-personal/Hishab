package com.hishab.app

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hishab.app.data.local.HishabDatabase
import com.hishab.app.data.repository.BackupManager
import com.hishab.app.data.repository.LedgerRepository
import com.hishab.app.data.settings.AppSettings
import com.hishab.app.data.settings.SettingsRepository
import com.hishab.app.domain.EntryKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class LedgerRepositoryTest {
    private lateinit var db: HishabDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var repo: LedgerRepository
    private lateinit var backup: BackupManager
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, HishabDatabase::class.java).allowMainThreadQueries().build()
        settings = SettingsRepository(context)
        settings.replaceAll(AppSettings())
        repo = LedgerRepository(db, settings)
        backup = BackupManager(db, settings)
        repo.initializeDefaultsIfNeeded(settings.settings.first())
    }
    @After fun teardown() { db.close() }

    @Test fun defaultsWithoutSampleTransactions() = runBlocking {
        val snap = repo.snapshot.first()
        assertEquals(4, snap.wallets.size)
        assertEquals(10, snap.categories.size)
        assertTrue(snap.transactions.isEmpty())
        assertTrue(settings.settings.first().initialized)
        repo.initializeDefaultsIfNeeded(settings.settings.first())
        assertEquals(4, repo.snapshot.first().wallets.size)
    }

    @Test fun paidAndUnpaidAccountingWithPartialPayment() = runBlocking {
        val snap = repo.snapshot.first()
        val wallet = snap.activeWallets.first()
        val cat = snap.activeCategories.first { it.kind == EntryKind.EXPENSE }
        val paidId = repo.addTransaction(EntryKind.EXPENSE, "নগদ খরচ", 10000, cat.id, wallet.id, "2026-11-14", true, null, null, null, null, null, null)
        val dueId = repo.addTransaction(EntryKind.EXPENSE, "বাকি খরচ", 30000, cat.id, null, "2026-11-14", false, "2026-11-20", "দোকান", null, null, null, null)
        repo.addPayment(dueId, wallet.id, 5000, "2026-11-15", null)
        val after = repo.snapshot.first()
        assertEquals(-15000L, after.walletBalance(wallet.id))
        assertEquals(25000L, after.remaining(after.transactions.first { it.id == dueId }))
        assertEquals(40000L, com.hishab.app.domain.Accounting.monthSummary("2026-11", after.transactions, after.payments).expenseTotal)
        expectIllegalArgumentException { repo.addPayment(dueId, wallet.id, 26000, "2026-11-16", null) }
        val payment = repo.snapshot.first().payments.first { it.transactionId == dueId }
        repo.deletePayment(payment.id)
        val deleted = repo.snapshot.first()
        assertEquals(30000L, deleted.remaining(deleted.transactions.first { it.id == dueId }))
        repo.restorePayment(payment)
        val restored = repo.snapshot.first()
        assertEquals(25000L, restored.remaining(restored.transactions.first { it.id == dueId }))
        assertNotNull(paidId)
    }

    @Test fun todoConversionIsIdempotent() = runBlocking {
        val snap = repo.snapshot.first()
        val cat = snap.activeCategories.first { it.kind == EntryKind.EXPENSE }
        val wallet = snap.activeWallets.first()
        repo.saveTodo(null, "চাল", 2000, "কেজি", 12000, cat.id, com.hishab.app.domain.TodoPriority.NORMAL, null)
        val todo = repo.snapshot.first().todos.single()
        val a = repo.addTransaction(EntryKind.EXPENSE, "চাল", 12000, cat.id, wallet.id, "2026-11-14", true, null, null, 2000, "কেজি", 600, null, todo.id)
        val b = repo.addTransaction(EntryKind.EXPENSE, "চাল আবার", 12000, cat.id, wallet.id, "2026-11-14", true, null, null, null, null, null, null, todo.id)
        assertEquals(a, b)
        assertEquals(1, repo.snapshot.first().transactions.size)
        assertTrue(repo.snapshot.first().todos.single().done)
    }

    @Test fun transferNeutralAndArchivedWalletRejected() = runBlocking {
        val snap = repo.snapshot.first()
        val a = snap.activeWallets[0]; val b = snap.activeWallets[1]
        repo.saveWallet(a.id, a.name, a.type, 50000, a.color, a.icon)
        repo.addTransfer(a.id, b.id, 20000, "2026-11-14", null)
        val after = repo.snapshot.first()
        assertEquals(30000L, after.walletBalance(a.id))
        assertEquals(20000L, after.walletBalance(b.id))
        assertEquals(50000L, after.totalBalance)
        expectIllegalArgumentException { repo.addTransfer(a.id, a.id, 100, "2026-11-14", null) }
        repo.archiveWallet(b.id, true)
        expectIllegalArgumentException { repo.addTransfer(a.id, b.id, 100, "2026-11-14", null) }
    }

    @Test fun categoryKindChangeBlockedWhenUsed() = runBlocking {
        val snap = repo.snapshot.first()
        val cat = snap.activeCategories.first { it.kind == EntryKind.EXPENSE }
        repo.addTransaction(EntryKind.EXPENSE, "x", 100, cat.id, null, "2026-11-14", false, "2026-11-20", null, null, null, null, null)
        expectIllegalArgumentException { repo.saveCategory(cat.id, cat.name, EntryKind.INCOME, cat.color, cat.icon, cat.sortOrder) }
    }

    private suspend fun expectIllegalArgumentException(block: suspend () -> Unit) {
        try { block(); fail("Expected IllegalArgumentException") } catch (_: IllegalArgumentException) { }
    }

    @Test fun editingPaidAmountCreatesDueAndKindIsLocked() = runBlocking {
        val snap = repo.snapshot.first()
        val wallet = snap.activeWallets.first()
        val expenseCat = snap.activeCategories.first { it.kind == EntryKind.EXPENSE }
        val incomeCat = snap.activeCategories.first { it.kind == EntryKind.INCOME }
        val id = repo.addTransaction(EntryKind.EXPENSE, "পরিশোধিত", 1000, expenseCat.id, wallet.id, "2026-11-14", true, null, null, null, null, null, null)
        repo.updateTransaction(id) { it.copy(amountPoisha = 2000) }
        val edited = repo.snapshot.first().transactions.first { it.id == id }
        assertEquals(1000L, repo.snapshot.first().remaining(edited))
        assertEquals(com.hishab.app.domain.DueDirection.PAYABLE, edited.dueDirection)
        assertNotNull(edited.dueDate)
        expectIllegalArgumentException { repo.updateTransaction(id) { it.copy(kind = EntryKind.INCOME, categoryId = incomeCat.id) } }
    }

    @Test fun deletingInitialPaymentCreatesDue() = runBlocking {
        val snap = repo.snapshot.first()
        val wallet = snap.activeWallets.first()
        val cat = snap.activeCategories.first { it.kind == EntryKind.EXPENSE }
        val id = repo.addTransaction(EntryKind.EXPENSE, "পরিশোধিত", 1000, cat.id, wallet.id, "2026-11-14", true, null, null, null, null, null, null)
        val payment = repo.snapshot.first().payments.first { it.transactionId == id }
        repo.deletePayment(payment.id)
        val after = repo.snapshot.first()
        val tx = after.transactions.first { it.id == id }
        assertEquals(1000L, after.remaining(tx))
        assertEquals(com.hishab.app.domain.DueDirection.PAYABLE, tx.dueDirection)
    }

    @Test fun mergeRejectsKindFlipWithPaymentHistory() = runBlocking {
        val snap = repo.snapshot.first()
        val wallet = snap.activeWallets.first()
        val expenseCat = snap.activeCategories.first { it.kind == EntryKind.EXPENSE }
        val incomeCat = snap.activeCategories.first { it.kind == EntryKind.INCOME }
        repo.addTransaction(EntryKind.EXPENSE, "খরচ", 1000, expenseCat.id, wallet.id, "2026-11-14", true, null, null, null, null, null, null)
        val preview = backup.parse(backup.export())
        val flipped = preview.document.copy(transactions = preview.document.transactions.map { it.copy(kind = EntryKind.INCOME, categoryId = incomeCat.id) })
        backup.validate(flipped)
        expectIllegalArgumentException { backup.import(com.hishab.app.data.repository.BackupPreview(flipped, emptyList()), replace = false) }
    }

    @Test fun backupRoundTripReplaceAndMerge() = runBlocking {
        val snap = repo.snapshot.first()
        val cat = snap.activeCategories.first { it.kind == EntryKind.EXPENSE }
        val wallet = snap.activeWallets.first()
        repo.addTransaction(EntryKind.EXPENSE, "প্রথম", 1000, cat.id, wallet.id, "2026-11-14", true, null, null, null, null, null, null)
        val text = backup.export()
        val preview = backup.parse(text)
        assertEquals(1, preview.activeTransactions)
        repo.addTransaction(EntryKind.EXPENSE, "দ্বিতীয়", 2000, cat.id, wallet.id, "2026-11-14", true, null, null, null, null, null, null)
        backup.import(preview, replace = false)
        assertEquals(2, repo.snapshot.first().transactions.size)
        backup.import(preview, replace = true)
        assertEquals(1, repo.snapshot.first().transactions.size)
        assertFalse(settings.settings.first().syncEnabled)
        repo.resetLedger()
        val empty = repo.snapshot.first()
        assertTrue(empty.transactions.isEmpty() && empty.wallets.isEmpty() && empty.categories.isEmpty() && empty.budgets.isEmpty())
    }
}
