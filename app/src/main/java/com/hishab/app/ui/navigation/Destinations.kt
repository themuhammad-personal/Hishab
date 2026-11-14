@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hishab.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class Destination(val route: String, val label: String, val icon: ImageVector)
val drawerDestinations = listOf(
    Destination("home", "হোম", Icons.Default.Home),
    Destination("transactions", "লেনদেন", Icons.AutoMirrored.Filled.ReceiptLong),
    Destination("categories", "ক্যাটাগরি", Icons.Default.Category),
    Destination("dues", "বাকির খাতা", Icons.Default.Schedule),
    Destination("todos", "কেনার তালিকা", Icons.Default.Checklist),
    Destination("wallets", "ওয়ালেট", Icons.Default.AccountBalanceWallet),
    Destination("budgets", "বাজেট", Icons.Default.GpsFixed),
    Destination("reports", "রিপোর্ট", Icons.Default.BarChart),
    Destination("assistant", "হিসাব সহকারী", Icons.Default.SmartToy),
    Destination("settings", "সেটিংস", Icons.Default.Settings)
)
fun titleFor(route: String?): String = when {
    route == null -> "হিসাব"
    route.startsWith("category/") -> "ক্যাটাগরি বিস্তারিত"
    else -> drawerDestinations.firstOrNull { it.route == route }?.label ?: "হিসাব"
}
