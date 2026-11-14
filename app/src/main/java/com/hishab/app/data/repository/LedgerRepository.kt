package com.hishab.app.data.repository

import androidx.room.withTransaction
import com.hishab.app.core.Dates
import com.hishab.app.core.Money
import com.hishab.app.data.local.*
import com.hishab.app.data.settings.AppSettings
import com.hishab.app.data.settings.SettingsRepository
import com.hishab.app.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

data class LedgerSnapshot(
    val wallets: List<WalletEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val payments: List<DuePaymentEntity> = emptyList(),
    val transfers: List<TransferEntity> = emptyList(),
    val todos: List<TodoEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val categoryBudgets: List<CategoryBudgetEntity> = emptyList()
) {
    val activeTransactions get() = transactions.filter { it.deletedAt == null }
    val activeWallets get() = wallets.filter { !it.archived }
    val activeCategories get() = categories.filter { !it.archived }
    private val balances: Map<String, Long> by lazy {
        Accounting.walletBalances(wallets, transactions, payments, transfers).associate { it.walletId to it.balance }
    }
    fun walletBalance(id: String): Long = balances[id] ?: 0L
    val totalBalance: Long get() = activeWallets.sumOf { walletBalance(it.id) }
    fun remaining(tx: TransactionEntity): Long = (tx.amountPoisha - payments.filter { it.transactionId == tx.id }.sumOf { it.amountPoisha }).coerceAtLeast(0)
}

class LedgerRepository(
    private val db: HishabDatabase,
    private val settings: SettingsRepository
) {
    private data class CoreSnap(val wallets: List<WalletEntity>, val categories: List<CategoryEntity>, val transactions: List<TransactionEntity>, val payments: List<DuePaymentEntity>, val transfers: List<TransferEntity>)
    private data class ExtraSnap(val todos: List<TodoEntity>, val budgets: List<BudgetEntity>, val categoryBudgets: List<CategoryBudgetEntity>)
    val snapshot: Flow<LedgerSnapshot> = combine(
        combine(db.wallets().observe(), db.categories().observe(), db.transactions().observe(), db.payments().observe(), db.transfers().observe()) { w, c, t, p, tr -> CoreSnap(w, c, t, p, tr) },
        combine(db.todos().observe(), db.budgets().observe(), db.budgets().observeCategories()) { td, b, cb -> ExtraSnap(td, b, cb) }
    ) { core, extra ->
        LedgerSnapshot(core.wallets, core.categories, core.transactions, core.payments, core.transfers, extra.todos, extra.budgets, extra.categoryBudgets)
    }

    private fun now() = System.currentTimeMillis()
    private fun id() = UUID.randomUUID().toString()

    suspend fun initializeDefaultsIfNeeded(current: AppSettings) {
        if (current.initialized) return
        db.withTransaction {
            if (db.wallets().observeOnce().isEmpty()) {
                val ts = now()
                listOf(
                    WalletEntity(id(), "নগদ", "cash", 0, "#145443", "cash", false, ts, ts),
                    WalletEntity(id(), "বিকাশ", "bkash", 0, "#A83E3E", "smartphone", false, ts, ts),
                    WalletEntity(id(), "নগদ (মোবাইল)", "nagad", 0, "#C99B48", "smartphone", false, ts, ts),
                    WalletEntity(id(), "ব্যাংক হিসাব", "bank", 0, "#2F628A", "bank", false, ts, ts)
                ).forEach { db.wallets().upsert(it) }
            }
            if (db.categories().observeOnce().isEmpty()) {
                val ts = now()
                val defaults = listOf(
                    Triple("আয়", EntryKind.INCOME, "income"),
                    Triple("বাজার সদাই", EntryKind.EXPENSE, "basket"),
                    Triple("মাছ ও মাংস", EntryKind.EXPENSE, "restaurant"),
                    Triple("যাতায়াত ও ভাড়া", EntryKind.EXPENSE, "bus"),
                    Triple("মাসিক বিল", EntryKind.EXPENSE, "receipt"),
                    Triple("ওষুধ / মেডিকেল", EntryKind.EXPENSE, "medical"),
                    Triple("শিক্ষা", EntryKind.EXPENSE, "school"),
                    Triple("দান-সদকা", EntryKind.EXPENSE, "heart"),
                    Triple("অন্যান্য খরচ", EntryKind.EXPENSE, "more"),
                    Triple("অন্যান্য আয়", EntryKind.INCOME, "trending")
                )
                defaults.forEachIndexed { i, (name, kind, icon) ->
                    db.categories().upsert(CategoryEntity(id(), name, kind, if (kind == EntryKind.INCOME) "#145443" else "#C99B48", icon, i, false, ts, ts))
                }
            }
        }
        settings.markInitialized()
    }

    suspend fun addTransaction(
        kind: EntryKind, title: String, amountPoisha: Long, categoryId: String, walletId: String?,
        date: String, paid: Boolean, dueDate: String?, shopOrPerson: String?, quantityMilli: Long?,
        unit: String?, unitPricePoisha: Long?, note: String?, sourceTodoId: String? = null
    ): String {
        require(title.isNotBlank()) { "শিরোনাম দিন" }
        require(amountPoisha > 0) { "টাকার পরিমাণ শূন্যের বেশি হতে হবে" }
        require(amountPoisha <= Money.MAX_POISHA) { "টাকার পরিমাণ সীমার বেশি" }
        validateQuantity(quantityMilli, unitPricePoisha)
        Dates.requireDate(date)
        dueDate?.let { Dates.requireDate(it) }
        val category = db.categories().get(categoryId) ?: throw IllegalArgumentException("ক্যাটাগরি পাওয়া যায়নি")
        require(!category.archived) { "আর্কাইভ করা ক্যাটাগরি ব্যবহার করা যাবে না" }
        require(category.kind == kind) { "ক্যাটাগরির ধরন মিলছে না" }
        if (paid) require(walletId != null && db.wallets().get(walletId)?.archived == false) { "সক্রিয় ওয়ালেট নির্বাচন করুন" }
        if (!paid) requireNotNull(dueDate ?: date) { "বাকির তারিখ দিন" }
        return db.withTransaction {
            if (sourceTodoId != null) {
                db.transactions().byTodo(sourceTodoId)?.let { return@withTransaction it.id }
            }
            val ts = now()
            val txId = id()
            val direction = if (!paid) (if (kind == EntryKind.EXPENSE) DueDirection.PAYABLE else DueDirection.RECEIVABLE) else null
            val tx = TransactionEntity(
                id = txId, kind = kind, title = title.trim(), amountPoisha = amountPoisha, paidPoisha = if (paid) amountPoisha else 0,
                categoryId = categoryId, walletId = walletId, date = date, dueDirection = direction, dueDate = if (!paid) (dueDate ?: date) else null,
                shopOrPerson = shopOrPerson?.trim()?.ifBlank { null }, quantityMilli = quantityMilli, unit = unit?.trim()?.ifBlank { null },
                unitPricePoisha = unitPricePoisha, note = note?.trim()?.ifBlank { null }, sourceTodoId = sourceTodoId, createdAt = ts, updatedAt = ts
            )
            db.transactions().upsert(tx)
            if (paid && walletId != null) {
                db.payments().upsert(DuePaymentEntity(id(), txId, walletId, amountPoisha, date, "প্রাথমিক পরিশোধ", ts))
            }
            if (sourceTodoId != null) {
                db.todos().get(sourceTodoId)?.let { todo ->
                    db.todos().upsert(todo.copy(done = true, convertedTransactionId = txId, updatedAt = ts))
                }
            }
            txId
        }
    }

    suspend fun updateTransaction(txId: String, edit: (TransactionEntity) -> TransactionEntity) {
        db.withTransaction {
            val old = db.transactions().get(txId) ?: throw IllegalArgumentException("লেনদেন পাওয়া যায়নি")
            require(old.deletedAt == null) { "মুছে ফেলা লেনদেন সম্পাদনা করা যাবে না" }
            val candidate = edit(old).copy(id = txId, updatedAt = now())
            require(candidate.title.isNotBlank()) { "শিরোনাম দিন" }
            require(candidate.amountPoisha > 0) { "টাকার পরিমাণ শূন্যের বেশি হতে হবে" }
            require(candidate.amountPoisha <= Money.MAX_POISHA) { "টাকার পরিমাণ সীমার বেশি" }
            validateQuantity(candidate.quantityMilli, candidate.unitPricePoisha)
            Dates.requireDate(candidate.date)
            candidate.dueDate?.let { Dates.requireDate(it) }
            val paid = db.payments().forTransaction(txId).sumOf { it.amountPoisha }
            require(paid <= candidate.amountPoisha) { "ইতিমধ্যে পরিশোধিত টাকার চেয়ে কম মোট পরিমাণ দেওয়া যাবে না" }
            val category = db.categories().get(candidate.categoryId) ?: throw IllegalArgumentException("ক্যাটাগরি পাওয়া যায়নি")
            require(category.kind == candidate.kind) { "ক্যাটাগরির ধরন মিলছে না" }
            require(candidate.dueDirection == null || candidate.dueDirection == directionFor(candidate.kind)) { "বাকির ধরন লেনদেনের ধরনের সাথে মিলছে না" }
            if (paid < candidate.amountPoisha) requireNotNull(candidate.dueDate ?: candidate.date) { "বাকির তারিখ দিন" }
            db.transactions().upsert(candidate.copy(paidPoisha = paid))
        }
    }

    suspend fun addPayment(txId: String, walletId: String, amountPoisha: Long, date: String, note: String?) {
        require(amountPoisha in 1..Money.MAX_POISHA) { "পরিশোধের পরিমাণ সঠিক সীমার মধ্যে দিন" }
        Dates.requireDate(date)
        db.withTransaction {
            val tx = db.transactions().get(txId) ?: throw IllegalArgumentException("লেনদেন পাওয়া যায়নি")
            require(tx.deletedAt == null) { "মুছে ফেলা লেনদেনে পরিশোধ যোগ করা যাবে না" }
            val wallet = db.wallets().get(walletId) ?: throw IllegalArgumentException("ওয়ালেট পাওয়া যায়নি")
            require(!wallet.archived) { "আর্কাইভ করা ওয়ালেট ব্যবহার করা যাবে না" }
            val paid = db.payments().forTransaction(txId).sumOf { it.amountPoisha }
            require(amountPoisha <= tx.amountPoisha - paid) { "বাকির চেয়ে বেশি পরিশোধ করা যাবে না" }
            val ts = now()
            db.payments().upsert(DuePaymentEntity(id(), txId, walletId, amountPoisha, date, note?.trim()?.ifBlank { null }, ts))
            db.transactions().upsert(tx.copy(paidPoisha = paid + amountPoisha, updatedAt = ts))
        }
    }

    suspend fun deletePayment(paymentId: String) {
        db.withTransaction {
            val payment = db.payments().observeOnce().firstOrNull { it.id == paymentId } ?: return@withTransaction
            db.payments().delete(paymentId)
            recalculatePaid(payment.transactionId)
        }
    }

    suspend fun restorePayment(payment: DuePaymentEntity) {
        db.withTransaction {
            val tx = db.transactions().get(payment.transactionId) ?: throw IllegalArgumentException("লেনদেন পাওয়া যায়নি")
            require(tx.deletedAt == null) { "মুছে ফেলা লেনদেনে পরিশোধ ফেরানো যাবে না" }
            val wallet = db.wallets().get(payment.walletId) ?: throw IllegalArgumentException("ওয়ালেট পাওয়া যায়নি")
            require(!wallet.archived) { "আর্কাইভ করা ওয়ালেটে পরিশোধ ফেরানো যাবে না" }
            val paid = db.payments().forTransaction(tx.id).filter { it.id != payment.id }.sumOf { it.amountPoisha }
            require(payment.amountPoisha <= tx.amountPoisha - paid) { "পরিশোধ ফেরালে মোট টাকা ছাড়িয়ে যাবে" }
            db.payments().upsert(payment)
            recalculatePaid(tx.id)
        }
    }

    private suspend fun recalculatePaid(txId: String) {
        db.transactions().get(txId)?.let { tx ->
            val paid = db.payments().forTransaction(txId).sumOf { it.amountPoisha }
            db.transactions().upsert(tx.copy(paidPoisha = paid, updatedAt = now()))
        }
    }

    suspend fun softDeleteTransaction(txId: String) {
        db.withTransaction {
            val tx = db.transactions().get(txId) ?: return@withTransaction
            db.transactions().upsert(tx.copy(deletedAt = now(), updatedAt = now()))
        }
    }
    suspend fun restoreTransaction(txId: String) {
        db.withTransaction {
            val tx = db.transactions().get(txId) ?: return@withTransaction
            db.transactions().upsert(tx.copy(deletedAt = null, updatedAt = now()))
        }
    }

    suspend fun saveWallet(walletId: String?, name: String, type: String, openingPoisha: Long, color: String, icon: String) {
        require(name.isNotBlank()) { "ওয়ালেটের নাম দিন" }
        require(openingPoisha in 0..Money.MAX_POISHA) { "প্রারম্ভিক ব্যালেন্স সঠিক সীমার মধ্যে দিন" }
        val ts = now()
        val old = walletId?.let { db.wallets().get(it) }
        db.wallets().upsert(WalletEntity(walletId ?: id(), name.trim(), type, openingPoisha, color, icon, old?.archived ?: false, old?.createdAt ?: ts, ts))
    }
    suspend fun archiveWallet(walletId: String, archived: Boolean) {
        db.wallets().get(walletId)?.let { db.wallets().upsert(it.copy(archived = archived, updatedAt = now())) }
    }
    suspend fun addTransfer(from: String, to: String, amountPoisha: Long, date: String, note: String?) {
        require(from != to) { "একই ওয়ালেটে স্থানান্তর করা যাবে না" }
        require(amountPoisha in 1..Money.MAX_POISHA) { "স্থানান্তরের পরিমাণ সঠিক সীমার মধ্যে দিন" }
        Dates.requireDate(date)
        val a = db.wallets().get(from); val b = db.wallets().get(to)
        require(a != null && b != null && !a.archived && !b.archived) { "সক্রিয় ওয়ালেট নির্বাচন করুন" }
        db.transfers().upsert(TransferEntity(id(), from, to, amountPoisha, date, note?.trim()?.ifBlank { null }, now()))
    }
    suspend fun softDeleteTransfer(id: String) { db.transfers().observeOnce().firstOrNull { it.id == id }?.let { db.transfers().upsert(it.copy(deletedAt = now())) } }
    suspend fun restoreTransfer(id: String) { db.transfers().observeOnce().firstOrNull { it.id == id }?.let { db.transfers().upsert(it.copy(deletedAt = null)) } }

    suspend fun saveCategory(categoryId: String?, name: String, kind: EntryKind, color: String, icon: String, sortOrder: Int) {
        require(name.isNotBlank()) { "ক্যাটাগরির নাম দিন" }
        require(sortOrder in 0..10000) { "ক্রম ০ থেকে ১০০০-এর মধ্যে দিন" }
        val ts = now()
        val old = categoryId?.let { db.categories().get(it) }
        if (old != null && old.kind != kind) {
            val used = db.transactions().observeOnce().any { it.categoryId == categoryId && it.deletedAt == null }
            require(!used) { "ব্যবহৃত ক্যাটাগরির ধরন বদলানো যাবে না; নতুন ক্যাটাগরি তৈরি করুন" }
        }
        db.categories().upsert(CategoryEntity(categoryId ?: id(), name.trim(), kind, color, icon, sortOrder, old?.archived ?: false, old?.createdAt ?: ts, ts))
    }
    suspend fun archiveCategory(categoryId: String, archived: Boolean) {
        db.categories().get(categoryId)?.let { db.categories().upsert(it.copy(archived = archived, updatedAt = now())) }
    }
    suspend fun deleteCategoryIfUnused(categoryId: String): Boolean {
        val used = db.transactions().observeOnce().any { it.categoryId == categoryId && it.deletedAt == null } || db.todos().observeOnce().any { it.targetCategoryId == categoryId && it.deletedAt == null }
        if (used) return false
        db.withTransaction {
            db.categories().get(categoryId)?.let { db.categories().upsert(it.copy(archived = true, updatedAt = now())) }
        }
        return true
    }

    suspend fun saveTodo(todoId: String?, title: String, quantityMilli: Long?, unit: String?, estimatedPoisha: Long?, targetCategoryId: String, priority: TodoPriority, dueDate: String?) {
        require(title.isNotBlank()) { "কেনার তালিকার শিরোনাম দিন" }
        validateQuantity(quantityMilli, estimatedPoisha)
        val category = db.categories().get(targetCategoryId) ?: throw IllegalArgumentException("ক্যাটাগরি পাওয়া যায়নি")
        require(!category.archived && category.kind == EntryKind.EXPENSE) { "খরচের একটি সক্রিয় ক্যাটাগরি নির্বাচন করুন" }
        dueDate?.let { Dates.requireDate(it) }
        val ts = now(); val old = todoId?.let { db.todos().get(it) }
        db.todos().upsert(TodoEntity(todoId ?: id(), title.trim(), quantityMilli, unit?.trim()?.ifBlank { null }, estimatedPoisha, targetCategoryId, priority, dueDate, old?.done ?: false, old?.convertedTransactionId, old?.createdAt ?: ts, ts))
    }
    suspend fun toggleTodo(todoId: String) {
        val todo = db.todos().get(todoId) ?: return
        require(todo.convertedTransactionId == null) { "খরচে রূপান্তরিত তালিকার অবস্থা বদলানো যাবে না" }
        db.todos().upsert(todo.copy(done = !todo.done, updatedAt = now()))
    }
    suspend fun softDeleteTodo(todoId: String) { db.todos().get(todoId)?.let { db.todos().upsert(it.copy(deletedAt = now(), updatedAt = now())) } }
    suspend fun restoreTodo(todoId: String) { db.todos().get(todoId)?.let { db.todos().upsert(it.copy(deletedAt = null, updatedAt = now())) } }

    suspend fun setOverallBudget(monthKey: String, limitPoisha: Long) {
        require(Dates.parseMonth(monthKey) != null) { "মাস ঠিক নয়" }
        require(limitPoisha in 0..Money.MAX_POISHA) { "বাজেট সঠিক সীমার মধ্যে দিন" }
        db.budgets().upsert(BudgetEntity(monthKey, limitPoisha, now()))
    }
    suspend fun setCategoryBudget(monthKey: String, categoryId: String, limitPoisha: Long) {
        require(Dates.parseMonth(monthKey) != null) { "মাস ঠিক নয়" }
        require(limitPoisha in 0..Money.MAX_POISHA) { "বাজেট সঠিক সীমার মধ্যে দিন" }
        val category = db.categories().get(categoryId) ?: throw IllegalArgumentException("ক্যাটাগরি পাওয়া যায়নি")
        require(category.kind == EntryKind.EXPENSE) { "শুধু খরচ ক্যাটাগরির বাজেট হয়" }
        db.budgets().upsertCategory(CategoryBudgetEntity(monthKey, categoryId, limitPoisha, now()))
    }

    suspend fun resetLedger() {
        db.withTransaction {
            db.payments().clear(); db.transactions().clear(); db.transfers().clear(); db.todos().clear()
            db.budgets().clearCategories(); db.budgets().clear(); db.categories().clear(); db.wallets().clear()
        }
        settings.markInitialized()
    }

    private fun directionFor(kind: EntryKind): DueDirection = if (kind == EntryKind.EXPENSE) DueDirection.PAYABLE else DueDirection.RECEIVABLE

    private fun validateQuantity(quantityMilli: Long?, unitPricePoisha: Long?) {
        require(quantityMilli == null || quantityMilli in 0..999_999_999L) { "পরিমাণ সঠিক সীমার মধ্যে দিন" }
        require(unitPricePoisha == null || unitPricePoisha in 0..Money.MAX_POISHA) { "একক দাম সঠিক সীমার মধ্যে দিন" }
    }

    // Synchronous helpers used inside transactions.
    private suspend fun WalletDao.observeOnce(): List<WalletEntity> = observe().firstOrNull() ?: emptyList()
    private suspend fun CategoryDao.observeOnce(): List<CategoryEntity> = observe().firstOrNull() ?: emptyList()
    private suspend fun TransactionDao.observeOnce(): List<TransactionEntity> = observe().firstOrNull() ?: emptyList()
    private suspend fun DuePaymentDao.observeOnce(): List<DuePaymentEntity> = observe().firstOrNull() ?: emptyList()
    private suspend fun TransferDao.observeOnce(): List<TransferEntity> = observe().firstOrNull() ?: emptyList()
    private suspend fun TodoDao.observeOnce(): List<TodoEntity> = observe().firstOrNull() ?: emptyList()
}
