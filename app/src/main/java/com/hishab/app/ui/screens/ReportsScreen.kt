@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hishab.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.hishab.app.core.Dates
import com.hishab.app.domain.Accounting
import com.hishab.app.domain.ReportMath
import java.time.YearMonth
import com.hishab.app.ui.AppViewModel
import com.hishab.app.ui.UiState
import com.hishab.app.ui.theme.HishabTheme

@Composable
fun ReportsScreen(vm: AppViewModel, state: UiState) {
    val snap = state.snapshot
    val summary = Accounting.monthSummary(state.monthKey, snap.transactions, snap.payments)
    val spend = Accounting.categorySpend(state.monthKey, snap.transactions, snap.payments)
    val slices = ReportMath.percentLabels(spend.map { s -> (snap.categories.firstOrNull { it.id == s.categoryId }?.name ?: "অন্যান্য") to s.amount })
    val colors = listOf(HishabTheme.colors.gold, HishabTheme.colors.blue, MaterialTheme.colorScheme.primary, HishabTheme.colors.rose, HishabTheme.colors.muted)
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let(vm::exportCsv) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
        item { MonthSelector(vm, state) }
        item {
            Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("মাসের মোট খরচ · বাকিসহ", style = MaterialTheme.typography.labelMedium, color = HishabTheme.colors.muted)
                    Text(vm.money(summary.expenseTotal), style = MaterialTheme.typography.headlineMedium)
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) { Text("পরিশোধিত ${vm.money(summary.expensePaid)}", style = MaterialTheme.typography.bodySmall); Text("বাকি ${vm.money(summary.expenseUnpaid)}", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        item {
            Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("ক্যাটাগরি অনুযায়ী", style = MaterialTheme.typography.titleMedium)
                    if (slices.isEmpty()) Text("এই মাসে কোনো খরচ নেই।", style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted, modifier = Modifier.padding(top = 12.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.fillMaxSize()) {
                                val stroke = 22.dp.toPx()
                                val diameter = size.minDimension - stroke
                                val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                                var start = -90f
                                val total = slices.sumOf { it.value }.coerceAtLeast(1)
                                slices.forEachIndexed { i, s ->
                                    val sweep = 360f * s.value / total
                                    drawArc(colors[i % colors.size], start, sweep, false, topLeft, Size(diameter, diameter), style = Stroke(stroke))
                                    start += sweep
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(vm.money(summary.expenseTotal, symbol = false), style = MaterialTheme.typography.titleMedium); Text("মোট খরচ", style = MaterialTheme.typography.labelSmall, color = HishabTheme.colors.muted) }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            slices.forEachIndexed { i, s -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Canvas(Modifier.size(10.dp)) { drawCircle(colors[i % colors.size]) }; Spacer(Modifier.width(8.dp)); Text(s.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall); Text(vm.digits("${s.percent}%"), style = MaterialTheme.typography.labelMedium) } }
                        }
                    }
                }
            }
        }
        item {
            Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                Column(Modifier.padding(18.dp)) {
                    val today = Dates.today()
                    val currentMonth = Dates.monthKey(today) == state.monthKey
                    val anchor = if (currentMonth) today else YearMonth.parse(state.monthKey).atEndOfMonth()
                    Text(if (currentMonth) "শেষ ৭ দিনের খরচ" else "${vm.monthLabel()} · শেষ ৭ দিন", style = MaterialTheme.typography.titleMedium)
                    val days = (6 downTo 0).map { anchor.minusDays(it.toLong()) }
                    val values = days.map { d -> snap.activeTransactions.filter { it.kind == com.hishab.app.domain.EntryKind.EXPENSE && it.date == d.toString() }.sumOf { it.amountPoisha } }
                    val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
                    val barTrack = HishabTheme.colors.primarySoft
                    Row(Modifier.fillMaxWidth().height(130.dp).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                        days.forEachIndexed { i, d ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                Canvas(Modifier.fillMaxWidth().weight(1f)) {
                                    val h = (size.height * values[i] / max).coerceAtLeast(2f)
                                    drawRoundRect(if (i == days.lastIndex) colors[2] else barTrack, topLeft = Offset(0f, size.height - h), size = Size(size.width, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f))
                                }
                                Text(vm.digits(d.dayOfMonth.toString()), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            OutlinedButton(onClick = { csvLauncher.launch("hishab-${state.monthKey}.csv") }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp).heightIn(min = 52.dp)) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("CSV এক্সপোর্ট") }
            Text("রিপোর্টের খরচ বাকিসহ; ওয়ালেট কমে শুধু প্রকৃত পরিশোধে।", style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
