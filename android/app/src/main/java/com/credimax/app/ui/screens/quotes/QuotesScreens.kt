package com.credimax.app.ui.screens.quotes

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.credimax.app.CredimaxApp
import com.credimax.app.data.ClientDto
import com.credimax.app.data.PreviewDto
import com.credimax.app.data.QuoteBody
import com.credimax.app.data.QuoteDto
import com.credimax.app.data.SettingsDto
import com.credimax.app.data.parseApiError
import com.credimax.app.data.textPart
import com.credimax.app.data.uriToPart
import com.credimax.app.ui.components.AmountRow
import com.credimax.app.ui.components.CredimaxCard
import com.credimax.app.ui.components.EmitLoanDialog
import com.credimax.app.ui.components.EmptyText
import com.credimax.app.ui.components.Field
import com.credimax.app.ui.components.LoadingBox
import com.credimax.app.ui.components.OptionalPhotoPicker
import com.credimax.app.ui.components.PrimaryButton
import com.credimax.app.ui.components.rememberPhotoActions
import com.credimax.app.ui.components.StatusChip
import com.credimax.app.ui.money
import com.credimax.app.ui.parseLocalDate
import com.credimax.app.ui.quoteStatusLabel
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
fun QuotesScreen(onNew: () -> Unit, onEdit: (String) -> Unit, onLoan: (String) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as CredimaxApp
    var items by remember { mutableStateOf<List<QuoteDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    var confirmQuoteId by rememberSaveable { mutableStateOf<String?>(null) }
    var emitPhoto by remember { mutableStateOf<Uri?>(null) }
    val photoActions = rememberPhotoActions(emitPhoto) { emitPhoto = it }

    suspend fun load() {
        loading = true
        try {
            items = app.container.api.quotes()
        } catch (_: Exception) {
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    val confirmQuote = items.find { it.id == confirmQuoteId }
    confirmQuote?.let { quote ->
        EmitLoanDialog(
            initialFecha = parseLocalDate(quote.fechaInicio) ?: LocalDate.now(),
            photo = photoActions,
            onDismiss = {
                confirmQuoteId = null
                emitPhoto = null
            },
            onConfirm = { fecha, photo ->
                val id = quote.id
                confirmQuoteId = null
                emitPhoto = null
                scope.launch {
                    try {
                        val imagen = photo?.let {
                            withContext(Dispatchers.IO) { uriToPart(context, it, "imagen") }
                        }
                        val loan = app.container.api.convertQuote(id, textPart(fecha.toString()), imagen)
                        onLoan(loan.id)
                    } catch (_: Exception) {
                        load()
                    }
                }
            },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Presupuestos") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNew) { Icon(Icons.Outlined.Add, null) }
        },
    ) { padding ->
        if (loading) LoadingBox()
        else if (items.isEmpty()) EmptyText("Crea un presupuesto y emítelo como préstamo cuando el cliente acepte.")
        else LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { it.id }) { q ->
                CredimaxCard(onClick = { if (q.estado != "convertido" && q.estado != "rechazado") onEdit(q.id) }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(q.client?.nombre ?: "Cliente", style = MaterialTheme.typography.titleMedium)
                            StatusChip(quoteStatusLabel(q.estado), q.estado)
                        }
                        Text("Capital ${money(q.capital)} · ${q.interesPct}% · ${q.semanas} semanas", color = SlateMuted)
                        AmountRow("Total a devolver", money(q.totalPagar), emphasis = true)
                        if (q.estado == "borrador" || q.estado == "aprobado") {
                            PrimaryButton("Emitir préstamo", onClick = { confirmQuoteId = q.id })
                        }
                        q.loan?.let { TextButton(onClick = { onLoan(it.id) }) { Text("Ver préstamo") } }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuoteFormScreen(
    quoteId: String,
    presetClientId: String,
    asLoan: Boolean,
    onDone: () -> Unit,
    onConverted: (String) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as CredimaxApp
    val scope = rememberCoroutineScope()
    var clients by remember { mutableStateOf<List<ClientDto>>(emptyList()) }
    var settings by remember { mutableStateOf(SettingsDto()) }
    var clientId by remember { mutableStateOf(presetClientId) }
    var lockedClient by remember { mutableStateOf<ClientDto?>(null) }
    var capital by remember { mutableStateOf("") }
    var interes by remember { mutableStateOf("10") }
    var semanasTxt by remember { mutableStateOf("4") }
    var notas by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<PreviewDto?>(null) }
    var fechaInicio by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val clientLocked = presetClientId.isNotBlank()

    LaunchedEffect(quoteId, presetClientId) {
        clients = runCatching { app.container.api.clients() }.getOrDefault(emptyList())
        settings = runCatching { app.container.api.settings() }.getOrDefault(SettingsDto())
        interes = settings.interesPctDefault.toString().trimEnd('0').trimEnd('.')
        val defSem = if (settings.semanasDefault in 1..52) settings.semanasDefault else settings.plazosPermitidos.firstOrNull() ?: 4
        semanasTxt = defSem.toString()
        if (presetClientId.isNotBlank()) {
            clientId = presetClientId
            lockedClient = runCatching { app.container.api.client(presetClientId) }.getOrNull()
        }
        if (quoteId.isNotBlank()) {
            runCatching { app.container.api.quote(quoteId) }.getOrNull()?.let { q ->
                clientId = q.clientId
                capital = q.capital.toString()
                interes = q.interesPct.toString()
                semanasTxt = q.semanas.toString()
                notas = q.notas.orEmpty()
                parseLocalDate(q.fechaInicio)?.let { fechaInicio = it }
                if (lockedClient == null) lockedClient = q.client
            }
        }
    }

    val cap = capital.replace(",", ".").toDoubleOrNull() ?: 0.0
    val pct = interes.replace(",", ".").toDoubleOrNull() ?: 0.0
    val semanas = semanasTxt.toIntOrNull()?.takeIf { it in 1..52 } ?: 0

    LaunchedEffect(cap, pct, semanas, fechaInicio) {
        preview = if (cap > 0 && semanas > 0) {
            runCatching {
                app.container.api.previewInstallments(cap, pct, semanas, fechaInicio.toString())
            }.getOrNull()
        } else {
            null
        }
    }

    val total = preview?.totalPagar ?: 0.0

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Emitir préstamo") },
            text = { Text("Se crearán $semanas letras desde ${shortDate(fechaInicio.toString())} (letra 1 a los 7 días).") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    scope.launch {
                        loading = true
                        error = null
                        try {
                            val imagen = photoUri?.let {
                                withContext(Dispatchers.IO) { uriToPart(context, it, "imagen") }
                            }
                            val loan = if (quoteId.isNotBlank()) {
                                app.container.api.updateQuote(
                                    quoteId,
                                    QuoteBody(clientId, cap, pct, semanas, fechaInicio.toString(), notas.ifBlank { null }),
                                )
                                app.container.api.convertQuote(quoteId, textPart(fechaInicio.toString()), imagen)
                            } else {
                                app.container.api.createLoan(
                                    textPart(clientId),
                                    textPart(cap.toString()),
                                    textPart(pct.toString()),
                                    textPart(semanas.toString()),
                                    textPart(fechaInicio.toString()),
                                    imagen,
                                )
                            }
                            onConverted(loan.id)
                        } catch (e: HttpException) {
                            error = parseApiError(app.container.gson, e.response()?.errorBody()?.string())
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            loading = false
                        }
                    }
                }) { Text("Emitir") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } },
        )
    }

    val title = when {
        quoteId.isNotBlank() -> "Presupuesto"
        asLoan -> "Nuevo préstamo"
        else -> "Nuevo presupuesto"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Outlined.ArrowBack, null) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (clientLocked) {
                val label = lockedClient?.let { "#${it.codigo} ${it.nombre}" } ?: "Cliente"
                OutlinedTextField(
                    value = label,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Cliente") },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                val selected = clients.find { it.id == clientId }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selected?.let { "#${it.codigo} ${it.nombre}" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cliente") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        clients.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("#${c.codigo} ${c.nombre}") },
                                onClick = {
                                    clientId = c.id
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
            val dateState = rememberDatePickerState(
                initialSelectedDateMillis = fechaInicio.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            )
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            dateState.selectedDateMillis?.let { ms ->
                                fechaInicio = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
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
            OutlinedTextField(
                value = shortDate(fechaInicio.toString()),
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha de inicio") },
                supportingText = { Text("La primera letra vence 7 días después") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) { Text("Cambiar") }
                },
            )
            Field(capital, { capital = it }, "Capital (S/)")
            Field(interes, { interes = it }, "Interés %")
            Field(semanasTxt, { semanasTxt = it.filter { ch -> ch.isDigit() }.take(2) }, "Plazo (semanas)")
            Text("Atajos de plazo", color = SlateMuted, style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (settings.plazosPermitidos.ifEmpty { listOf(4, 6, 8, 10, 12, 16) }).forEach { n ->
                    FilterChip(
                        selected = semanas == n,
                        onClick = { semanasTxt = n.toString() },
                        label = { Text("$n sem.") },
                    )
                }
            }
            Field(notas, { notas = it }, "Notas (opcional)", singleLine = false)
            OptionalPhotoPicker(
                uri = photoUri,
                onUri = { photoUri = it },
                title = "Foto del préstamo (opcional)",
                hint = "Puedes adjuntar una foto o tomarla con la cámara al emitir.",
            )
            CredimaxCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Fechas de pago", style = MaterialTheme.typography.titleMedium)
                    Text("Desde ${shortDate(fechaInicio.toString())}. La letra 1 vence 7 días después.", color = SlateMuted, style = MaterialTheme.typography.bodyMedium)
                    AmountRow("Total a devolver", money(total), emphasis = true)
                    preview?.letras?.forEach { letra ->
                        AmountRow("Letra ${letra.nro} · ${shortDate(letra.fechaVencimiento)}", money(letra.monto))
                    }
                    if (cap > 0 && preview == null) {
                        Text("No se pudo calcular el calendario.", color = SlateMuted)
                    }
                }
            }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            if (!asLoan || quoteId.isNotBlank()) {
                PrimaryButton(
                    "Guardar presupuesto",
                    loading = loading && !confirm,
                    enabled = clientId.isNotBlank() && cap > 0,
                    onClick = {
                        scope.launch {
                            loading = true
                            error = null
                            val body = QuoteBody(clientId, cap, pct, semanas, fechaInicio.toString(), notas.ifBlank { null })
                            try {
                                if (quoteId.isBlank()) app.container.api.createQuote(body)
                                else app.container.api.updateQuote(quoteId, body)
                                onDone()
                            } catch (e: HttpException) {
                                error = parseApiError(app.container.gson, e.response()?.errorBody()?.string())
                            } catch (e: Exception) {
                                error = e.message
                            } finally {
                                loading = false
                            }
                        }
                    },
                )
            }
            OutlinedButton(
                onClick = { confirm = true },
                enabled = clientId.isNotBlank() && cap > 0 && !loading,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Emitir préstamo") }
            if (asLoan && quoteId.isBlank()) {
                TextButton(
                    onClick = {
                        scope.launch {
                            loading = true
                            error = null
                            try {
                                app.container.api.createQuote(
                                    QuoteBody(clientId, cap, pct, semanas, fechaInicio.toString(), notas.ifBlank { null }),
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
                    },
                    enabled = clientId.isNotBlank() && cap > 0 && !loading,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Guardar solo como presupuesto") }
            }
        }
    }
}
