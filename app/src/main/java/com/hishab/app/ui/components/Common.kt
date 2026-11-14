@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hishab.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hishab.app.domain.BudgetWarning
import com.hishab.app.ui.theme.HishabTheme

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(action) }
    }
}

@Composable
fun IconBubble(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, bg: Color, size: Int = 44) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape((size / 3).dp)).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.55).dp))
    }
}

@Composable
fun BudgetBar(percent: Int, warning: BudgetWarning, modifier: Modifier = Modifier) {
    val color = when (warning) { BudgetWarning.FULL -> HishabTheme.colors.rose; BudgetWarning.EIGHTY -> HishabTheme.colors.gold; BudgetWarning.NONE -> MaterialTheme.colorScheme.primary }
    LinearProgressIndicator(
        progress = { (percent.coerceIn(0, 100) / 100f) },
        modifier = modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)).semantics { contentDescription = "বাজেট $percent শতাংশ ব্যবহৃত" },
        color = color, trackColor = HishabTheme.colors.surface2
    )
}

@Composable
fun EmptyState(title: String, message: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = HishabTheme.colors.muted)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(action) }
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, destructive: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(if (destructive) Icons.Default.Warning else Icons.Default.Check, contentDescription = null) },
        title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = 48.dp)) { Text(confirm, color = if (destructive) HishabTheme.colors.rose else MaterialTheme.colorScheme.primary) } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("বাতিল") } }
    )
}

@Composable
fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = HishabTheme.colors.muted, modifier = Modifier.padding(bottom = 6.dp))
        content()
    }
}

@Composable
fun ErrorText(text: String?) {
    if (!text.isNullOrBlank()) Text(text, color = HishabTheme.colors.rose, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
}

@Composable
fun MoneyText(amount: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium) {
    Text(amount, modifier = modifier, color = color, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
}
