@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hishab.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hishab.app.ai.AssistantDraft
import com.hishab.app.ui.AppViewModel
import com.hishab.app.ui.ChatMessage
import com.hishab.app.ui.components.ConfirmDialog
import com.hishab.app.ui.sheets.Sheet
import com.hishab.app.ui.theme.HishabTheme

@Composable
fun AssistantScreen(vm: AppViewModel) {
    val chat by vm.chat.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf<AssistantDraft.DeleteTransaction?>(null) }
    val listState = rememberLazyListState()
    LaunchedEffect(chat.size, draft) { if (chat.isNotEmpty()) listState.animateScrollToItem(chat.lastIndex) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(bottom = 84.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Surface(color = HishabTheme.colors.primarySoft, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Column { Text("লোকাল মোড চালু", style = MaterialTheme.typography.titleSmall); Text("ক্লাউড AI বন্ধ · তথ্য বাইরে পাঠানো হচ্ছে না", style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted) } } } }
            if (chat.isEmpty()) item { Text("যেমন: আজ বাজারে ৫০০ টাকা খরচ। রহিম স্টোরের বাকি থেকে ৫০০ টাকা নগদে পরিশোধ করো।", style = MaterialTheme.typography.bodyMedium, color = HishabTheme.colors.muted, modifier = Modifier.padding(8.dp)) }
            items(chat) { msg ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.role == "user") Arrangement.End else Arrangement.Start) {
                    Surface(color = if (msg.role == "user") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large, modifier = Modifier.widthIn(max = 320.dp)) {
                        Text(msg.text, modifier = Modifier.padding(14.dp), color = if (msg.role == "user") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            draft?.let { d ->
                item {
                    Surface(shape = MaterialTheme.shapes.extraLarge, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.SmartToy, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text(d.summary, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
                            d.lines.forEach { (k, v) -> Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Text(k, modifier = Modifier.weight(1f), color = HishabTheme.colors.muted, style = MaterialTheme.typography.bodySmall); Text(v, style = MaterialTheme.typography.bodySmall) } }
                            Surface(color = HishabTheme.colors.goldSoft, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("আপনি নিশ্চিত না করলে খাতা বদলাবে না। ভুল বুঝলে বাতিল বা সম্পাদনা করুন।", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.goldInk) }
                            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = vm::clearDraft, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("বাতিল") }
                                if (d is AssistantDraft.AddTransaction) OutlinedButton(onClick = { vm.openSheet(Sheet.Entry(draft = d)); vm.clearDraft() }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)); Text("সম্পাদনা") }
                                Button(onClick = { if (d is AssistantDraft.DeleteTransaction) confirmDelete = d else vm.confirmDraft(d) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("নিশ্চিত") }
                            }
                        }
                    }
                }
            }
        }
        Surface(tonalElevation = 3.dp, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(input, { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("হিসাব লিখুন…") }, maxLines = 3)
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = { vm.sendAssistant(input); input = "" }, modifier = Modifier.size(52.dp)) { Icon(Icons.AutoMirrored.Filled.Send, "পাঠান") }
            }
        }
    }
    confirmDelete?.let { d -> ConfirmDialog("লেনদেন মুছবেন?", d.lines.joinToString("\n") { "${it.first}: ${it.second}" }, "মুছুন", { vm.confirmDraft(d); confirmDelete = null }, { confirmDelete = null }, true) }
}
