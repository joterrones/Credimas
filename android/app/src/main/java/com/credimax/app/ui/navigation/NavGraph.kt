package com.credimax.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.credimax.app.CredimaxApp
import com.credimax.app.ui.screens.clients.ClientActionsScreen
import com.credimax.app.ui.screens.clients.ClientFormScreen
import com.credimax.app.ui.screens.clients.ClientLoansScreen
import com.credimax.app.ui.screens.clients.ClientQuotesScreen
import com.credimax.app.ui.screens.clients.ClientsScreen
import com.credimax.app.ui.screens.home.HomeScreen
import com.credimax.app.ui.screens.loans.LoanDetailScreen
import com.credimax.app.ui.screens.loans.LoansScreen
import com.credimax.app.ui.screens.loans.PayScreen
import com.credimax.app.ui.screens.login.LoginScreen
import com.credimax.app.ui.screens.more.MoreScreen
import com.credimax.app.ui.screens.quotes.QuoteFormScreen
import com.credimax.app.ui.screens.quotes.QuotesScreen
import com.credimax.app.ui.screens.reports.QuotesReportScreen
import com.credimax.app.ui.screens.reports.ReportsScreen
import com.credimax.app.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.flow.map

data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("home", "Inicio", Icons.Outlined.Home),
    Tab("clients", "Clientes", Icons.Outlined.Groups),
    Tab("loans", "Préstamos", Icons.Outlined.AccountBalanceWallet),
    Tab("more", "Más", Icons.Outlined.MoreHoriz),
)

@Composable
fun CredimaxNav() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as CredimaxApp
    val loggedIn by app.container.tokenStore.token.map { !it.isNullOrBlank() }.collectAsState(initial = false)

    LaunchedEffect(loggedIn) {
        if (!loggedIn) {
            nav.navigate("login") {
                popUpTo(nav.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBar = current in tabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = if (loggedIn) "home" else "login",
            modifier = Modifier.padding(padding),
        ) {
            composable("login") {
                LoginScreen(onLoggedIn = {
                    nav.navigate("home") { popUpTo("login") { inclusive = true } }
                })
            }
            composable("home") {
                HomeScreen(
                    onNewClient = { nav.navigate("clientForm?id=") },
                    onNewQuote = { nav.navigate("quoteForm?id=&clientId=&asLoan=false") },
                    onLoan = { nav.navigate("loan/$it") },
                    onReports = { nav.navigate("reports") },
                )
            }
            composable("clients") {
                ClientsScreen(
                    onNew = { nav.navigate("clientForm?id=") },
                    onOpen = { nav.navigate("client/$it") },
                )
            }
            composable("loans") {
                LoansScreen(onOpen = { nav.navigate("loan/$it") })
            }
            composable("more") {
                MoreScreen(
                    onQuotes = { nav.navigate("quotes") },
                    onQuotesReport = { nav.navigate("quotesReport") },
                    onReports = { nav.navigate("reports") },
                    onSettings = { nav.navigate("settings") },
                    onLogout = {
                        // token cleared in screen
                    },
                )
            }
            composable("quotes") {
                QuotesScreen(
                    onNew = { nav.navigate("quoteForm?id=&clientId=&asLoan=false") },
                    onEdit = { nav.navigate("quoteForm?id=$it&clientId=&asLoan=false") },
                    onLoan = { nav.navigate("loan/$it") },
                )
            }
            composable("reports") {
                ReportsScreen(
                    onLoan = { nav.navigate("loan/$it") },
                    onQuotesReport = { nav.navigate("quotesReport") },
                )
            }
            composable("quotesReport") {
                QuotesReportScreen(
                    onQuote = { nav.navigate("quoteForm?id=$it&clientId=&asLoan=false") },
                    onBack = { nav.popBackStack() },
                )
            }
            composable("settings") { SettingsScreen() }
            composable(
                route = "clientForm?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                ClientFormScreen(
                    clientId = entry.arguments?.getString("id").orEmpty(),
                    onDone = { nav.popBackStack() },
                )
            }
            composable(
                route = "client/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                ClientActionsScreen(
                    clientId = id,
                    onEdit = { nav.navigate("clientForm?id=$id") },
                    onQuotes = { nav.navigate("client/$id/quotes") },
                    onLoans = { nav.navigate("client/$id/loans") },
                    onNewQuote = { nav.navigate("quoteForm?id=&clientId=$id&asLoan=false") },
                    onNewLoan = { nav.navigate("quoteForm?id=&clientId=$id&asLoan=true") },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                route = "client/{id}/quotes",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                ClientQuotesScreen(
                    clientId = id,
                    onEdit = { nav.navigate("quoteForm?id=$it&clientId=$id&asLoan=false") },
                    onLoan = { nav.navigate("loan/$it") },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                route = "client/{id}/loans",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                ClientLoansScreen(
                    clientId = id,
                    onOpen = { nav.navigate("loan/$it") },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                route = "quoteForm?id={id}&clientId={clientId}&asLoan={asLoan}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType; defaultValue = "" },
                    navArgument("clientId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("asLoan") { type = NavType.StringType; defaultValue = "false" },
                ),
            ) { entry ->
                QuoteFormScreen(
                    quoteId = entry.arguments?.getString("id").orEmpty(),
                    presetClientId = entry.arguments?.getString("clientId").orEmpty(),
                    asLoan = entry.arguments?.getString("asLoan") == "true",
                    onDone = { nav.popBackStack() },
                    onConverted = { loanId ->
                        nav.popBackStack()
                        nav.navigate("loan/$loanId")
                    },
                )
            }
            composable("loan/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                LoanDetailScreen(
                    loanId = id,
                    onPay = { instId -> nav.navigate("pay/$id/$instId") },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                "pay/{loanId}/{installmentId}",
                arguments = listOf(
                    navArgument("loanId") { type = NavType.StringType },
                    navArgument("installmentId") { type = NavType.StringType },
                ),
            ) { entry ->
                PayScreen(
                    loanId = entry.arguments?.getString("loanId").orEmpty(),
                    installmentId = entry.arguments?.getString("installmentId").orEmpty(),
                    onDone = { nav.popBackStack() },
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
