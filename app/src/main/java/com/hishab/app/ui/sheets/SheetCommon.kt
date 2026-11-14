@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hishab.app.ui.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hishab.app.core.Dates
import com.hishab.app.ui.components.IconBubble
import com.hishab.app.ui.components.IconRegistry
import com.hishab.app.ui.theme.HishabTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun SheetTitle(title: String, subtitle: String? = null) {
    Column(Modifier.padding(bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted)
    }
}

@Composable
fun OptionPicker(label: String, value: String, options: List<Pair<String, String>>, onChange: (String) -> Unit, enabled: Boolean = true) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = MaterialTheme.shapes.medium) {
        Text(options.firstOrNull { it.first == value }?.second ?: value, modifier = Modifier.weight(1f))
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(label) }, text = {
        Column(Modifier.heightIn(max = 360.dp)) {
            options.forEach { (id, name) ->
                TextButton(onClick = { onChange(id); open = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(name, modifier = Modifier.weight(1f)); if (id == value) Icon(Icons.Default.Check, null)
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { open = false }) { Text("বন্ধ") } })
}

@Composable
fun DateField(value: String, onChange: (String) -> Unit, label: String = "তারিখ") {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = MaterialTheme.shapes.medium) {
        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
        Text(Dates.formatDate(value), modifier = Modifier.weight(1f))
    }
    if (open) {
        val initial = Dates.parseDate(value) ?: Dates.today()
        val picker = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { open = false }, confirmButton = {
            TextButton(onClick = { picker.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }; open = false }) { Text("ঠিক আছে") }
        }, dismissButton = { TextButton(onClick = { open = false }) { Text("বাতিল") } }) { DatePicker(state = picker) }
    }
}

@Composable
fun ColorPicker(value: String, onChange: (String) -> Unit) {
    val colors = listOf("#145443", "#2F628A", "#C99B48", "#A83E3E", "#7A5518", "#5F6F65", "#6B4FA0", "#0F766E")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { c ->
            FilterChip(selected = value.equals(c, true), onClick = { onChange(c) }, label = { Text(" ") }, modifier = Modifier.size(40.dp),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IconRegistry.parseColor(c), selectedLabelColor = IconRegistry.parseColor(c)))
        }
    }
}

@Composable
fun IconPicker(value: String, onChange: (String) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Adaptive(64.dp), modifier = Modifier.fillMaxWidth().height(220.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(IconRegistry.choices) { choice ->
            Surface(onClick = { onChange(choice.key) }, shape = MaterialTheme.shapes.medium, color = if (choice.key == value) HishabTheme.colors.primarySoft else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.height(64.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(choice.icon, contentDescription = choice.label, tint = if (choice.key == value) MaterialTheme.colorScheme.primary else HishabTheme.colors.muted)
                    Text(choice.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun SheetActions(onCancel: () -> Unit, onSave: () -> Unit, saveLabel: String = "সংরক্ষণ করুন", saveEnabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("বাতিল") }
        Button(onClick = onSave, enabled = saveEnabled, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text(saveLabel) }
    }
}
