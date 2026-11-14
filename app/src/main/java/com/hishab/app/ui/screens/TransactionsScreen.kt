@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hishab.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hishab.app.core.Dates
import com.hishab.app.domain.Accounting
import com.hishab.app.domain.EntryKind
import com.hishab.app.domain.PaymentStatus
import com.hishab.app.domain.paymentStatus
import com.hishab.app.ui.AppViewModel
import com.hishab.app.ui.UiState
import com.hishab.app.ui.components.*
import com.hishab.app.ui.sheets.Sheet
import com.hishab.app.ui.theme.HishabTheme

@Composable
fun TransactionsScreen(vm: AppViewModel, state: UiState, categoryId: String? = null) {
    val snap = state.snapshot
    var query by rememberSaveable { mutableStateOf("") }
    var kindFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var statusFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val category = categoryId?.let { id -> snap.categories.firstOrNull { it.id == id } }
    val txs = snap.activeTransactions.filter { t ->
        (categoryId == null || t.categoryId == categoryId) &&
        (kindFilter == null || t.kind.name == kindFilter) &&
        (statusFilter == null || paymentStatus(t.amountPoisha, t.paidPoisha).name == statusFilter) &&
        (query.isBlank() || t.title.contains(query, true) || t.note?.contains(query, true) == true || t.shopOrPerson?.contains(query, true) == true)
    }.sortedByDescending { it.date }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
        if (category != null) {
            item {
                val insights = Accounting.itemInsights(category.id, state.monthKey, snap.transactions)
                val spend = Accounting.categorySpend(state.monthKey, snap.transactions, snap.payments).firstOrNull { it.categoryId == category.id }
                Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(category.name, style = MaterialTheme.typography.titleMedium)
                        Text("${vm.monthLabel()} · ${vm.money(spend?.amount ?: 0)}", color = HishabTheme.colors.muted)
                        if (insights.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp)); Text("আইটেম অনুযায়ী", style = MaterialTheme.typography.titleSmall)
                            insights.take(5).forEach { Text("${it.title} · ${vm.digits(it.count.toString())} বার · ${vm.money(it.amount)}", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
        item { OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("খুঁজুন") }) }
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = kindFilter == null, onClick = { kindFilter = null }, label = { Text("সব") })
                FilterChip(selected = kindFilter == "EXPENSE", onClick = { kindFilter = "EXPENSE" }, label = { Text("খরচ") })
                FilterChip(selected = kindFilter == "INCOME", onClick = { kindFilter = "INCOME" }, label = { Text("আয়") })
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = statusFilter == null, onClick = { statusFilter = null }, label = { Text("সব অবস্থা") })
                FilterChip(selected = statusFilter == "PAID", onClick = { statusFilter = "PAID" }, label = { Text("পরিশোধিত") })
                FilterChip(selected = statusFilter == "UNPAID", onClick = { statusFilter = "UNPAID" }, label = { Text("বাকি") })
                FilterChip(selected = statusFilter == "PARTIAL", onClick = { statusFilter = "PARTIAL" }, label = { Text("আংশিক") })
            }
        }
        if (txs.isEmpty()) item { EmptyState("কোনো লেনদেন নেই", "ফিল্টার বদলান অথবা নতুন হিসাব যোগ করুন।", "নতুন হিসাব") { vm.openSheet(Sheet.Entry(kind = if (category?.kind == EntryKind.INCOME) EntryKind.INCOME else EntryKind.EXPENSE)) } }
        txs.groupBy { it.date }.toSortedMap(reverseOrder()).forEach { (date, group) ->
            item(key = "header-$date") { Text(Dates.relativeLabel(date, bengaliDigits = state.settings.bengaliDigits), style = MaterialTheme.typography.labelMedium, color = HishabTheme.colors.muted, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }
            items(group, key = { it.id }) { tx ->
                val cat = snap.categories.firstOrNull { it.id == tx.categoryId }
                SwipeableTransactionRow(tx, cat?.name ?: "", cat?.icon ?: "more", snap.wallets.firstOrNull { it.id == tx.walletId }?.name, vm.money(tx.amountPoisha), state.settings.bengaliDigits,
                    onEdit = { vm.openSheet(Sheet.Entry(tx.id)) }, onDelete = { vm.deleteTransaction(tx.id) }, onClick = { vm.openSheet(Sheet.Entry(tx.id)) })
                HorizontalDivider(color = HishabTheme.colors.border)
            }
        }
    }
}
