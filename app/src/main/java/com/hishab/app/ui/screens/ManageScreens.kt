@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hishab.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hishab.app.core.Dates
import com.hishab.app.domain.BudgetWarning
import com.hishab.app.domain.DueDirection
import com.hishab.app.domain.EntryKind
import com.hishab.app.domain.Accounting
import com.hishab.app.ui.AppViewModel
import com.hishab.app.ui.UiState
import com.hishab.app.ui.components.*
import com.hishab.app.ui.sheets.Sheet
import com.hishab.app.ui.theme.HishabTheme
import java.time.ZoneOffset

@Composable
fun CategoriesScreen(vm: AppViewModel, state: UiState, onNavigate: (String) -> Unit) {
    val snap = state.snapshot
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val cats = snap.categories.filter { it.archived == showArchived }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(if (showArchived) "আর্কাইভ করা ক্যাটাগরি" else "সক্রিয় ক্যাটাগরি", style = MaterialTheme.typography.titleMedium); TextButton(onClick = { showArchived = !showArchived }) { Text(if (showArchived) "সক্রিয় দেখুন" else "আর্কাইভ দেখুন") } } }
        items(cats, key = { it.id }) { c ->
            Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(IconRegistry.get(c.icon), IconRegistry.parseColor(c.color), IconRegistry.parseColor(c.color).copy(alpha = .14f))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(c.name, style = MaterialTheme.typography.titleSmall); Text(if (c.kind == EntryKind.EXPENSE) "খরচ" else "আয়", style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted) }
                    IconButton(onClick = { onNavigate("category/${c.id}") }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Visibility, "দেখুন") }
                    IconButton(onClick = { vm.openSheet(Sheet.Category(c.id)) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Edit, "সম্পাদনা") }
                    IconButton(onClick = { vm.archiveCategory(c.id, !c.archived) }, modifier = Modifier.size(48.dp)) { Icon(if (c.archived) Icons.Default.Refresh else Icons.Default.Archive, if (c.archived) "সক্রিয় করুন" else "আর্কাইভ") }
                }
            }
        }
        item { Button(onClick = { vm.openSheet(Sheet.Category()) }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 52.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("নতুন ক্যাটাগরি") } }
    }
}

@Composable
fun DuesScreen(vm: AppViewModel, state: UiState) {
    val snap = state.snapshot
    var tab by rememberSaveable { mutableStateOf(0) }
    val context = LocalContext.current
    val dues = snap.activeTransactions.filter { it.dueDirection != null && snap.remaining(it) > 0 }
    val shown = when (tab) { 0 -> dues.filter { it.dueDirection == DueDirection.PAYABLE }; 1 -> dues.filter { it.dueDirection == DueDirection.RECEIVABLE }; else -> dues }
    val payable = dues.filter { it.dueDirection == DueDirection.PAYABLE }.sumOf { snap.remaining(it) }
    val receivable = dues.filter { it.dueDirection == DueDirection.RECEIVABLE }.sumOf { snap.remaining(it) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = HishabTheme.colors.roseSoft, shape = MaterialTheme.shapes.large, modifier = Modifier.weight(1f)) { Column(Modifier.padding(16.dp)) { Text("আমি দেব", style = MaterialTheme.typography.labelMedium); Text(vm.money(payable), style = MaterialTheme.typography.titleLarge, color = HishabTheme.colors.rose) } }
                Surface(color = HishabTheme.colors.primarySoft, shape = MaterialTheme.shapes.large, modifier = Modifier.weight(1f)) { Column(Modifier.padding(16.dp)) { Text("আমি পাব", style = MaterialTheme.typography.labelMedium); Text(vm.money(receivable), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) } }
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                listOf("দেনা", "পাওনা", "সব").forEachIndexed { i, label -> SegmentedButton(selected = tab == i, onClick = { tab = i }, shape = SegmentedButtonDefaults.itemShape(i, 3)) { Text(label) } }
            }
        }
        if (shown.isEmpty()) item { EmptyState("কোনো বাকি নেই", "বাকি হিসাব যোগ করলে এখানে দেখাবে।", "বাকি যোগ করুন") { vm.openSheet(Sheet.Entry()) } }
        items(shown, key = { it.id }) { tx ->
            val remaining = snap.remaining(tx)
            val percent = if (tx.amountPoisha == 0L) 0 else ((tx.paidPoisha * 100) / tx.amountPoisha).toInt()
            Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(tx.shopOrPerson ?: tx.title, style = MaterialTheme.typography.titleSmall); Text("মূল ${vm.money(tx.amountPoisha)} · পরিশোধ ${vm.money(tx.paidPoisha)}", style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted) }
                        Column(horizontalAlignment = Alignment.End) { Text(vm.money(remaining), style = MaterialTheme.typography.titleMedium, color = if (tx.dueDirection == DueDirection.PAYABLE) HishabTheme.colors.rose else MaterialTheme.colorScheme.primary); Text(if (tx.dueDirection == DueDirection.PAYABLE) "বাকি আছে" else "পাওনা", style = MaterialTheme.typography.labelSmall) }
                    }
                    BudgetBar(percent, BudgetWarning.NONE, Modifier.padding(top = 10.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("${vm.digits(percent.toString())}% পরিশোধিত", style = MaterialTheme.typography.bodySmall); Text(Dates.formatDate(tx.dueDate ?: tx.date, state.settings.bengaliDigits), style = MaterialTheme.typography.bodySmall) }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { vm.openSheet(Sheet.Payment(tx.id)) }) { Text("ইতিহাস / পরিশোধ") }
                        Row {
                            IconButton(onClick = { addCalendar(context, tx.title, tx.dueDate ?: tx.date, remaining) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.CalendarMonth, "ক্যালেন্ডারে যোগ করুন") }
                            Button(onClick = { vm.openSheet(Sheet.Payment(tx.id)) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (tx.dueDirection == DueDirection.PAYABLE) "পরিশোধ করুন" else "গ্রহণ করুন") }
                        }
                    }
                }
            }
        }
    }
}
private fun addCalendar(context: Context, title: String, date: String, amount: Long) {
    val d = Dates.parseDate(date) ?: return
    val start = d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val end = d.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, "বাকি: $title")
        .putExtra(CalendarContract.Events.DESCRIPTION, "বাকি ${com.hishab.app.core.Money.formatPoisha(amount)}")
        .putExtra(CalendarContract.Events.ALL_DAY, true)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
    try { context.startActivity(intent) } catch (_: ActivityNotFoundException) { }
}

@Composable
fun WalletsScreen(vm: AppViewModel, state: UiState) {
    val snap = state.snapshot
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val wallets = snap.wallets.filter { it.archived == showArchived }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(20.dp)) { Text("মোট ওয়ালেট ব্যালেন্স", color = MaterialTheme.colorScheme.onPrimary); Text(vm.money(snap.totalBalance), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimary); Text("প্রারম্ভিক + পরিশোধিত আয় − পরিশোধিত খরচ ± স্থানান্তর", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .85f)) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(if (showArchived) "আর্কাইভ" else "সক্রিয় ওয়ালেট", style = MaterialTheme.typography.titleMedium); TextButton(onClick = { showArchived = !showArchived }) { Text(if (showArchived) "সক্রিয় দেখুন" else "আর্কাইভ দেখুন") } }
        }
        items(wallets, key = { it.id }) { w ->
            Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(IconRegistry.get(w.icon), IconRegistry.parseColor(w.color), IconRegistry.parseColor(w.color).copy(alpha = .14f))
                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(w.name, style = MaterialTheme.typography.titleSmall); Text("প্রারম্ভিক ${vm.money(w.openingBalancePoisha)}", style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted) }
                    Text(vm.money(snap.walletBalance(w.id)), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { vm.openSheet(Sheet.Wallet(w.id)) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Edit, "সম্পাদনা") }
                    IconButton(onClick = { vm.archiveWallet(w.id, !w.archived) }, modifier = Modifier.size(48.dp)) { Icon(if (w.archived) Icons.Default.Refresh else Icons.Default.Archive, if (w.archived) "সক্রিয়" else "আর্কাইভ") }
                }
            }
        }
        item {
            OutlinedButton(onClick = { vm.openSheet(Sheet.Transfer) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 52.dp)) { Icon(Icons.Default.SwapHoriz, null); Spacer(Modifier.width(8.dp)); Text("ওয়ালেট স্থানান্তর") }
            Button(onClick = { vm.openSheet(Sheet.Wallet()) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 52.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("নতুন ওয়ালেট") }
        }
    }
}

@Composable
fun TodosScreen(vm: AppViewModel, state: UiState) {
    val snap = state.snapshot
    val todos = snap.todos.filter { it.deletedAt == null }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
        if (todos.isEmpty()) item { EmptyState("কেনার তালিকা খালি", "যা কিনতে হবে যোগ করুন; পরে খরচে রূপান্তর করা যাবে।", "তালিকা যোগ করুন") { vm.openSheet(Sheet.Todo()) } }
        items(todos, key = { it.id }) { t ->
            Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = t.done, onCheckedChange = { vm.toggleTodo(t.id) }, modifier = Modifier.size(48.dp))
                    Column(Modifier.weight(1f)) { Text(t.title, style = MaterialTheme.typography.titleSmall); Text(buildString { append(snap.categories.firstOrNull { it.id == t.targetCategoryId }?.name ?: ""); t.estimatedPoisha?.let { append(" · ${vm.money(it)}") }; t.dueDate?.let { append(" · ${Dates.formatDate(it, state.settings.bengaliDigits)}") } }, style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted) }
                    if (!t.done && t.convertedTransactionId == null) IconButton(onClick = { vm.openSheet(Sheet.Entry(todoId = t.id)) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ShoppingCart, "খরচে রূপান্তর") }
                    IconButton(onClick = { vm.openSheet(Sheet.Todo(t.id)) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Edit, "সম্পাদনা") }
                    IconButton(onClick = { vm.deleteTodo(t.id) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Delete, "মুছুন", tint = HishabTheme.colors.rose) }
                }
            }
        }
        item { Button(onClick = { vm.openSheet(Sheet.Todo()) }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 52.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("নতুন তালিকা") } }
    }
}

@Composable
fun BudgetsScreen(vm: AppViewModel, state: UiState) {
    val snap = state.snapshot
    val overall = Accounting.overallBudget(state.monthKey, snap.budgets.firstOrNull { it.monthKey == state.monthKey }?.overallLimitPoisha ?: 0, snap.transactions, snap.payments)
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
        item { MonthSelector(vm, state) }
        item {
            Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("মাসের বাজেট", style = MaterialTheme.typography.titleMedium); TextButton(onClick = { vm.openSheet(Sheet.Budget()) }) { Text("সম্পাদনা") } }
                    Text("${vm.money(overall.spent)} / ${vm.money(overall.limit)}", style = MaterialTheme.typography.headlineSmall)
                    BudgetBar(overall.percent, overall.warning, Modifier.padding(top = 10.dp))
                    Text(if (overall.limit == 0L) "বাজেট সেট করা হয়নি" else if (overall.remaining >= 0) "আরও ${vm.money(overall.remaining)} খরচের সুযোগ" else "সীমা অতিক্রম ${vm.money(-overall.remaining)}", style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted, modifier = Modifier.padding(top = 6.dp))
                }
            }
            SectionHeader("ক্যাটাগরি বাজেট")
        }
        items(snap.activeCategories.filter { it.kind == EntryKind.EXPENSE }, key = { it.id }) { c ->
            val limit = snap.categoryBudgets.firstOrNull { it.monthKey == state.monthKey && it.categoryId == c.id }?.limitPoisha ?: 0L
            val bs = Accounting.categoryBudget(state.monthKey, c, limit, snap.transactions, snap.payments)
            Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { IconBubble(IconRegistry.get(c.icon), IconRegistry.parseColor(c.color), IconRegistry.parseColor(c.color).copy(alpha = .14f), 38); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(c.name, style = MaterialTheme.typography.titleSmall); Text("${vm.money(bs.spent)} / ${vm.money(bs.limit)}", style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted) }; Text(if (limit == 0L) "—" else vm.digits("${bs.percent}%"), style = MaterialTheme.typography.titleMedium); IconButton(onClick = { vm.openSheet(Sheet.Budget(c.id)) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Edit, "বাজেট সম্পাদনা") } }
                    if (limit > 0) { BudgetBar(bs.percent, bs.warning, Modifier.padding(top = 8.dp)); Text(when (bs.warning) { BudgetWarning.FULL -> "বাজেট সীমা পূর্ণ"; BudgetWarning.EIGHTY -> "৮০% সতর্কতা পেরিয়েছে"; BudgetWarning.NONE -> "বাজেটের মধ্যে আছে" }, style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted, modifier = Modifier.padding(top = 4.dp)) }
                }
            }
        }
        item { Text("সতর্কতা শুধু অ্যাপের ভেতরে; সীমা পেরোলেও খরচ যোগ করা বন্ধ হবে না।", style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted, modifier = Modifier.padding(top = 12.dp)) }
    }
}
