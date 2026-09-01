package com.credimax.app.ui.screens.loans

import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.credimax.app.CredimaxApp
import com.credimax.app.data.InstallmentDto
import com.credimax.app.data.LoanDto
import com.credimax.app.data.mediaUrl
import com.credimax.app.data.parseApiError
import com.credimax.app.data.textPart
import com.credimax.app.data.uriToPart
import com.credimax.app.ui.components.AmountRow
import com.credimax.app.ui.components.CredimaxCard
import com.credimax.app.ui.components.EmptyText
import com.credimax.app.ui.components.Field
import com.credimax.app.ui.components.LoadingBox
import com.credimax.app.ui.components.OptionalPhotoPicker
import com.credimax.app.ui.components.PrimaryButton
import com.credimax.app.ui.components.StatusChip
import com.credimax.app.ui.installmentStatusLabel
import com.credimax.app.ui.lateFee
import com.credimax.app.ui.loanStatusLabel
import com.credimax.app.ui.money
import com.credimax.app.ui.parseLocalDate
import com.credimax.app.ui.shortDate
import com.credimax.app.ui.theme.SlateMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(onOpen: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as CredimaxApp
    var filter by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<LoanDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(filter) {
        loading = true
        items = runCatching { app.container.api.loans(filter) }.getOrDefault(emptyList())
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Préstamos") })
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Todos") })
            FilterChip(selected = filter == "al_dia", onClick = { filter = "al_dia" }, label = { Text("Al día") })
            FilterChip(selected = filter == "atrasado", onClick = { filter = "atrasado" }, label = { Text("Atrasados") })
            FilterChip(selected = filter == "pagado", onClick = { filter = "pagado" }, label = { Text("Pagados") })
        }
        when {
            loading -> LoadingBox()
            items.isEmpty() -> EmptyText("No hay préstamos en este filtro.")
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items, key = { it.id }) { loan ->
                    CredimaxCard(onClick = { onOpen(loan.id) }) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(loan.client?.nombre ?: "Cliente", style = MaterialTheme.typography.titleMedium)
                                StatusChip(loanStatusLabel(loan.estado), loan.estado)
                            }
                            Text("${money(loan.capital)} al ${loan.interesPct}% · ${loan.semanas} semanas", color = SlateMuted)
                            AmountRow("Total", money(loan.totalPagar), emphasis = true)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(loanId: String, onPay: (String) -> Unit, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as CredimaxApp
    var loan by remember { mutableStateOf<LoanDto?>(null) }

    LaunchedEffect(loanId) {
        loan = runCatching { app.container.api.loan(loanId) }.getOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Préstamo") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) } },
            )
        },
    ) { padding ->
        val l = loan
        if (l == null) LoadingBox()
        else LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CredimaxCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(l.client?.nombre ?: "", style = MaterialTheme.typography.titleLarge)
                            StatusChip(loanStatusLabel(l.estado), l.estado)
                        }
                        Text(l.client?.let { "${it.tipoDocumento} ${it.nroDocumento}" } ?: "", color = SlateMuted)
                        Spacer(Modifier.height(8.dp))
                        AmountRow("Capital", money(l.capital))
                        AmountRow("Interés", "${l.interesPct}%")
                        AmountRow("Total a devolver", money(l.totalPagar), emphasis = true)
                        mediaUrl(l.imagenUrl)?.let { url ->
                            Spacer(Modifier.height(8.dp))
                            Text("Foto del préstamo", color = SlateMuted)
                            AsyncImage(
                                model = url,
                                contentDescription = "Foto del préstamo",
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
            }
            items(l.installments, key = { it.id }) { inst ->
                InstallmentCard(inst, onPay = { onPay(inst.id) })
            }
        }
    }
}

@Composable
private fun InstallmentCard(inst: InstallmentDto, onPay: () -> Unit) {
    val unpaid = inst.estado == "pendiente" || inst.estado == "atrasada"
    CredimaxCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Letra ${inst.nro}", style = MaterialTheme.typography.titleMedium)
                StatusChip(installmentStatusLabel(inst.estado), inst.estado)
            }
            Text("Vence ${shortDate(inst.fechaVencimiento)}", color = SlateMuted)
            AmountRow("Cuota", money(inst.monto))
            if (inst.recargoAcumulado > 0) AmountRow("Recargo", money(inst.recargoAcumulado))
            AmountRow(
                if (unpaid) "Por pagar" else "Pagado",
                money(if (unpaid) inst.monto + inst.recargoAcumulado else inst.montoPagado),
                emphasis = true,
            )
            if (!unpaid) {
                val paidOn = inst.pagadaEn ?: inst.payments.firstOrNull()?.fecha
                if (!paidOn.isNullOrBlank()) {
                    Text("Pagado el ${shortDate(paidOn)}", color = SlateMuted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            inst.payments.firstOrNull()?.let { p ->
                val receipt = mediaUrl(p.comprobanteUrl)
                if (receipt != null) {
                    Text("Comprobante adjunto", color = SlateMuted, style = MaterialTheme.typography.bodyMedium)
                    AsyncImage(
                        model = receipt,
                        contentDescription = "Comprobante",
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            if (unpaid) {
                Spacer(Modifier.height(4.dp))
                PrimaryButton("Registrar pago", onClick = onPay)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayScreen(loanId: String, installmentId: String, onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as CredimaxApp
    val scope = rememberCoroutineScope()
    var loan by remember { mutableStateOf<LoanDto?>(null) }
    var uri by remember { mutableStateOf<Uri?>(null) }
    var notas by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf(false) }
    var fechaPago by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(loanId) {
        loan = runCatching { app.container.api.loan(loanId) }.getOrNull()
    }

    val inst = loan?.installments?.find { it.id == installmentId }
    val due = inst?.let { parseLocalDate(it.fechaVencimiento) }
    val recargo = if (inst != null && due != null) {
        lateFee(inst.monto, loan?.tasaSemanal ?: 0.0, due, fechaPago)
    } else {
        0.0
    }
    val total = if (inst != null) inst.monto + recargo else 0.0

    if (confirm && inst != null) {
        val recargoNota = if (recargo > 0) {
            " Incluye recargo de ${money(recargo)} porque la fecha es posterior al vencimiento."
        } else {
            " Sin recargo: la fecha de pago no supera el vencimiento."
        }
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Confirmar pago") },
            text = {
                Text("Se registrará ${money(total)} en la letra ${inst.nro} con fecha ${shortDate(fechaPago.toString())}.$recargoNota")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    val photo = uri
                    val fecha = fechaPago.toString()
                    scope.launch {
                        loading = true
                        error = null
                        try {
                            val part = photo?.let {
                                withContext(Dispatchers.IO) { uriToPart(context, it, "comprobante") }
                            }
                            app.container.api.payInstallment(
                                loanId,
                                installmentId,
                                part,
                                textPart(notas),
                                textPart(fecha),
                            )
                            onDone()
                        } catch (e: HttpException) {
                            error = parseApiError(app.container.gson, e.response()?.errorBody()?.string())
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            loading = false
                        }
                    }
                }) { Text("Registrar") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar pago") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) } },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (inst == null) LoadingBox()
            else {
                val dateState = rememberDatePickerState(
                    initialSelectedDateMillis = fechaPago.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                    selectableDates = object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                            val d = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                            return !d.isAfter(LocalDate.now())
                        }
                    },
                )
                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                dateState.selectedDateMillis?.let { ms ->
                                    fechaPago = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                                }
                                showDatePicker = false
                            }) { Text("Listo") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                        },
                    ) {
                        DatePicker(state = dateState)
                    }
                }
                CredimaxCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Letra ${inst.nro} · ${loan?.client?.nombre}", style = MaterialTheme.typography.titleMedium)
                        Text("Vence ${shortDate(inst.fechaVencimiento)}", color = SlateMuted)
                        AmountRow("Cuota", money(inst.monto))
                        AmountRow("Recargo por atraso", money(recargo))
                        AmountRow("Total a cobrar", money(total), emphasis = true)
                        if (recargo == 0.0 && due != null && !fechaPago.isAfter(due)) {
                            Text(
                                "Sin recargo: pagó en o antes del vencimiento.",
                                color = SlateMuted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = shortDate(fechaPago.toString()),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha de pago") },
                    supportingText = { Text("El recargo se calcula con esta fecha, no con el día de hoy.") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { showDatePicker = true }) { Text("Cambiar") }
                    },
                )
                OptionalPhotoPicker(
                    uri = uri,
                    onUri = { uri = it },
                    title = "Comprobante (opcional)",
                    hint = "Puedes adjuntar una foto o tomarla con la cámara. No es obligatorio.",
                )
                Field(notas, { notas = it }, "Notas (opcional)")
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                PrimaryButton(
                    "Registrar pago de ${money(total)}",
                    enabled = !loading,
                    loading = loading,
                    onClick = { confirm = true },
                )
            }
        }
    }
}
