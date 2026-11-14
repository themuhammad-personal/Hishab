@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hishab.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hishab.app.core.Dates
import com.hishab.app.domain.Accounting
import com.hishab.app.domain.BudgetWarning
import com.hishab.app.ui.AppViewModel
import com.hishab.app.ui.UiState
import com.hishab.app.ui.components.*
import com.hishab.app.ui.sheets.Sheet
import com.hishab.app.ui.theme.HishabTheme

@Composable
fun MonthSelector(vm: AppViewModel, state: UiState) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = { vm.shiftMonth(-1) }, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "আগের মাস") }
        Text(vm.monthLabel(), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { vm.shiftMonth(1) }, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "পরের মাস") }
    }
}

@Composable
fun HomeScreen(vm: AppViewModel, state: UiState, onNavigate: (String) -> Unit) {
    val snap = state.snapshot
    val summary = Accounting.monthSummary(state.monthKey, snap.transactions, snap.payments)
    val budget = snap.budgets.firstOrNull { it.monthKey == state.monthKey }?.overallLimitPoisha ?: 0L
    val budgetState = Accounting.overallBudget(state.monthKey, budget, snap.transactions, snap.payments)
    val recent = snap.activeTransactions.filter { Dates.monthKey(Dates.requireDate(it.date)) == state.monthKey }.take(8)
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
        item { MonthSelector(vm, state) }
        if (snap.activeTransactions.isEmpty()) {
            item { EmptyState("আপনার খাতা প্রস্তুত", "কোনো নমুনা লেনদেন যোগ করা হয়নি। নিজের মতো করে হিসাব শুরু করুন।", "প্রথম হিসাব যোগ করুন") { vm.openSheet(Sheet.Entry()) } }
        } else {
            item {
                Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("মাসের খরচ", color = MaterialTheme.colorScheme.onPrimary); Surface(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .16f), shape = MaterialTheme.shapes.small) { Text("বাকিসহ", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary) } }
                        Text(vm.money(summary.expenseTotal), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.height(12.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .25f)); Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) { Text("পরিশোধিত", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .85f)); Text(vm.money(summary.expensePaid), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary) }
                            Column(Modifier.weight(1f)) { Text("এখনো বাকি", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .85f)); Text(vm.money(summary.expenseUnpaid), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary) }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.weight(1f)) { Column(Modifier.padding(14.dp)) { Text("মাসের আয়", style = MaterialTheme.typography.labelMedium, color = HishabTheme.colors.muted); Text(vm.money(summary.incomeReceived), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary); Text("প্রাপ্ত আয়", style = MaterialTheme.typography.labelSmall, color = HishabTheme.colors.muted) } }
                    Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.weight(1f)) { Column(Modifier.padding(14.dp)) { Text("ওয়ালেট ব্যালেন্স", style = MaterialTheme.typography.labelMedium, color = HishabTheme.colors.muted); Text(vm.money(snap.totalBalance), style = MaterialTheme.typography.titleLarge); Text("প্রকৃত পরিশোধের ভিত্তিতে", style = MaterialTheme.typography.labelSmall, color = HishabTheme.colors.muted) } }
                }
            }
            if (budget > 0) item {
                Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), onClick = { onNavigate("budgets") }) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("মাসের বাজেট ${vm.money(budget)}", style = MaterialTheme.typography.titleSmall); Text(vm.digits("${budgetState.percent}%")) }
                        BudgetBar(budgetState.percent, budgetState.warning, Modifier.padding(top = 8.dp))
                        Text(if (budgetState.remaining >= 0) "খরচের সুযোগ আরও ${vm.money(budgetState.remaining)}" else "সীমা অতিক্রম ${vm.money(-budgetState.remaining)}", style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            item { SectionHeader("সাম্প্রতিক লেনদেন", "সব দেখুন") { onNavigate("transactions") } }
            items(recent, key = { it.id }) { tx ->
                val cat = snap.categories.firstOrNull { it.id == tx.categoryId }
                SwipeableTransactionRow(tx, cat?.name ?: "", cat?.icon ?: "more", snap.wallets.firstOrNull { it.id == tx.walletId }?.name, vm.money(tx.amountPoisha), state.settings.bengaliDigits,
                    onEdit = { vm.openSheet(Sheet.Entry(tx.id)) }, onDelete = { vm.deleteTransaction(tx.id) }, onClick = { vm.openSheet(Sheet.Entry(tx.id)) })
                HorizontalDivider(color = HishabTheme.colors.border)
            }
        }
    }
}
