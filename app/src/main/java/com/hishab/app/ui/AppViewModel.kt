@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hishab.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hishab.app.HishabApplication
import com.hishab.app.ai.AssistantDraft
import com.hishab.app.ai.LocalAssistant
import com.hishab.app.core.Dates
import com.hishab.app.data.repository.BackupPreview
import com.hishab.app.data.repository.LedgerSnapshot
import com.hishab.app.data.settings.AppSettings
import com.hishab.app.domain.Accounting
import com.hishab.app.domain.BudgetWarning
import com.hishab.app.domain.EntryKind
import com.hishab.app.domain.ThemeMode
import com.hishab.app.domain.TodoPriority
import com.hishab.app.ui.sheets.Sheet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth

data class UiState(
    val settings: AppSettings = AppSettings(),
    val snapshot: LedgerSnapshot = LedgerSnapshot(),
    val monthKey: String = YearMonth.now().toString(),
    val loading: Boolean = true
)
data class AppEvent(val message: String, val undo: (() -> Unit)? = null)
data class ChatMessage(val role: String, val text: String)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as HishabApplication).container
    val ledger = container.ledger
    val backup = container.backup
    val settingsRepo = container.settings
    private val assistant = LocalAssistant()
    private val _chat = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chat: StateFlow<List<ChatMessage>> = _chat.asStateFlow()
    private val _draft = MutableStateFlow<AssistantDraft?>(null)
    val draft: StateFlow<AssistantDraft?> = _draft.asStateFlow()
    private val month = MutableStateFlow(YearMonth.now().toString())
    private val _sheet = MutableStateFlow<Sheet?>(null)
    val sheet: StateFlow<Sheet?> = _sheet.asStateFlow()
    fun openSheet(value: Sheet) { _sheet.value = value }
    fun closeSheet() { _sheet.value = null }
    private val _events = Channel<AppEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private val budgetWarningLevels = mutableMapOf<String, BudgetWarning>()
    val state: StateFlow<UiState> = combine(container.settings.settings, container.ledger.snapshot, month) { s, snap, m ->
        UiState(s, snap, m, false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    init {
        viewModelScope.launch {
            val s = container.settings.settings.first()
            runCatching { container.ledger.initializeDefaultsIfNeeded(s) }
                .onFailure { _events.trySend(AppEvent(it.message ?: "প্রারম্ভিক ডেটা তৈরি হয়নি")) }
            container.reminders.schedule(s.remindersEnabled)
        }
    }

    fun setMonth(key: String) { if (Dates.parseMonth(key) != null) month.value = key }
    fun shiftMonth(delta: Long) { month.value = YearMonth.parse(month.value).plusMonths(delta).toString() }
    fun money(poisha: Long, symbol: Boolean = true) = com.hishab.app.core.Money.formatPoisha(poisha, state.value.settings.bengaliDigits, symbol)
    fun digits(value: String) = if (state.value.settings.bengaliDigits) com.hishab.app.core.Money.toBengaliDigits(value) else value
    fun date(value: String) = Dates.formatDate(value, state.value.settings.bengaliDigits)
    fun monthLabel(key: String = month.value) = Dates.formatMonth(key, state.value.settings.bengaliDigits)

    fun run(action: suspend () -> Unit, success: String? = null, undo: (() -> Unit)? = null) {
        viewModelScope.launch {
            try { action(); success?.let { _events.send(AppEvent(it, undo)) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _events.send(AppEvent(e.message ?: "কাজটি সম্পন্ন হয়নি")) }
        }
    }

    fun setTheme(mode: ThemeMode) = run({ settingsRepo.setTheme(mode) })
    fun setDigits(value: Boolean) = run({ settingsRepo.setBengaliDigits(value) })
    fun setReminders(value: Boolean) = run({ settingsRepo.setReminders(value); container.reminders.schedule(value) }, if (value) "রিমাইন্ডার চালু হয়েছে" else "রিমাইন্ডার বন্ধ হয়েছে")
    fun setCloudConsent(value: Boolean) = run({ settingsRepo.setCloudAiConsent(value) })

    fun addTransaction(kind: EntryKind, title: String, amount: Long, categoryId: String, walletId: String?, date: String, paid: Boolean, dueDate: String?, shop: String?, qty: Long?, unit: String?, unitPrice: Long?, note: String?, todoId: String? = null) =
        run({ ledger.addTransaction(kind, title, amount, categoryId, walletId, date, paid, dueDate, shop, qty, unit, unitPrice, note, todoId) }, "হিসাব সংরক্ষণ হয়েছে")
    fun updateTransaction(id: String, edit: (com.hishab.app.data.local.TransactionEntity) -> com.hishab.app.data.local.TransactionEntity) = run({ ledger.updateTransaction(id, edit) }, "লেনদেন আপডেট হয়েছে")
    fun deleteTransaction(id: String) = run({ ledger.softDeleteTransaction(id) }, "লেনদেন মুছে ফেলা হয়েছে", undo = { run({ ledger.restoreTransaction(id) }, "লেনদেন ফিরিয়ে আনা হয়েছে") })
    fun addPayment(txId: String, walletId: String, amount: Long, date: String, note: String?) = run({ ledger.addPayment(txId, walletId, amount, date, note) }, "পরিশোধ সংরক্ষণ হয়েছে")
    fun deletePayment(id: String) {
        val payment = state.value.snapshot.payments.firstOrNull { it.id == id }
        run({ ledger.deletePayment(id) }, "পরিশোধ মুছে ফেলা হয়েছে", undo = payment?.let { p -> { run({ ledger.restorePayment(p) }, "পরিশোধ ফিরিয়ে আনা হয়েছে") } })
    }
    fun saveWallet(id: String?, name: String, type: String, opening: Long, color: String, icon: String) = run({ ledger.saveWallet(id, name, type, opening, color, icon) }, "ওয়ালেট সংরক্ষণ হয়েছে")
    fun archiveWallet(id: String, archived: Boolean) = run({ ledger.archiveWallet(id, archived) }, if (archived) "ওয়ালেট আর্কাইভ হয়েছে" else "ওয়ালেট সক্রিয় হয়েছে", undo = { run({ ledger.archiveWallet(id, !archived) }, "ওয়ালেটের অবস্থা ফিরিয়ে আনা হয়েছে") })
    fun addTransfer(from: String, to: String, amount: Long, date: String, note: String?) = run({ ledger.addTransfer(from, to, amount, date, note) }, "স্থানান্তর সংরক্ষণ হয়েছে")
    fun deleteTransfer(id: String) = run({ ledger.softDeleteTransfer(id) }, "স্থানান্তর মুছে ফেলা হয়েছে", undo = { run({ ledger.restoreTransfer(id) }, "স্থানান্তর ফিরিয়ে আনা হয়েছে") })
    fun saveCategory(id: String?, name: String, kind: EntryKind, color: String, icon: String, sort: Int) = run({ ledger.saveCategory(id, name, kind, color, icon, sort) }, "ক্যাটাগরি সংরক্ষণ হয়েছে")
    fun archiveCategory(id: String, archived: Boolean) = run({ ledger.archiveCategory(id, archived) }, if (archived) "ক্যাটাগরি আর্কাইভ হয়েছে" else "ক্যাটাগরি সক্রিয় হয়েছে", undo = { run({ ledger.archiveCategory(id, !archived) }, "ক্যাটাগরির অবস্থা ফিরিয়ে আনা হয়েছে") })
    fun saveTodo(id: String?, title: String, qty: Long?, unit: String?, estimated: Long?, category: String, priority: TodoPriority, due: String?) = run({ ledger.saveTodo(id, title, qty, unit, estimated, category, priority, due) }, "তালিকা সংরক্ষণ হয়েছে")
    fun toggleTodo(id: String) = run({ ledger.toggleTodo(id) })
    fun deleteTodo(id: String) = run({ ledger.softDeleteTodo(id) }, "তালিকা মুছে ফেলা হয়েছে", undo = { run({ ledger.restoreTodo(id) }, "তালিকা ফিরিয়ে আনা হয়েছে") })
    fun setBudget(monthKey: String, limit: Long) = run({ ledger.setOverallBudget(monthKey, limit) }, "বাজেট সংরক্ষণ হয়েছে")
    fun setCategoryBudget(monthKey: String, categoryId: String, limit: Long) = run({ ledger.setCategoryBudget(monthKey, categoryId, limit) }, "ক্যাটাগরি বাজেট সংরক্ষণ হয়েছে")
    fun resetLedger() = run({ ledger.resetLedger() }, "খাতা রিসেট হয়েছে")

    fun sendAssistant(input: String) {
        if (input.isBlank()) return
        val reply = assistant.parse(input, state.value.snapshot.activeCategories, state.value.snapshot.activeWallets, state.value.snapshot.activeTransactions, Dates.today().toString())
        _chat.value = _chat.value + ChatMessage("user", input) + ChatMessage("assistant", reply.text)
        _draft.value = reply.draft
    }
    fun clearDraft() { _draft.value = null }
    fun confirmDraft(draft: AssistantDraft) {
        // Consume the draft before launching the write so a double tap cannot execute it twice.
        if (!_draft.compareAndSet(draft, null)) return
        when (draft) {
            is AssistantDraft.AddTransaction -> addTransaction(draft.kind, draft.title, draft.amountPoisha, draft.categoryId, draft.walletId, draft.date, draft.paid, if (draft.paid) null else draft.date, null, null, null, null, null)
            is AssistantDraft.PayDue -> addPayment(draft.transactionId, draft.walletId, draft.amountPoisha, draft.date, "সহকারী থেকে নিশ্চিতকৃত")
            is AssistantDraft.DeleteTransaction -> deleteTransaction(draft.transactionId)
        }
        _chat.value = _chat.value + ChatMessage("assistant", "নিশ্চিতকরণ পাওয়া গেছে; কাজটি সম্পন্ন হলে বার্তা দেখাবে।")
    }

    fun exportJson(uri: Uri) = run({
        val text = backup.export()
        getApplication<HishabApplication>().contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } ?: error("ফাইল লেখা যায়নি")
        settingsRepo.markBackup(System.currentTimeMillis())
    }, "JSON ব্যাকআপ সংরক্ষণ হয়েছে")
    fun exportCsv(uri: Uri) = run({
        val snap = state.value.snapshot
        val text = com.hishab.app.domain.CsvExporter.transactions(snap.transactions, snap.categories, snap.wallets, state.value.settings.bengaliDigits)
        getApplication<HishabApplication>().contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } ?: error("ফাইল লেখা যায়নি")
    }, "CSV এক্সপোর্ট হয়েছে")
    fun readBackup(uri: Uri): BackupPreview? {
        return try {
            val text = getApplication<HishabApplication>().contentResolver.openInputStream(uri)?.use { backup.readBounded(it) } ?: error("ফাইল পড়া যায়নি")
            backup.parse(text)
        } catch (e: Exception) { viewModelScope.launch { _events.send(AppEvent(e.message ?: "ব্যাকআপ পড়া যায়নি")) }; null }
    }
    fun importBackup(preview: BackupPreview, replace: Boolean) = run({
        backup.import(preview, replace)
        budgetWarningLevels.clear()
        container.reminders.schedule(preview.document.settings.remindersEnabled)
    }, "ব্যাকআপ রিস্টোর হয়েছে")
}
