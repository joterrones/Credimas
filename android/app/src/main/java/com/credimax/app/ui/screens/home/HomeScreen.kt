package com.credimax.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.credimax.app.CredimaxApp
import com.credimax.app.data.DashboardDto
import com.credimax.app.data.InstallmentDto
import com.credimax.app.ui.components.AmountRow
import com.credimax.app.ui.components.CredimaxCard
import com.credimax.app.ui.components.EmptyText
import com.credimax.app.ui.components.KpiTile
import com.credimax.app.ui.components.LoadingBox
import com.credimax.app.ui.components.PrimaryButton
import com.credimax.app.ui.dueDateLabel
import com.credimax.app.ui.money
import com.credimax.app.ui.theme.AmberAlert
import com.credimax.app.ui.theme.SlateMuted
import com.credimax.app.ui.theme.Success
import com.credimax.app.ui.theme.TealDeep
import com.credimax.app.ui.weeksLateNow
import com.credimax.app.ui.weeksLateText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewClient: () -> Unit,
    onNewQuote: () -> Unit,
    onLoan: (String) -> Unit,
    onReports: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as CredimaxApp
    var data by remember { mutableStateOf<DashboardDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var lateOpen by rememberSaveable { mutableStateOf(false) }
    var weekOpen by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            data = app.container.api.dashboard()
        } catch (e: Exception) {
            error = e.message
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Inicio") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        when {
            data == null && error == null -> LoadingBox()
            error != null -> EmptyText(error!!)
            data != null -> {
                val d = data!!
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text("Cartera", style = MaterialTheme.typography.headlineMedium)
                        Text("Resumen de microcréditos", color = SlateMuted)
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            KpiTile("Capital", money(d.cartera.capital), TealDeep, Modifier.weight(1f))
                            KpiTile("Montos cobrados", money(d.cartera.montosCobrados), Success, Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            KpiTile("Cuentas por cobrar", money(d.cartera.cuentasPorCobrar), AmberAlert, Modifier.weight(1f))
                            KpiTile("Interés", money(d.cartera.interes), Success, Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PrimaryButton("Nuevo cliente", onClick = onNewClient, modifier = Modifier.weight(1f))
                            PrimaryButton("Presupuesto", onClick = onNewQuote, modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        CollapsibleHeader(
                            title = "Letras atrasadas",
                            count = d.cobrosRetrasados.size,
                            expanded = lateOpen,
                            onToggle = { lateOpen = !lateOpen },
                        )
                    }
                    if (lateOpen) {
                        if (d.cobrosRetrasados.isEmpty()) {
                            item { EmptyText("No hay letras atrasadas.") }
                        }
                        items(d.cobrosRetrasados, key = { "late-${it.id}" }) { inst ->
                            LateLetterCard(inst, onLoan)
                        }
                    }
                    item {
                        CollapsibleHeader(
                            title = "Vence esta semana",
                            count = d.vencimientosSemana.size,
                            expanded = weekOpen,
                            onToggle = { weekOpen = !weekOpen },
                            trailing = { TextButton(onClick = onReports) { Text("Ver reportes") } },
                        )
                    }
                    if (weekOpen) {
                        if (d.vencimientosSemana.isEmpty()) {
                            item { EmptyText("No hay letras por vencer en los próximos 7 días.") }
                        }
                        items(d.vencimientosSemana, key = { it.id }) { inst ->
                            WeekLetterCard(inst, onLoan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Minimizar" else "Expandir",
            )
            Text(
                "$title ($count)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun LateLetterCard(inst: InstallmentDto, onLoan: (String) -> Unit) {
    val weeks = weeksLateText(weeksLateNow(inst.fechaVencimiento))
    CredimaxCard(onClick = { inst.loan?.id?.let(onLoan) }) {
        Column(Modifier.padding(16.dp)) {
            Text(inst.loan?.client?.nombre ?: "Cliente", style = MaterialTheme.typography.titleMedium)
            Text("Letra ${inst.nro} · ${dueDateLabel(inst.fechaVencimiento)}", color = SlateMuted)
            if (weeks != null) {
                Text(weeks, color = AmberAlert, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))
            AmountRow("Monto", money(inst.monto + inst.recargoAcumulado), emphasis = true)
        }
    }
}

@Composable
private fun WeekLetterCard(inst: InstallmentDto, onLoan: (String) -> Unit) {
    CredimaxCard(onClick = { inst.loan?.id?.let(onLoan) }) {
        Column(Modifier.padding(16.dp)) {
            Text(inst.loan?.client?.nombre ?: "Cliente", style = MaterialTheme.typography.titleMedium)
            Text("Letra ${inst.nro} · ${dueDateLabel(inst.fechaVencimiento)}", color = SlateMuted)
            Spacer(Modifier.height(8.dp))
            AmountRow("Monto", money(inst.monto + inst.recargoAcumulado), emphasis = true)
        }
    }
}
