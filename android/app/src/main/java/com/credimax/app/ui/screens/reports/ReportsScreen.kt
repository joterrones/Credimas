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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.credimax.app.data.DashboardDto
import com.credimax.app.data.LoanDto
import com.credimax.app.ui.components.AmountRow
import com.credimax.app.ui.components.CredimaxCard
import com.credimax.app.ui.components.EmptyText
import com.credimax.app.ui.components.KpiTile
import com.credimax.app.ui.components.LoadingBox
import com.credimax.app.ui.components.StatusChip
import com.credimax.app.ui.dueDateLabel
import com.credimax.app.ui.loanStatusLabel
import com.credimax.app.ui.money
import com.credimax.app.ui.weeksLateNow
import com.credimax.app.ui.weeksLateText
import com.credimax.app.ui.theme.AmberAlert
import com.credimax.app.ui.theme.SlateMuted
import com.credimax.app.ui.theme.Success
import com.credimax.app.ui.theme.TealDeep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(onLoan: (String) -> Unit, onQuotesReport: () -> Unit) {
    val app = LocalContext.current.applicationContext as CredimaxApp
    var dash by remember { mutableStateOf<DashboardDto?>(null) }
    var loans by remember { mutableStateOf<List<LoanDto>>(emptyList()) }
    var filter by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(filter) {
        loading = true
        dash = runCatching { app.container.api.dashboard() }.getOrNull()
        loans = runCatching { app.container.api.reportLoans(filter) }.getOrDefault(emptyList())
        loading = false
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Reportes") }) }) { padding ->
        if (loading && dash == null) LoadingBox()
        else LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val d = dash
            if (d != null) {
                item { Text("Cartera", style = MaterialTheme.typography.headlineMedium) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KpiTile("Capital", money(d.cartera.capital), TealDeep, Modifier.weight(1f))
                        KpiTile("Montos cobrados", money(d.cartera.montosCobrados), Success, Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KpiTile("Cuentas por cobrar", money(d.cartera.cuentasPorCobrar), AmberAlert, Modifier.weight(1f))
                        KpiTile("Interés", money(d.cartera.interes), Success, Modifier.weight(1f))
                    }
                }
                item {
                    CredimaxCard(onClick = onQuotesReport) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Presupuestos abiertos", style = MaterialTheme.typography.titleMedium)
                            Text("Capital e interés potencial aún no emitido", color = SlateMuted)
                        }
                    }
                }
                item {
                    CredimaxCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Préstamos por estado", style = MaterialTheme.typography.titleMedium)
                            AmountRow("Al día", d.prestamosPorEstado.al_dia.toString())
                            AmountRow("Atrasados", d.prestamosPorEstado.atrasado.toString())
                            AmountRow("Pagados", d.prestamosPorEstado.pagado.toString())
                        }
                    }
                }
                item { Text("Cobros retrasados", style = MaterialTheme.typography.titleLarge) }
                if (d.cobrosRetrasados.isEmpty()) {
                    item { EmptyText("No hay letras atrasadas.") }
                }
                items(d.cobrosRetrasados, key = { it.id }) { inst ->
                    val weeks = weeksLateText(weeksLateNow(inst.fechaVencimiento))
                    CredimaxCard(onClick = { inst.loan?.id?.let(onLoan) }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(inst.loan?.client?.nombre ?: "Cliente", style = MaterialTheme.typography.titleMedium)
                            Text("Letra ${inst.nro} · ${dueDateLabel(inst.fechaVencimiento)}", color = SlateMuted)
                            if (weeks != null) {
                                Text(weeks, color = AmberAlert, style = MaterialTheme.typography.titleSmall)
                            }
                            AmountRow("Adeudado", money(inst.totalAdeudado ?: (inst.monto + inst.recargoAcumulado)), emphasis = true)
                        }
                    }
                }
            }
            item { Text("Cartera", style = MaterialTheme.typography.titleLarge) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Todos") })
                    FilterChip(selected = filter == "atrasado", onClick = { filter = "atrasado" }, label = { Text("Atrasados") })
                    FilterChip(selected = filter == "pagado", onClick = { filter = "pagado" }, label = { Text("Pagados") })
                }
            }
            items(loans, key = { it.id }) { loan ->
                CredimaxCard(onClick = { onLoan(loan.id) }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(loan.client?.nombre ?: "", style = MaterialTheme.typography.titleMedium)
                            StatusChip(loanStatusLabel(loan.estado), loan.estado)
                        }
                        AmountRow("Pendiente", money(loan.pendiente ?: 0.0), emphasis = true)
                        AmountRow("Cobrado", money(loan.cobrado ?: 0.0))
                        AmountRow("Interés pactado", money(loan.interesPactado ?: 0.0))
                    }
                }
            }
        }
    }
}
