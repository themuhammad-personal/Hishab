package com.hishab.app.data.repository

import androidx.room.withTransaction
import com.hishab.app.core.Dates
import com.hishab.app.core.Money
import com.hishab.app.data.local.*
import com.hishab.app.data.settings.AppSettings
import com.hishab.app.data.settings.SettingsRepository
import com.hishab.app.domain.DueDirection
import com.hishab.app.domain.EntryKind
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.InputStream

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

    /** Reads at most [maxBytes] + 1 bytes so a hostile SAF file cannot exhaust memory. */
    fun readBounded(input: InputStream): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            require(total <= maxBytes) { "ব্যাকআপ ফাইল ২০ MB-এর বেশি হতে পারবে না" }
            out.write(buffer, 0, n)
        }
        return out.toString(Charsets.UTF_8.name())
    }

    suspend fun export(): String = db.withTransaction {
        json.encodeToString(readDocument())
    }

    private suspend fun readDocument(): BackupDocument = BackupDocument(
        exportedAt = System.currentTimeMillis(),
        wallets = db.wallets().observe().firstOrNull().orEmpty(),
        categories = db.categories().observe().firstOrNull().orEmpty(),
        transactions = db.transactions().observe().firstOrNull().orEmpty(),
        duePayments = db.payments().observe().firstOrNull().orEmpty(),
        transfers = db.transfers().observe().firstOrNull().orEmpty(),
        todos = db.todos().observe().firstOrNull().orEmpty(),
        budgets = db.budgets().observe().firstOrNull().orEmpty(),
        categoryBudgets = db.budgets().observeCategories().firstOrNull().orEmpty(),
        settings = settings.settings.firstOrNull() ?: AppSettings()
    )

    fun parse(text: String): BackupPreview {
        require(text.toByteArray(Charsets.UTF_8).size <= maxBytes) { "ব্যাকআপ ফাইল ২০ MB-এর বেশি হতে পারবে না" }
        val doc = try { json.decodeFromString<BackupDocument>(text) } catch (e: Exception) {
            throw IllegalArgumentException("ব্যাকআপ ফাইল পড়া যায়নি: ${e.message ?: "অবৈধ JSON"}")
        }
        validate(doc)
        val warnings = buildList {
            if (doc.transactions.any { it.deletedAt != null }) add("কিছু মুছে ফেলা লেনদেন ব্যাকআপে আছে; রিস্টোরের পর সেগুলো সক্রিয় হবে না।")
            if (doc.settings.syncEnabled) add("ব্যাকআপে সিঙ্ক চালু চিহ্নিত আছে; এই সংস্করণে ক্লাউড সিঙ্ক নিষ্ক্রিয় থাকবে।")
            if (doc.settings.remindersEnabled) add("ব্যাকআপে রিমাইন্ডার চালু ছিল; ডিভাইসের নোটিফিকেশন অনুমতি না থাকলে আবার চালু করতে হবে।")
        }
        return BackupPreview(doc.copy(settings = doc.settings.copy(syncEnabled = false)), warnings)
    }

    fun validate(doc: BackupDocument) {
        require(doc.app == "Hishab") { "এটি Hishab ব্যাকআপ নয়" }
        require(doc.schemaVersion == 1) { "অসমর্থিত ব্যাকআপ সংস্করণ: ${doc.schemaVersion}" }
        fun unique(ids: List<String>, label: String) {
            require(ids.all { it.isNotBlank() } && ids.size == ids.distinct().size) { "$label-এ অবৈধ বা ডুপ্লিকেট id আছে" }
        }
        unique(doc.wallets.map { it.id }, "ওয়ালেট")
        unique(doc.categories.map { it.id }, "ক্যাটাগরি")
        unique(doc.transactions.map { it.id }, "লেনদেন")
        unique(doc.duePayments.map { it.id }, "পরিশোধ")
        unique(doc.transfers.map { it.id }, "স্থানান্তর")
        unique(doc.todos.map { it.id }, "কেনার তালিকা")
        unique(doc.budgets.map { it.monthKey }, "মাসিক বাজেট")
        unique(doc.categoryBudgets.map { "${it.monthKey}/${it.categoryId}" }, "ক্যাটাগরি বাজেট")

        val walletIds = doc.wallets.map { it.id }.toSet()
        val categories = doc.categories.associateBy { it.id }
        val transactions = doc.transactions.associateBy { it.id }
        val todos = doc.todos.associateBy { it.id }
        doc.wallets.forEach {
            require(it.name.isNotBlank() && it.openingBalancePoisha in 0..Money.MAX_POISHA) { "অবৈধ ওয়ালেট" }
        }
        doc.categories.forEach { require(it.name.isNotBlank()) { "অবৈধ ক্যাটাগরি" } }
        doc.transactions.forEach { t ->
            require(t.title.isNotBlank() && t.amountPoisha in 1..Money.MAX_POISHA) { "অবৈধ লেনদেনের পরিমাণ" }
            require(t.paidPoisha in 0..t.amountPoisha) { "পরিশোধিত পরিমাণ মোট টাকার বেশি" }
            val category = categories[t.categoryId] ?: throw IllegalArgumentException("লেনদেনের ক্যাটাগরি নেই")
            require(category.kind == t.kind) { "লেনদেন ও ক্যাটাগরির ধরন মিলছে না" }
            t.walletId?.let { require(it in walletIds) { "লেনদেনের ওয়ালেট নেই" } }
            Dates.requireDate(t.date); t.dueDate?.let { Dates.requireDate(it) }
            val expectedDirection = if (t.kind == EntryKind.EXPENSE) DueDirection.PAYABLE else DueDirection.RECEIVABLE
            require(t.dueDirection == null || t.dueDirection == expectedDirection) { "বাকির ধরন লেনদেনের ধরনের সাথে মিলছে না" }
            if (t.paidPoisha < t.amountPoisha) {
                require(t.dueDirection != null && t.dueDate != null) { "অপরিশোধিত লেনদেনে বাকির ধরন ও তারিখ দরকার" }
            }
            require(t.quantityMilli == null || t.quantityMilli in 0..999_999_999L) { "অবৈধ পরিমাণ" }
            require(t.unitPricePoisha == null || t.unitPricePoisha in 0..Money.MAX_POISHA) { "অবৈধ একক দাম" }
            t.sourceTodoId?.let { source ->
                val todo = todos[source] ?: throw IllegalArgumentException("রূপান্তরের উৎস তালিকা নেই")
                require(todo.convertedTransactionId == null || todo.convertedTransactionId == t.id) { "তালিকা ও লেনদেনের রূপান্তর সম্পর্ক মিলছে না" }
            }
        }
        val paidByTx = doc.duePayments.groupBy { it.transactionId }.mapValues { e -> e.value.sumOf { it.amountPoisha } }
        doc.duePayments.forEach { p ->
            require(p.transactionId in transactions.keys) { "পরিশোধের লেনদেন নেই" }
            require(p.walletId in walletIds) { "পরিশোধের ওয়ালেট নেই" }
            require(p.amountPoisha in 1..Money.MAX_POISHA) { "অবৈধ পরিশোধ" }
            Dates.requireDate(p.date)
        }
        doc.transactions.forEach { t ->
            val sum = paidByTx[t.id] ?: 0L
            require(sum <= t.amountPoisha) { "কিস্তির যোগফল মোট টাকার বেশি" }
            require(sum == t.paidPoisha) { "পরিশোধের ইতিহাসের সাথে paidPoisha মিলছে না" }
        }
        doc.transfers.forEach { t ->
            require(t.fromWalletId in walletIds && t.toWalletId in walletIds && t.fromWalletId != t.toWalletId) { "অবৈধ স্থানান্তর" }
            require(t.amountPoisha in 1..Money.MAX_POISHA) { "অবৈধ স্থানান্তর পরিমাণ" }
            Dates.requireDate(t.date)
        }
        doc.todos.forEach { t ->
            val category = categories[t.targetCategoryId] ?: throw IllegalArgumentException("কেনার তালিকার ক্যাটাগরি নেই")
            require(t.title.isNotBlank() && category.kind == EntryKind.EXPENSE) { "অবৈধ কেনার তালিকা" }
            require(t.estimatedPoisha == null || t.estimatedPoisha in 0..Money.MAX_POISHA) { "অবৈধ আনুমানিক দাম" }
            require(t.quantityMilli == null || t.quantityMilli in 0..999_999_999L) { "অবৈধ পরিমাণ" }
            t.dueDate?.let { Dates.requireDate(it) }
            t.convertedTransactionId?.let { id ->
                val tx = transactions[id] ?: throw IllegalArgumentException("রূপান্তরিত লেনদেন নেই")
                require(tx.sourceTodoId == t.id) { "তালিকা ও লেনদেনের রূপান্তর সম্পর্ক মিলছে না" }
            }
        }
        doc.budgets.forEach {
            require(Dates.parseMonth(it.monthKey) != null && it.overallLimitPoisha in 0..Money.MAX_POISHA) { "অবৈধ বাজেট" }
        }
        doc.categoryBudgets.forEach {
            require(Dates.parseMonth(it.monthKey) != null && it.limitPoisha in 0..Money.MAX_POISHA) { "অবৈধ ক্যাটাগরি বাজেট" }
            require(categories[it.categoryId]?.kind == EntryKind.EXPENSE) { "বাজেটের ক্যাটাগরি খরচের নয় বা নেই" }
        }
    }

    suspend fun import(preview: BackupPreview, replace: Boolean) {
        val incoming = preview.document.copy(settings = preview.document.settings.copy(syncEnabled = false))
        validate(incoming)
        db.withTransaction {
            val target = if (replace) incoming else mergeWithCurrent(incoming)
            validate(target)
            if (replace) {
                db.payments().clear(); db.transactions().clear(); db.transfers().clear(); db.todos().clear()
                db.budgets().clearCategories(); db.budgets().clear(); db.categories().clear(); db.wallets().clear()
            }
            db.wallets().upsertAll(target.wallets)
            db.categories().upsertAll(target.categories)
            target.transactions.chunked(200).forEach { db.transactions().upsertAll(it) }
            target.duePayments.chunked(200).forEach { db.payments().upsertAll(it) }
            target.transfers.chunked(200).forEach { db.transfers().upsertAll(it) }
            target.todos.chunked(200).forEach { db.todos().upsertAll(it) }
            db.budgets().upsertAll(target.budgets)
            db.budgets().upsertAllCategories(target.categoryBudgets)
        }
        settings.replaceAll(incoming.settings.copy(initialized = true, syncEnabled = false))
    }

    /** Validates the *resulting* ledger before writing, so colliding ids cannot corrupt foreign keys or paid totals. */
    private suspend fun mergeWithCurrent(incoming: BackupDocument): BackupDocument {
        val current = readDocument()
        fun <T> merge(old: List<T>, new: List<T>, key: (T) -> String): List<T> =
            (old.associateBy(key) + new.associateBy(key)).values.toList()
        return incoming.copy(
            wallets = merge(current.wallets, incoming.wallets) { it.id },
            categories = merge(current.categories, incoming.categories) { it.id },
            transactions = merge(current.transactions, incoming.transactions) { it.id },
            duePayments = merge(current.duePayments, incoming.duePayments) { it.id },
            transfers = merge(current.transfers, incoming.transfers) { it.id },
            todos = merge(current.todos, incoming.todos) { it.id },
            budgets = merge(current.budgets, incoming.budgets) { it.monthKey },
            categoryBudgets = merge(current.categoryBudgets, incoming.categoryBudgets) { "${it.monthKey}/${it.categoryId}" },
            settings = current.settings
        )
    }
}
