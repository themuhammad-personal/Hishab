package com.hishab.app.data.repository

import androidx.room.withTransaction
import com.hishab.app.core.Dates
import com.hishab.app.core.Money
import com.hishab.app.data.local.*
import com.hishab.app.data.settings.AppSettings
import com.hishab.app.data.settings.SettingsRepository
import com.hishab.app.domain.EntryKind
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupDocument(
    val schemaVersion: Int = 1,
    val app: String = "Hishab",
    val exportedAt: Long,
    val wallets: List<WalletEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val duePayments: List<DuePaymentEntity>,
    val transfers: List<TransferEntity>,
    val todos: List<TodoEntity>,
    val budgets: List<BudgetEntity>,
    val categoryBudgets: List<CategoryBudgetEntity>,
    val settings: AppSettings
)

data class BackupPreview(val document: BackupDocument, val warnings: List<String>) {
    val activeTransactions get() = document.transactions.count { it.deletedAt == null }
    val walletCount get() = document.wallets.size
    val categoryCount get() = document.categories.size
    val paymentCount get() = document.duePayments.size
    val transferCount get() = document.transfers.size
    val todoCount get() = document.todos.count { it.deletedAt == null }
}

class BackupManager(private val db: HishabDatabase, private val settings: SettingsRepository) {
    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = false }
    val maxBytes = 20 * 1024 * 1024

    suspend fun export(): String {
        val snap = LedgerSnapshot(
            wallets = db.wallets().observe().firstOrNull() ?: emptyList(),
            categories = db.categories().observe().firstOrNull() ?: emptyList(),
            transactions = db.transactions().observe().firstOrNull() ?: emptyList(),
            payments = db.payments().observe().firstOrNull() ?: emptyList(),
            transfers = db.transfers().observe().firstOrNull() ?: emptyList(),
            todos = db.todos().observe().firstOrNull() ?: emptyList(),
            budgets = db.budgets().observe().firstOrNull() ?: emptyList(),
            categoryBudgets = db.budgets().observeCategories().firstOrNull() ?: emptyList()
        )
        val doc = BackupDocument(
            exportedAt = System.currentTimeMillis(),
            wallets = snap.wallets, categories = snap.categories, transactions = snap.transactions,
            duePayments = snap.payments, transfers = snap.transfers, todos = snap.todos,
            budgets = snap.budgets, categoryBudgets = snap.categoryBudgets,
            settings = settings.settings.firstOrNull() ?: AppSettings()
        )
        return json.encodeToString(doc)
    }

    fun parse(text: String): BackupPreview {
        require(text.toByteArray(Charsets.UTF_8).size <= maxBytes) { "ব্যাকআপ ফাইল ২০ MB-এর বেশি হতে পারবে না" }
        val doc = try { json.decodeFromString<BackupDocument>(text) } catch (e: Exception) {
            throw IllegalArgumentException("ব্যাকআপ ফাইল পড়া যায়নি: ${e.message ?: "অবৈধ JSON"}")
        }
        validate(doc)
        val warnings = buildList {
            if (doc.transactions.any { it.deletedAt != null }) add("কিছু মুছে ফেলা লেনদেন ব্যাকআপে আছে; রিস্টোরের পর সেগুলো সক্রিয় হবে না।")
            if (doc.settings.syncEnabled) add("ব্যাকআপে সিঙ্ক চালু চিহ্নিত আছে; এই সংস্করণে ক্লাউড সিঙ্ক নিষ্ক্রিয় থাকবে।")
        }
        return BackupPreview(doc.copy(settings = doc.settings.copy(syncEnabled = false)), warnings)
    }

    fun validate(doc: BackupDocument) {
        require(doc.app == "Hishab") { "এটি Hishab ব্যাকআপ নয়" }
        require(doc.schemaVersion == 1) { "অসমর্থিত ব্যাকআপ সংস্করণ: ${doc.schemaVersion}" }
        fun unique(ids: List<String>, label: String) { require(ids.size == ids.distinct().size) { "$label-এ ডুপ্লিকেট id আছে" } }
        unique(doc.wallets.map { it.id }, "ওয়ালেট")
        unique(doc.categories.map { it.id }, "ক্যাটাগরি")
        unique(doc.transactions.map { it.id }, "লেনদেন")
        unique(doc.duePayments.map { it.id }, "পরিশোধ")
        unique(doc.transfers.map { it.id }, "স্থানান্তর")
        unique(doc.todos.map { it.id }, "কেনার তালিকা")
        val walletIds = doc.wallets.map { it.id }.toSet()
        val categoryIds = doc.categories.map { it.id }.toSet()
        val txIds = doc.transactions.map { it.id }.toSet()
        doc.wallets.forEach { require(it.name.isNotBlank() && it.openingBalancePoisha >= 0) { "অবৈধ ওয়ালেট" } }
        doc.categories.forEach { require(it.name.isNotBlank()) { "অবৈধ ক্যাটাগরি" } }
        doc.transactions.forEach { t ->
            require(t.title.isNotBlank() && t.amountPoisha > 0 && t.amountPoisha <= Money.MAX_POISHA) { "অবৈধ লেনদেনের পরিমাণ" }
            require(t.paidPoisha in 0..t.amountPoisha) { "পরিশোধিত পরিমাণ মোট টাকার বেশি" }
            require(t.categoryId in categoryIds) { "লেনদেনের ক্যাটাগরি নেই" }
            t.walletId?.let { require(it in walletIds) { "লেনদেনের ওয়ালেট নেই" } }
            Dates.requireDate(t.date); t.dueDate?.let { Dates.requireDate(it) }
            require(t.quantityMilli == null || t.quantityMilli >= 0) { "অবৈধ পরিমাণ" }
            require(t.unitPricePoisha == null || t.unitPricePoisha >= 0) { "অবৈধ একক দাম" }
        }
        val paidByTx = doc.duePayments.groupBy { it.transactionId }.mapValues { e -> e.value.sumOf { it.amountPoisha } }
        doc.duePayments.forEach { p ->
            require(p.transactionId in txIds) { "পরিশোধের লেনদেন নেই" }
            require(p.walletId in walletIds) { "পরিশোধের ওয়ালেট নেই" }
            require(p.amountPoisha > 0) { "অবৈধ পরিশোধ" }
            Dates.requireDate(p.date)
        }
        doc.transactions.forEach { t ->
            val sum = paidByTx[t.id] ?: 0L
            require(sum <= t.amountPoisha) { "কিস্তির যোগফল মোট টাকার বেশি" }
            require(sum == t.paidPoisha) { "পরিশোধের ইতিহাসের সাথে paidPoisha মিলছে না" }
        }
        doc.transfers.forEach { t ->
            require(t.fromWalletId in walletIds && t.toWalletId in walletIds && t.fromWalletId != t.toWalletId) { "অবৈধ স্থানান্তর" }
            require(t.amountPoisha > 0) { "অবৈধ স্থানান্তর পরিমাণ" }
            Dates.requireDate(t.date)
        }
        doc.todos.forEach { t ->
            require(t.title.isNotBlank() && t.targetCategoryId in categoryIds) { "অবৈধ কেনার তালিকা" }
            t.dueDate?.let { Dates.requireDate(it) }
            t.convertedTransactionId?.let { require(it in txIds) { "রূপান্তরিত লেনদেন নেই" } }
        }
        doc.budgets.forEach { require(Dates.parseMonth(it.monthKey) != null && it.overallLimitPoisha >= 0) { "অবৈধ বাজেট" } }
        doc.categoryBudgets.forEach { require(Dates.parseMonth(it.monthKey) != null && it.categoryId in categoryIds && it.limitPoisha >= 0) { "অবৈধ ক্যাটাগরি বাজেট" } }
    }

    suspend fun import(preview: BackupPreview, replace: Boolean) {
        val doc = preview.document
        validate(doc)
        db.withTransaction {
            if (replace) {
                db.payments().clear(); db.transactions().clear(); db.transfers().clear(); db.todos().clear()
                db.budgets().clearCategories(); db.budgets().clear(); db.categories().clear(); db.wallets().clear()
            }
            db.wallets().upsertAll(doc.wallets)
            db.categories().upsertAll(doc.categories)
            db.transactions().upsertAll(doc.transactions)
            db.payments().upsertAll(doc.duePayments)
            db.transfers().upsertAll(doc.transfers)
            db.todos().upsertAll(doc.todos)
            db.budgets().upsertAll(doc.budgets)
            db.budgets().upsertAllCategories(doc.categoryBudgets)
        }
        settings.replaceAll(doc.settings.copy(initialized = true, syncEnabled = false))
    }
}

