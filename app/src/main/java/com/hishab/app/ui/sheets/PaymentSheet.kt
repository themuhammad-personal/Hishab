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
import com.hishab.app.ui.AppViewModel
import com.hishab.app.ui.components.ConfirmDialog
import com.hishab.app.ui.components.ErrorText
import com.hishab.app.ui.components.LabeledField
import com.hishab.app.ui.theme.HishabTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSheetHost(vm: AppViewModel, sheet: Sheet.Payment) {
    val state by vm.state.collectAsStateWithLifecycle()
    val tx = state.snapshot.transactions.firstOrNull { it.id == sheet.txId } ?: return
    val remaining = state.snapshot.remaining(tx)
    val wallets = state.snapshot.activeWallets
    var amount by rememberSaveable { mutableStateOf(Money.formatPoisha(remaining, false, false)) }
    var walletId by rememberSaveable { mutableStateOf(wallets.firstOrNull()?.id ?: "") }
    var date by rememberSaveable { mutableStateOf(Dates.today().toString()) }
    var note by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var deletePayment by remember { mutableStateOf<String?>(null) }
    val parsed = Money.parsePoisha(amount)
    val wallet = wallets.firstOrNull { it.id == walletId }
    val before = wallet?.let { state.snapshot.walletBalance(it.id) }
    ModalBottomSheet(onDismissRequest = vm::closeSheet, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            SheetTitle("বাকি পরিশোধ", "${tx.shopOrPerson ?: tx.title} · বাকি ${vm.money(remaining)}")
            LabeledField("এখন কত পরিশোধ করবেন?") { OutlinedTextField(value = amount, onValueChange = { amount = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.headlineSmall, prefix = { Text("৳") }) }
            LabeledField("কোন ওয়ালেট থেকে") { OptionPicker("ওয়ালেট", walletId, wallets.map { it.id to it.name }, { walletId = it }) }
            LabeledField("তারিখ") { DateField(date, { date = it }) }
            LabeledField("নোট") { OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            if (parsed != null && wallet != null && before != null && parsed in 1..remaining) {
                Surface(color = HishabTheme.colors.primarySoft, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("সংরক্ষণের আগে দেখুন", style = MaterialTheme.typography.titleSmall)
                        Text("বাকি থাকবে: ${vm.money(remaining)} → ${vm.money(remaining - parsed)}")
                        Text("${wallet.name}: ${vm.money(before)} → ${vm.money(if (tx.kind == com.hishab.app.domain.EntryKind.EXPENSE) before - parsed else before + parsed)}")
                        Text("মাসের খরচ: অপরিবর্তিত", style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted)
                    }
                }
            }
            val history = state.snapshot.payments.filter { it.transactionId == tx.id }
            if (history.isNotEmpty()) {
                Text("পরিশোধের ইতিহাস", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
                history.forEach { p ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(vm.date(p.date)); Text(vm.money(p.amountPoisha), style = MaterialTheme.typography.bodySmall) }
                        TextButton(onClick = { deletePayment = p.id }) { Text("মুছুন") }
                    }
                }
            }
            ErrorText(error)
            SheetActions(vm::closeSheet, onSave = {
                when {
                    parsed == null || parsed <= 0 -> error = "সঠিক পরিশোধের পরিমাণ দিন"
                    parsed > remaining -> error = "বাকির চেয়ে বেশি পরিশোধ করা যাবে না"
                    walletId.isBlank() -> error = "ওয়ালেট নির্বাচন করুন"
                    else -> { vm.addPayment(tx.id, walletId, parsed, date, note.ifBlank { null }); vm.closeSheet() }
                }
            }, saveLabel = "পরিশোধ নিশ্চিত করুন")
        }
    }
    deletePayment?.let { id -> ConfirmDialog("পরিশোধ মুছবেন?", "এই কিস্তি মুছে ফেললে বাকি ও ওয়ালেট ব্যালেন্স আবার সমন্বয় হবে।", "মুছুন", { vm.deletePayment(id); deletePayment = null }, { deletePayment = null }, true) }
}
