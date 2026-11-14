@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hishab.app.ui.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hishab.app.core.Dates
import com.hishab.app.core.Money
import com.hishab.app.data.local.TransactionEntity
import com.hishab.app.domain.EntryKind
import com.hishab.app.ui.AppViewModel
import com.hishab.app.ui.components.ErrorText
import com.hishab.app.ui.components.LabeledField
import com.hishab.app.ui.theme.HishabTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySheetHost(vm: AppViewModel, sheet: Sheet.Entry) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snap = state.snapshot
    val existing = sheet.txId?.let { id -> snap.transactions.firstOrNull { it.id == id } }
    val todo = sheet.todoId?.let { id -> snap.todos.firstOrNull { it.id == id } }
    val draft = sheet.draft
    val initialKind = existing?.kind ?: draft?.kind ?: todo?.let { EntryKind.EXPENSE } ?: sheet.kind
    var kind by rememberSaveable(sheet) { mutableStateOf(initialKind) }
    var title by rememberSaveable(sheet) { mutableStateOf(existing?.title ?: draft?.title ?: todo?.title ?: "") }
    var amount by rememberSaveable(sheet) { mutableStateOf(existing?.amountPoisha?.let { Money.formatPoisha(it, false, false) } ?: draft?.amountPoisha?.let { Money.formatPoisha(it, false, false) } ?: todo?.estimatedPoisha?.let { Money.formatPoisha(it, false, false) } ?: "") }
    val categories = snap.activeCategories.filter { it.kind == kind }
    var categoryId by rememberSaveable(sheet) { mutableStateOf(existing?.categoryId ?: draft?.categoryId ?: todo?.targetCategoryId ?: categories.firstOrNull()?.id ?: "") }
    LaunchedEffect(kind, categories.map { it.id }) { if (categories.none { it.id == categoryId }) categoryId = categories.firstOrNull()?.id ?: "" }
    val wallets = snap.activeWallets
    var walletId by rememberSaveable(sheet) { mutableStateOf(existing?.walletId ?: draft?.walletId ?: wallets.firstOrNull()?.id ?: "") }
    var date by rememberSaveable(sheet) { mutableStateOf(existing?.date ?: draft?.date ?: Dates.today().toString()) }
    var paid by rememberSaveable(sheet) { mutableStateOf(existing?.let { it.paidPoisha >= it.amountPoisha } ?: draft?.paid ?: sheet.paid) }
    var dueDate by rememberSaveable(sheet) { mutableStateOf(existing?.dueDate ?: draft?.date ?: Dates.today().plusDays(7).toString()) }
    var shop by rememberSaveable(sheet) { mutableStateOf(existing?.shopOrPerson ?: "") }
    var expanded by rememberSaveable(sheet) { mutableStateOf(false) }
    var qty by rememberSaveable(sheet) { mutableStateOf(existing?.quantityMilli?.let { Money.formatQuantity(it, false) } ?: todo?.quantityMilli?.let { Money.formatQuantity(it, false) } ?: "") }
    var unit by rememberSaveable(sheet) { mutableStateOf(existing?.unit ?: todo?.unit ?: "") }
    var unitPrice by rememberSaveable(sheet) { mutableStateOf(existing?.unitPricePoisha?.let { Money.formatPoisha(it, false, false) } ?: "") }
    var note by rememberSaveable(sheet) { mutableStateOf(existing?.note ?: "") }
    var error by rememberSaveable(sheet) { mutableStateOf<String?>(null) }
    val editing = existing != null
    val lockedPayment = editing

    ModalBottomSheet(onDismissRequest = vm::closeSheet, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            SheetTitle(if (editing) "হিসাব সম্পাদনা" else "নতুন হিসাব", if (editing) "পরিশোধের ইতিহাস আলাদাভাবে সংরক্ষিত থাকবে" else "খরচ, আয় বা বাকি হিসাব যোগ করুন")
            if (!editing) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    EntryKind.entries.forEachIndexed { i, k ->
                        SegmentedButton(selected = kind == k, onClick = { kind = k }, shape = SegmentedButtonDefaults.itemShape(i, EntryKind.entries.size)) { Text(if (k == EntryKind.EXPENSE) "খরচ" else "আয়") }
                    }
                }
            }
            LabeledField("টাকার পরিমাণ") { OutlinedTextField(value = amount, onValueChange = { amount = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.headlineSmall, prefix = { Text("৳") }) }
            LabeledField("শিরোনাম") { OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            LabeledField("ক্যাটাগরি") { OptionPicker("ক্যাটাগরি", categoryId, categories.map { it.id to it.name }, { categoryId = it }) }
            if (lockedPayment) {
                Text("পরিশোধের অবস্থা বা ওয়ালেট বদলাতে লেনদেনের ইতিহাস / পরিশোধ ব্যবহার করুন।", style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted, modifier = Modifier.padding(top = 8.dp))
            } else {
                LabeledField("ওয়ালেট") { OptionPicker("ওয়ালেট", walletId, wallets.map { it.id to it.name }, { walletId = it }) }
                LabeledField("পরিশোধের অবস্থা") {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = paid, onClick = { paid = true }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text(if (kind == EntryKind.EXPENSE) "পরিশোধিত" else "প্রাপ্ত") }
                        SegmentedButton(selected = !paid, onClick = { paid = false }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text(if (kind == EntryKind.EXPENSE) "বাকি" else "পাওনা") }
                    }
                }
            }
            LabeledField("তারিখ") { DateField(date, { date = it }, bengaliDigits = state.settings.bengaliDigits) }
            if (!paid) {
                LabeledField("বাকির তারিখ") { DateField(dueDate, { dueDate = it }, bengaliDigits = state.settings.bengaliDigits) }
                LabeledField("দোকান / ব্যক্তি") { OutlinedTextField(value = shop, onValueChange = { shop = it }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            }
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (expanded) "বিস্তারিত লুকান" else "বিস্তারিত তথ্য") }
            if (expanded) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("পরিমাণ") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("একক") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                LabeledField("একক দাম") { OutlinedTextField(value = unitPrice, onValueChange = { unitPrice = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, prefix = { Text("৳") }) }
                val q = Money.parseQuantityMilli(qty); val up = Money.parsePoisha(unitPrice)
                val total = Money.totalPoisha(q, up)
                if (total != null) TextButton(onClick = { amount = Money.formatPoisha(total, false, false) }) { Text("হিসাব করা মোট: ${vm.money(total)} ব্যবহার করুন") }
                LabeledField("নোট") { OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
            }
            ErrorText(error)
            SheetActions(vm::closeSheet, onSave = {
                val parsedAmount = Money.parsePoisha(amount)
                val q = if (qty.isBlank()) null else Money.parseQuantityMilli(qty)
                val up = if (unitPrice.isBlank()) null else Money.parsePoisha(unitPrice)
                when {
                    title.isBlank() -> error = "শিরোনাম দিন"
                    parsedAmount == null || parsedAmount <= 0 -> error = "সঠিক টাকার পরিমাণ দিন"
                    categoryId.isBlank() -> error = "ক্যাটাগরি নির্বাচন করুন"
                    !lockedPayment && paid && walletId.isBlank() -> error = "ওয়ালেট নির্বাচন করুন"
                    qty.isNotBlank() && q == null -> error = "সঠিক পরিমাণ দিন"
                    unitPrice.isNotBlank() && up == null -> error = "সঠিক একক দাম দিন"
                    existing != null -> {
                        vm.updateTransaction(existing.id) { old ->
                            old.copy(title = title.trim(), amountPoisha = parsedAmount, categoryId = categoryId, date = date,
                                dueDate = if (old.dueDirection != null) dueDate else old.dueDate, shopOrPerson = shop.ifBlank { null },
                                quantityMilli = q, unit = unit.ifBlank { null }, unitPricePoisha = up, note = note.ifBlank { null })
                        }
                        vm.closeSheet()
                    }
                    else -> {
                        vm.addTransaction(kind, title, parsedAmount, categoryId, if (paid) walletId else walletId.ifBlank { null }, date, paid, if (paid) null else dueDate, shop.ifBlank { null }, q, unit.ifBlank { null }, up, note.ifBlank { null }, sheet.todoId)
                        vm.closeSheet()
                    }
                }
            })
        }
    }
}
