package com.credimax.app.ui.screens.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.credimax.app.CredimaxApp
import com.credimax.app.ui.components.CredimaxCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onQuotes: () -> Unit,
    onQuotesReport: () -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as CredimaxApp
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { TopAppBar(title = { Text("Más") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CredimaxCard(onClick = onQuotes) {
                ListItem(
                    headlineContent = { Text("Presupuestos") },
                    supportingContent = { Text("Propuestas que pueden convertirse en préstamo") },
                    leadingContent = { Icon(Icons.Outlined.Description, null) },
                )
            }
            CredimaxCard(onClick = onQuotesReport) {
                ListItem(
                    headlineContent = { Text("Presupuestos abiertos") },
                    supportingContent = { Text("Reporte de propuestas aún no emitidas") },
                    leadingContent = { Icon(Icons.Outlined.Description, null) },
                )
            }
            CredimaxCard(onClick = onReports) {
                ListItem(
                    headlineContent = { Text("Reportes") },
                    supportingContent = { Text("Intereses, deudas y cobros retrasados") },
                    leadingContent = { Icon(Icons.Outlined.Summarize, null) },
                )
            }
            CredimaxCard(onClick = onSettings) {
                ListItem(
                    headlineContent = { Text("Configuración") },
                    supportingContent = { Text("Interés y plazos por defecto") },
                    leadingContent = { Icon(Icons.Outlined.Settings, null) },
                )
            }
            CredimaxCard(onClick = {
                scope.launch {
                    app.container.tokenStore.clear()
                    onLogout()
                }
            }) {
                ListItem(
                    headlineContent = { Text("Cerrar sesión", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Outlined.Logout, null, tint = MaterialTheme.colorScheme.error) },
                )
            }
        }
    }
}
