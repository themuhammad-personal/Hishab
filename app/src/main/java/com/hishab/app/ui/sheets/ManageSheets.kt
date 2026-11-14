@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hishab.app.ui.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hishab.app.core.Dates
import com.hishab.app.core.Money
import com.hishab.app.domain.EntryKind
import com.hishab.app.domain.TodoPriority
import com.hishab.app.ui.AppViewModel
import com.hishab.app.ui.components.ErrorText
import com.hishab.app.ui.components.LabeledField
import com.hishab.app.ui.theme.HishabTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletSheetHost(vm: AppViewModel, sheet: Sheet.Wallet) {
    val state by vm.state.collectAsStateWithLifecycle()
    val existing = sheet.id?.let { id -> state.snapshot.wallets.firstOrNull { it.id == id } }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var type by rememberSaveable { mutableStateOf(existing?.type ?: "cash") }
    var opening by rememberSaveable { mutableStateOf(existing?.openingBalancePoisha?.let { Money.formatPoisha(it, false, false) } ?: "0") }
    var color by rememberSaveable { mutableStateOf(existing?.color ?: "#145443") }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: "cash") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = vm::closeSheet) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
            SheetTitle(if (existing == null) "নতুন ওয়ালেট" else "ওয়ালেট সম্পাদনা")
            LabeledField("নাম") { OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            LabeledField("ধরন") { OptionPicker("ধরন", type, listOf("cash" to "নগদ", "bkash" to "বিকাশ", "nagad" to "নগদ (মোবাইল)", "bank" to "ব্যাংক", "custom" to "অন্যান্য"), { type = it }) }
            LabeledField("প্রারম্ভিক ব্যালেন্স") { OutlinedTextField(opening, { opening = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, prefix = { Text("৳") }) }
            LabeledField("রং") { ColorPicker(color, { color = it }) }
            LabeledField("আইকন") { IconPicker(icon, { icon = it }) }
            ErrorText(error)
            SheetActions(vm::closeSheet, {
                val bal = Money.parsePoisha(opening)
                when { name.isBlank() -> error = "নাম দিন"; bal == null -> error = "সঠিক প্রারম্ভিক ব্যালেন্স দিন"; else -> { vm.saveWallet(existing?.id, name, type, bal, color, icon); vm.closeSheet() } }
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySheetHost(vm: AppViewModel, sheet: Sheet.Category) {
    val state by vm.state.collectAsStateWithLifecycle()
    val existing = sheet.id?.let { id -> state.snapshot.categories.firstOrNull { it.id == id } }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var kind by rememberSaveable { mutableStateOf(existing?.kind ?: EntryKind.EXPENSE) }
    var color by rememberSaveable { mutableStateOf(existing?.color ?: "#C99B48") }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: "basket") }
    var sort by rememberSaveable { mutableStateOf((existing?.sortOrder ?: state.snapshot.categories.size).toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = vm::closeSheet) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
            SheetTitle(if (existing == null) "নতুন ক্যাটাগরি" else "ক্যাটাগরি সম্পাদনা")
            LabeledField("নাম") { OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            LabeledField("ধরন") { SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) { EntryKind.entries.forEachIndexed { i, k -> SegmentedButton(selected = kind == k, onClick = { kind = k }, shape = SegmentedButtonDefaults.itemShape(i, 2), enabled = existing == null) { Text(if (k == EntryKind.EXPENSE) "খরচ" else "আয়") } } } }
            LabeledField("রং") { ColorPicker(color, { color = it }) }
            LabeledField("আইকন") { IconPicker(icon, { icon = it }) }
            LabeledField("ক্রম") { OutlinedTextField(sort, { sort = it }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            ErrorText(error)
            SheetActions(vm::closeSheet, { when { name.isBlank() -> error = "নাম দিন"; sort.toIntOrNull() == null -> error = "ক্রম সংখ্যা হতে হবে"; else -> { vm.saveCategory(existing?.id, name, kind, color, icon, sort.toInt()); vm.closeSheet() } } })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoSheetHost(vm: AppViewModel, sheet: Sheet.Todo) {
    val state by vm.state.collectAsStateWithLifecycle()
    val existing = sheet.id?.let { id -> state.snapshot.todos.firstOrNull { it.id == id } }
    val categories = state.snapshot.activeCategories.filter { it.kind == EntryKind.EXPENSE }
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var qty by rememberSaveable { mutableStateOf(existing?.quantityMilli?.let { Money.formatQuantity(it, false) } ?: "") }
    var unit by rememberSaveable { mutableStateOf(existing?.unit ?: "") }
    var estimated by rememberSaveable { mutableStateOf(existing?.estimatedPoisha?.let { Money.formatPoisha(it, false, false) } ?: "") }
    var category by rememberSaveable { mutableStateOf(existing?.targetCategoryId ?: categories.firstOrNull()?.id ?: "") }
    var priority by rememberSaveable { mutableStateOf(existing?.priority ?: TodoPriority.NORMAL) }
    var due by rememberSaveable { mutableStateOf(existing?.dueDate ?: "") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = vm::closeSheet) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
            SheetTitle(if (existing == null) "কেনার তালিকা" else "তালিকা সম্পাদনা")
            LabeledField("শিরোনাম") { OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(qty, { qty = it }, label = { Text("পরিমাণ") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(unit, { unit = it }, label = { Text("একক") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            LabeledField("আনুমানিক দাম") { OutlinedTextField(estimated, { estimated = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, prefix = { Text("৳") }) }
            LabeledField("ক্যাটাগরি") { OptionPicker("ক্যাটাগরি", category, categories.map { it.id to it.name }, { category = it }) }
            LabeledField("অগ্রাধিকার") { OptionPicker("অগ্রাধিকার", priority.name, TodoPriority.entries.map { it.name to when (it) { TodoPriority.LOW -> "কম"; TodoPriority.NORMAL -> "সাধারণ"; TodoPriority.HIGH -> "বেশি" } }, { priority = TodoPriority.valueOf(it) }) }
            LabeledField("তারিখ (ঐচ্ছিক)") { if (due.isBlank()) OutlinedButton(onClick = { due = Dates.today().toString() }, modifier = Modifier.fillMaxWidth()) { Text("তারিখ যোগ করুন") } else DateField(due, { due = it }) }
            ErrorText(error)
            SheetActions(vm::closeSheet, {
                val q = if (qty.isBlank()) null else Money.parseQuantityMilli(qty)
                val est = if (estimated.isBlank()) null else Money.parsePoisha(estimated)
                when { title.isBlank() -> error = "শিরোনাম দিন"; category.isBlank() -> error = "ক্যাটাগরি নির্বাচন করুন"; qty.isNotBlank() && q == null -> error = "সঠিক পরিমাণ দিন"; estimated.isNotBlank() && est == null -> error = "সঠিক দাম দিন"; else -> { vm.saveTodo(existing?.id, title, q, unit.ifBlank { null }, est, category, priority, due.ifBlank { null }); vm.closeSheet() } }
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferSheetHost(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val wallets = state.snapshot.activeWallets
    var from by rememberSaveable { mutableStateOf(wallets.firstOrNull()?.id ?: "") }
    var to by rememberSaveable { mutableStateOf(wallets.getOrNull(1)?.id ?: "") }
    var amount by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(Dates.today().toString()) }
    var note by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = vm::closeSheet) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
            SheetTitle("ওয়ালেট স্থানান্তর", "স্থানান্তর আয় বা খরচ নয়")
            LabeledField("যেখান থেকে") { OptionPicker("ওয়ালেট", from, wallets.map { it.id to it.name }, { from = it }) }
            LabeledField("যেখানে") { OptionPicker("ওয়ালেট", to, wallets.map { it.id to it.name }, { to = it }) }
            LabeledField("পরিমাণ") { OutlinedTextField(amount, { amount = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, prefix = { Text("৳") }) }
            LabeledField("তারিখ") { DateField(date, { date = it }) }
            LabeledField("নোট") { OutlinedTextField(note, { note = it }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            ErrorText(error)
            SheetActions(vm::closeSheet, { val a = Money.parsePoisha(amount); when { a == null || a <= 0 -> error = "সঠিক পরিমাণ দিন"; from == to -> error = "ভিন্ন ওয়ালেট নির্বাচন করুন"; else -> { vm.addTransfer(from, to, a, date, note.ifBlank { null }); vm.closeSheet() } } })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSheetHost(vm: AppViewModel, sheet: Sheet.Budget) {
    val state by vm.state.collectAsStateWithLifecycle()
    val category = sheet.categoryId?.let { id -> state.snapshot.categories.firstOrNull { it.id == id } }
    val current = if (category == null) state.snapshot.budgets.firstOrNull { it.monthKey == state.monthKey }?.overallLimitPoisha else state.snapshot.categoryBudgets.firstOrNull { it.monthKey == state.monthKey && it.categoryId == category.id }?.limitPoisha
    var limit by rememberSaveable { mutableStateOf(current?.let { Money.formatPoisha(it, false, false) } ?: "") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = vm::closeSheet) {
        Column(Modifier.padding(24.dp)) {
            SheetTitle(if (category == null) "মাসের বাজেট" else "${category.name} বাজেট", vm.monthLabel())
            LabeledField("সীমা") { OutlinedTextField(limit, { limit = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, prefix = { Text("৳") }) }
            Text("৮০% ও ১০০%-এ অ্যাপের ভেতরে সতর্কতা দেখাবে; খরচ বন্ধ হবে না।", style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted)
            ErrorText(error)
            SheetActions(vm::closeSheet, { val v = Money.parsePoisha(limit); if (v == null) error = "সঠিক সীমা দিন" else { if (category == null) vm.setBudget(state.monthKey, v) else vm.setCategoryBudget(state.monthKey, category.id, v); vm.closeSheet() } })
        }
    }
}
