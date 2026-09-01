package com.credimax.app.ui.screens.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.credimax.app.CredimaxApp
import com.credimax.app.data.QuotesReportDto
import com.credimax.app.ui.components.AmountRow
import com.credimax.app.ui.components.CredimaxCard
import com.credimax.app.ui.components.EmptyText
import com.credimax.app.ui.components.KpiTile
import com.credimax.app.ui.components.LoadingBox
import com.credimax.app.ui.components.StatusChip
import com.credimax.app.ui.money
import com.credimax.app.ui.quoteStatusLabel
import com.credimax.app.ui.theme.SlateMuted
import com.credimax.app.ui.theme.TealDeep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotesReportScreen(onQuote: (String) -> Unit, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as CredimaxApp
    var data by remember { mutableStateOf<QuotesReportDto?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        data = runCatching { app.container.api.reportQuotes() }.getOrNull()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Presupuestos abiertos") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) }
                },
            )
        },
    ) { padding ->
        when {
            loading -> LoadingBox()
            data == null -> EmptyText("No se pudo cargar el reporte.")
            else -> {
                val d = data!!
                LazyColumn(
                    Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            KpiTile("Abiertos", d.kpis.abiertos.toString(), TealDeep, Modifier.weight(1f))
                            KpiTile("Capital potencial", money(d.kpis.capitalPotencial), TealDeep, Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            KpiTile("Interés proyectado", money(d.kpis.interesProyectado), TealDeep, Modifier.weight(1f))
                            KpiTile("Total potencial", money(d.kpis.totalPotencial), TealDeep, Modifier.weight(1f))
                        }
                    }
                    if (d.quotes.isEmpty()) {
                        item { EmptyText("No hay presupuestos abiertos.") }
                    }
                    items(d.quotes, key = { it.id }) { q ->
                        CredimaxCard(onClick = { onQuote(q.id) }) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(q.client?.nombre ?: "Cliente", style = MaterialTheme.typography.titleMedium)
                                    StatusChip(quoteStatusLabel(q.estado), q.estado)
                                }
                                Text("${money(q.capital)} · ${q.interesPct}% · ${q.semanas} sem.", color = SlateMuted)
                                AmountRow("Potencial", money(q.totalPagar), emphasis = true)
                            }
                        }
                    }
                }
            }
        }
    }
}
