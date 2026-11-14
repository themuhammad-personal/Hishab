@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hishab.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hishab.app.data.repository.BackupPreview
import com.hishab.app.domain.ThemeMode
import com.hishab.app.ui.AppViewModel
import com.hishab.app.ui.UiState
import com.hishab.app.ui.components.ConfirmDialog
import com.hishab.app.ui.components.SectionHeader
import com.hishab.app.ui.theme.HishabTheme

@Composable
fun SettingsScreen(vm: AppViewModel, state: UiState) {
    val context = LocalContext.current
    var preview by remember { mutableStateOf<BackupPreview?>(null) }
    var reset by rememberSaveable { mutableStateOf(false) }
    val jsonExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::exportJson) }
    val csvExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let(vm::exportCsv) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { preview = vm.readBackup(it) } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> vm.setReminders(granted) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { SectionHeader("চেহারা ও ভাষা") }
        item {
            Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("থিম", style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        ThemeMode.entries.forEachIndexed { i, m -> SegmentedButton(selected = state.settings.themeMode == m, onClick = { vm.setTheme(m) }, shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size)) { Text(when (m) { ThemeMode.LIGHT -> "লাইট"; ThemeMode.DARK -> "ডার্ক"; ThemeMode.SYSTEM -> "সিস্টেম" }) } }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("বাংলা সংখ্যা", style = MaterialTheme.typography.titleSmall); Text("বন্ধ করলে English digits", style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted) }; Switch(checked = state.settings.bengaliDigits, onCheckedChange = vm::setDigits) }
                }
            }
        }
        item { SectionHeader("রিমাইন্ডার") }
        item {
            Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Notifications, null); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("বাকির তারিখে মনে করিয়ে দিন", style = MaterialTheme.typography.titleSmall); Text("স্থানীয় নোটিফিকেশন · অনুমতি সাপেক্ষে", style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted) }
                    Switch(checked = state.settings.remindersEnabled, onCheckedChange = { want ->
                        if (!want) vm.setReminders(false)
                        else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else vm.setReminders(true)
                    })
                }
            }
        }
        item { SectionHeader("ব্যাকআপ ও ডেটা") }
        item {
            SettingsAction(Icons.Default.Download, "JSON ব্যাকআপ", "সম্পূর্ণ খাতা ফাইলে সংরক্ষণ") { jsonExport.launch("hishab-backup-${state.monthKey}.json") }
            SettingsAction(Icons.Default.Description, "CSV এক্সপোর্ট", "স্প্রেডশিটের জন্য · UTF-8") { csvExport.launch("hishab-${state.monthKey}.csv") }
            SettingsAction(Icons.Default.Upload, "ব্যাকআপ রিস্টোর", "আগে যাচাই ও প্রিভিউ, তারপর মার্জ/রিপ্লেস") { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
            SettingsAction(Icons.Default.Delete, "খাতা রিসেট", "সব লেনদেন, ওয়ালেট, ক্যাটাগরি ও বাজেট মুছবে", destructive = true) { reset = true }
        }
        item { SectionHeader("গোপনীয়তা ও ক্লাউড") }
        item {
            Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CloudOff, null); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("ক্লাউড AI", style = MaterialTheme.typography.titleSmall); Text("এই সংস্করণে বন্ধ; চালু হলে আলাদা সম্মতি লাগবে", style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted) }; Switch(checked = false, onCheckedChange = {}, enabled = false) }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = HishabTheme.colors.border)
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CloudOff, null); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("Firebase সিঙ্ক", style = MaterialTheme.typography.titleSmall); Text("প্রজেক্ট যাচাইয়ের পর চালু হবে", style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted) }; Switch(checked = false, onCheckedChange = {}, enabled = false) }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = HishabTheme.colors.border)
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Text("অ্যানালিটিক্স ও বিজ্ঞাপন SDK নেই; আর্থিক তথ্য ডিভাইসেই থাকে।", style = MaterialTheme.typography.bodySmall) }
                }
            }
            Text("হিসাব ১.০.০ · অফলাইন-ফার্স্ট", style = MaterialTheme.typography.labelSmall, color = HishabTheme.colors.muted, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
    preview?.let { p ->
        AlertDialog(onDismissRequest = { preview = null }, title = { Text("ব্যাকআপ যাচাই হয়েছে") }, text = {
            Column { Text("লেনদেন ${vm.digits(p.activeTransactions.toString())}, ওয়ালেট ${vm.digits(p.walletCount.toString())}, ক্যাটাগরি ${vm.digits(p.categoryCount.toString())}, পরিশোধ ${vm.digits(p.paymentCount.toString())}, স্থানান্তর ${vm.digits(p.transferCount.toString())}, তালিকা ${vm.digits(p.todoCount.toString())}")
                p.warnings.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.goldInk, modifier = Modifier.padding(top = 6.dp)) }
                Text("মার্জ: একই id আপডেট হবে, নতুনগুলো যুক্ত হবে। রিপ্লেস: বর্তমান খাতা মুছে ব্যাকআপ বসবে।", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
        }, confirmButton = { TextButton(onClick = { vm.importBackup(p, false); preview = null }) { Text("মার্জ") } }, dismissButton = { Row { TextButton(onClick = { preview = null }) { Text("বাতিল") }; TextButton(onClick = { vm.importBackup(p, true); preview = null }) { Text("রিপ্লেস", color = HishabTheme.colors.rose) } } })
    }
    if (reset) ConfirmDialog("পুরো খাতা রিসেট করবেন?", "এই কাজ ফিরিয়ে আনা যাবে না। আগে JSON ব্যাকআপ নেওয়া ভালো।", "রিসেট", { vm.resetLedger(); reset = false }, { reset = false }, true)
}

@Composable
private fun SettingsAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, destructive: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = if (destructive) HishabTheme.colors.rose else MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleSmall, color = if (destructive) HishabTheme.colors.rose else MaterialTheme.colorScheme.onSurface); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted) }; Icon(Icons.Default.KeyboardArrowRight, null) }
    }
}
