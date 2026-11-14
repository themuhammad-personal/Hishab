@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hishab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hishab.app.ui.AppViewModel
import com.hishab.app.ui.navigation.drawerDestinations
import com.hishab.app.ui.navigation.titleFor
import com.hishab.app.ui.screens.*
import com.hishab.app.ui.sheets.*
import com.hishab.app.ui.theme.HishabTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { HishabApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HishabApp(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val sheet by vm.sheet.collectAsStateWithLifecycle()
    HishabTheme(state.settings.themeMode) {
        val nav = rememberNavController()
        val backStack by nav.currentBackStackEntryAsState()
        val route = backStack?.destination?.route
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(vm) {
            vm.events.collect { event ->
                val result = snackbar.showSnackbar(event.message, actionLabel = if (event.undo != null) "ফিরিয়ে আনুন" else null, duration = SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed) event.undo?.invoke()
            }
        }
        ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
            ModalDrawerSheet(Modifier.width(320.dp)) {
                Column(Modifier.padding(24.dp)) { Text("হিসাব", style = MaterialTheme.typography.headlineSmall); Text("আপনার খাতা, আপনার নিয়ন্ত্রণে", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                drawerDestinations.forEach { d ->
                    NavigationDrawerItem(icon = { Icon(d.icon, null) }, label = { Text(d.label) }, selected = route == d.route || (d.route == "categories" && route?.startsWith("category/") == true), onClick = {
                        scope.launch { drawerState.close() }
                        nav.navigate(d.route) { launchSingleTop = true; popUpTo("home") { saveState = true }; restoreState = true }
                    }, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.padding(20.dp)) { Icon(Icons.Default.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Column { Text("ডিভাইসে সংরক্ষিত", style = MaterialTheme.typography.titleSmall); Text("ক্লাউড সিঙ্ক এখন বন্ধ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            }
        }) {
            Scaffold(
                topBar = { TopAppBar(title = { Text(titleFor(route)) }, navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Menu, "মেনু") } }) },
                floatingActionButton = {
                    when {
                        route == "home" || route == "transactions" || route?.startsWith("category/") == true -> ExtendedFloatingActionButton(onClick = { vm.openSheet(Sheet.Entry()) }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("নতুন হিসাব") }
                        route == "dues" -> ExtendedFloatingActionButton(onClick = { vm.openSheet(Sheet.Entry()) }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("নতুন বাকি") }
                        route == "wallets" -> ExtendedFloatingActionButton(onClick = { vm.openSheet(Sheet.Wallet()) }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("নতুন ওয়ালেট") }
                        route == "todos" -> ExtendedFloatingActionButton(onClick = { vm.openSheet(Sheet.Todo()) }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("নতুন তালিকা") }
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) }
            ) { padding ->
                NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
                    composable("home") { HomeScreen(vm, state) { nav.navigate(it) { launchSingleTop = true } } }
                    composable("transactions") { TransactionsScreen(vm, state) }
                    composable("categories") { CategoriesScreen(vm, state) { nav.navigate(it) } }
                    composable("category/{categoryId}", arguments = listOf(navArgument("categoryId") { type = NavType.StringType })) { entry -> TransactionsScreen(vm, state, entry.arguments?.getString("categoryId")) }
                    composable("dues") { DuesScreen(vm, state) }
                    composable("wallets") { WalletsScreen(vm, state) }
                    composable("todos") { TodosScreen(vm, state) }
                    composable("budgets") { BudgetsScreen(vm, state) }
                    composable("reports") { ReportsScreen(vm, state) }
                    composable("assistant") { AssistantScreen(vm) }
                    composable("settings") { SettingsScreen(vm, state) }
                }
            }
        }
        when (val s = sheet) {
            is Sheet.Entry -> EntrySheetHost(vm, s)
            is Sheet.Payment -> PaymentSheetHost(vm, s)
            is Sheet.Wallet -> WalletSheetHost(vm, s)
            is Sheet.Category -> CategorySheetHost(vm, s)
            is Sheet.Todo -> TodoSheetHost(vm, s)
            is Sheet.Transfer -> TransferSheetHost(vm)
            is Sheet.Budget -> BudgetSheetHost(vm, s)
            null -> Unit
        }
    }
}
