package com.credimax.app.ui.screens.clients

import android.net.Uri
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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.credimax.app.CredimaxApp
import com.credimax.app.data.ClientDto
import com.credimax.app.data.LoanDto
import com.credimax.app.data.QuoteDto
import com.credimax.app.data.textPart
import com.credimax.app.data.uriToPart
import com.credimax.app.ui.components.AmountRow
import com.credimax.app.ui.components.CredimaxCard
import com.credimax.app.ui.components.EmitLoanDialog
import com.credimax.app.ui.components.EmptyText
import com.credimax.app.ui.components.rememberPhotoActions
import com.credimax.app.ui.components.LoadingBox
import com.credimax.app.ui.components.PrimaryButton
import com.credimax.app.ui.components.StatusChip
import com.credimax.app.ui.loanStatusLabel
import com.credimax.app.ui.money
import com.credimax.app.ui.parseLocalDate
import com.credimax.app.ui.quoteStatusLabel
import com.credimax.app.ui.theme.SlateMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientActionsScreen(
    clientId: String,
    onEdit: () -> Unit,
    onQuotes: () -> Unit,
    onLoans: () -> Unit,
    onNewQuote: () -> Unit,
    onNewLoan: () -> Unit,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as CredimaxApp
    var client by remember { mutableStateOf<ClientDto?>(null) }

    LaunchedEffect(clientId) {
        client = runCatching { app.container.api.client(clientId) }.getOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(client?.nombre ?: "Cliente") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Atrás") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            client?.let { c ->
                Text("#${c.codigo} · ${c.tipoDocumento} ${c.nroDocumento}", color = SlateMuted)
                Text(c.telefono, color = SlateMuted, style = MaterialTheme.typography.bodyMedium)
            }
            ActionRow("Editar ficha", "Nombre, documento, teléfono y dirección", Icons.Outlined.Edit, onEdit)
            ActionRow("Ver presupuestos", "Propuestas de este cliente", Icons.Outlined.Description, onQuotes)
            ActionRow("Ver préstamos", "Créditos emitidos a este cliente", Icons.Outlined.AccountBalanceWallet, onLoans)
            ActionRow("Crear presupuesto", "Queda como propuesta hasta emitirlo", Icons.Outlined.NoteAdd, onNewQuote)
            ActionRow("Crear préstamo", "Genera letras desde hoy", Icons.Outlined.Add, onNewLoan)
        }
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    CredimaxCard(onClick = onClick) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            leadingContent = { Icon(icon, contentDescription = null) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientQuotesScreen(
    clientId: String,
    onEdit: (String) -> Unit,
    onLoan: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as CredimaxApp
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<QuoteDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var confirmQuoteId by rememberSaveable { mutableStateOf<String?>(null) }
    var emitPhoto by remember { mutableStateOf<Uri?>(null) }
    val photoActions = rememberPhotoActions(emitPhoto) { emitPhoto = it }

    suspend fun load() {
        loading = true
        items = runCatching { app.container.api.quotes(clientId = clientId) }.getOrDefault(emptyList())
        loading = false
    }

    LaunchedEffect(clientId) { load() }

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
        topBar = {
            TopAppBar(
                title = { Text("Presupuestos") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) }
                },
            )
        },
    ) { padding ->
        when {
            loading -> LoadingBox()
            items.isEmpty() -> EmptyText("Este cliente no tiene presupuestos.")
            else -> LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items, key = { it.id }) { q ->
                    CredimaxCard(onClick = {
                        if (q.estado != "convertido" && q.estado != "rechazado") onEdit(q.id)
                    }) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Capital ${money(q.capital)}", style = MaterialTheme.typography.titleMedium)
                                StatusChip(quoteStatusLabel(q.estado), q.estado)
                            }
                            Text("${q.interesPct}% · ${q.semanas} semanas", color = SlateMuted)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientLoansScreen(
    clientId: String,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as CredimaxApp
    var items by remember { mutableStateOf<List<LoanDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(clientId) {
        items = runCatching { app.container.api.loans(clientId = clientId) }.getOrDefault(emptyList())
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Préstamos") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) }
                },
            )
        },
    ) { padding ->
        when {
            loading -> LoadingBox()
            items.isEmpty() -> EmptyText("Este cliente no tiene préstamos.")
            else -> LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items, key = { it.id }) { loan ->
                    CredimaxCard(onClick = { onOpen(loan.id) }) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(money(loan.capital), style = MaterialTheme.typography.titleMedium)
                                StatusChip(loanStatusLabel(loan.estado), loan.estado)
                            }
                            Text("${loan.interesPct}% · ${loan.semanas} semanas", color = SlateMuted)
                            AmountRow("Total", money(loan.totalPagar), emphasis = true)
                        }
                    }
                }
            }
        }
    }
}
