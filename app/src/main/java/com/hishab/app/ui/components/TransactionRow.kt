@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hishab.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hishab.app.core.Dates
import com.hishab.app.data.local.TransactionEntity
import com.hishab.app.domain.EntryKind
import com.hishab.app.domain.paymentStatus
import com.hishab.app.domain.PaymentStatus
import com.hishab.app.ui.theme.HishabTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTransactionRow(tx: TransactionEntity, categoryName: String, categoryIcon: String, walletName: String?, amount: String, bengaliDigits: Boolean, onEdit: () -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
        when (value) {
            SwipeToDismissBoxValue.StartToEnd -> { onEdit(); false }
            SwipeToDismissBoxValue.EndToStart -> { onDelete(); false }
            SwipeToDismissBoxValue.Settled -> false
        }
    })
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val dir = state.dismissDirection
            Row(Modifier.fillMaxSize().background(if (dir == SwipeToDismissBoxValue.EndToStart) HishabTheme.colors.roseSoft else HishabTheme.colors.primarySoft).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (dir == SwipeToDismissBoxValue.EndToStart) Arrangement.End else Arrangement.Start) {
                Icon(if (dir == SwipeToDismissBoxValue.EndToStart) Icons.Default.Delete else Icons.Default.Edit, contentDescription = null, tint = if (dir == SwipeToDismissBoxValue.EndToStart) HishabTheme.colors.rose else MaterialTheme.colorScheme.primary)
            }
        }
    ) {
        TransactionRow(tx, categoryName, categoryIcon, walletName, amount, bengaliDigits, onEdit, onDelete, onClick)
    }
}

@Composable
fun TransactionRow(tx: TransactionEntity, categoryName: String, categoryIcon: String, walletName: String?, amount: String, bengaliDigits: Boolean, onEdit: () -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    val status = paymentStatus(tx.amountPoisha, tx.paidPoisha)
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBubble(IconRegistry.get(categoryIcon), if (tx.kind == EntryKind.INCOME) MaterialTheme.colorScheme.primary else HishabTheme.colors.goldInk, if (tx.kind == EntryKind.INCOME) HishabTheme.colors.primarySoft else HishabTheme.colors.goldSoft)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tx.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text("${Dates.relativeLabel(tx.date, bengaliDigits = bengaliDigits)} · $categoryName${walletName?.let { " · $it" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = HishabTheme.colors.muted, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(amount, color = if (tx.kind == EntryKind.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                Text(when (status) { PaymentStatus.PAID -> if (tx.kind == EntryKind.INCOME) "প্রাপ্ত" else "পরিশোধিত"; PaymentStatus.PARTIAL -> "আংশিক"; PaymentStatus.UNPAID -> "বাকি" }, style = MaterialTheme.typography.labelSmall, color = if (status == PaymentStatus.PAID) HishabTheme.colors.muted else HishabTheme.colors.rose)
            }
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.MoreVert, contentDescription = "লেনদেনের কাজ") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("সম্পাদনা") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { menuOpen = false; onEdit() })
                    DropdownMenuItem(text = { Text("মুছুন") }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = HishabTheme.colors.rose) }, onClick = { menuOpen = false; onDelete() })
                }
            }
        }
    }
}
