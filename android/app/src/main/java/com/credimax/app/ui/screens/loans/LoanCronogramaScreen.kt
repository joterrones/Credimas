package com.credimax.app.ui.screens.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.credimax.app.CredimaxApp
import com.credimax.app.data.InstallmentDto
import com.credimax.app.data.LoanDto
import com.credimax.app.ui.buildLoanCronogramaGreeting
import com.credimax.app.ui.components.AmountRow
import com.credimax.app.ui.components.CredimaxCard
import com.credimax.app.ui.components.EmptyText
import com.credimax.app.ui.components.LoadingBox
import com.credimax.app.ui.components.PrimaryButton
import com.credimax.app.ui.components.StatusChip
import com.credimax.app.ui.cronogramaStatusLabel
import com.credimax.app.ui.dueDateLabel
import com.credimax.app.ui.formatPct
import com.credimax.app.ui.money
import com.credimax.app.ui.plazoLabel
import com.credimax.app.ui.saveBitmapToCache
import com.credimax.app.ui.shareImageAndTextViaWhatsApp
import com.credimax.app.ui.shortDate
import com.credimax.app.ui.theme.AmberAlert
import com.credimax.app.ui.theme.SlateMuted
import com.credimax.app.ui.weeksLateNow
import com.credimax.app.ui.weeksLateText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanCronogramaScreen(loanId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as CredimaxApp
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val graphicsLayer = rememberGraphicsLayer()
    var loan by remember { mutableStateOf<LoanDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var sharing by remember { mutableStateOf(false) }

    fun shareCronograma(l: LoanDto) {
        if (sharing) return
        scope.launch {
            sharing = true
            try {
                val bitmap = withContext(Dispatchers.Main) {
                    graphicsLayer.toImageBitmap().asAndroidBitmap()
                }
                if (bitmap.width <= 0 || bitmap.height <= 0) {
                    snackbar.showSnackbar("No se pudo capturar el cronograma")
                    return@launch
                }
                val uri = withContext(Dispatchers.IO) { saveBitmapToCache(context, bitmap) }
                val ok = shareImageAndTextViaWhatsApp(context, uri, buildLoanCronogramaGreeting(l))
                if (!ok) {
                    snackbar.showSnackbar("WhatsApp no está instalado en este dispositivo")
                }
            } catch (e: Exception) {
                snackbar.showSnackbar(e.message ?: "No se pudo compartir el cronograma")
            } finally {
                sharing = false
            }
        }
    }

    LaunchedEffect(loanId) {
        loading = true
        error = null
        runCatching { app.container.api.loan(loanId) }
            .onSuccess { loan = it }
            .onFailure { error = it.message ?: "No se pudo cargar el préstamo" }
        loading = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Cronograma") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Atrás") }
                },
                actions = {
                    val l = loan
                    if (l != null) {
                        IconButton(
                            onClick = { shareCronograma(l) },
                            enabled = !sharing,
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = "Compartir por WhatsApp")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading -> LoadingBox()
            error != null -> EmptyText(error!!)
            loan == null -> EmptyText("Préstamo no encontrado.")
            else -> {
                val l = loan!!
                val letters = l.installments.orEmpty().sortedBy { it.nro }
                LazyColumn(
                    Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        CredimaxCard(
                            modifier = Modifier.drawWithContent {
                                graphicsLayer.record {
                                    this@drawWithContent.drawContent()
                                }
                                drawLayer(graphicsLayer)
                            },
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Fechas de pago", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Desde ${shortDate(l.fechaInicio)}. Capital ${money(l.capital)} · ${formatPct(l.interesPct)}% · ${plazoLabel(l.frecuencia, l.semanas)}.",
                                    color = SlateMuted,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    l.client?.let { "${it.nombre} · #${it.codigo}" } ?: "Cliente",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                AmountRow("Total a devolver", money(l.totalPagar), emphasis = true)
                                if (letters.isEmpty()) {
                                    Text("Este préstamo no tiene letras.", color = SlateMuted)
                                } else {
                                    letters.forEach { inst ->
                                        CronogramaLetterRow(inst)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        PrimaryButton(
                            if (sharing) "Preparando…" else "Compartir por WhatsApp",
                            enabled = !sharing,
                            onClick = { shareCronograma(l) },
                        )
                    }
                    item {
                        Text(
                            "Se enviará la imagen del cronograma (Fechas de pago) y el saludo. Elige el chat en WhatsApp.",
                            color = SlateMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CronogramaLetterRow(inst: InstallmentDto) {
    val showRecargo = inst.recargoAcumulado > 0 &&
        (inst.estado == "atrasada" || inst.estado == "pagada_con_atraso")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    "Letra ${inst.nro} · ${dueDateLabel(inst.fechaVencimiento)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusChip(cronogramaStatusLabel(inst.estado), inst.estado)
                    if (inst.estado == "atrasada") {
                        weeksLateText(weeksLateNow(inst.fechaVencimiento))?.let { weeks ->
                            Text(weeks, color = AmberAlert, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                if (showRecargo) {
                    Text("Recargo ${money(inst.recargoAcumulado)}", color = AmberAlert, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(money(inst.monto), style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(4.dp))
    }
}
