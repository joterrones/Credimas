package com.credimax.app.ui.screens.clients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.credimax.app.CredimaxApp
import com.credimax.app.data.ClientBody
import com.credimax.app.data.ClientDto
import com.credimax.app.data.parseApiError
import com.credimax.app.ui.components.CredimaxCard
import com.credimax.app.ui.components.EmptyText
import com.credimax.app.ui.components.Field
import com.credimax.app.ui.components.LoadingBox
import com.credimax.app.ui.components.PrimaryButton
import com.credimax.app.ui.theme.SlateMuted
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(onNew: () -> Unit, onOpen: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as CredimaxApp
    var q by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<ClientDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    suspend fun load() {
        loading = true
        try {
            items = app.container.api.clients(q.ifBlank { null })
        } catch (_: Exception) {
        } finally {
            loading = false
        }
    }

    LaunchedEffect(q) { load() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Clientes") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNew) {
                Icon(Icons.Outlined.Add, contentDescription = "Nuevo")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Field(q, { q = it }, "Buscar por nombre, DNI o código")
            when {
                loading -> LoadingBox()
                items.isEmpty() -> EmptyText("Aún no hay clientes. Crea el primero.")
                else -> LazyColumn(
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(items, key = { it.id }) { c ->
                        CredimaxCard(onClick = { onOpen(c.id) }) {
                            Column(Modifier.padding(16.dp)) {
                                Text("#${c.codigo}  ${c.nombre}", style = MaterialTheme.typography.titleMedium)
                                Text("${c.tipoDocumento} ${c.nroDocumento} · ${c.telefono}", color = SlateMuted)
                                Text(c.direccion, color = SlateMuted, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientFormScreen(clientId: String, onDone: () -> Unit) {
    val app = LocalContext.current.applicationContext as CredimaxApp
    val scope = rememberCoroutineScope()
    var nombre by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("DNI") }
    var nro by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(clientId) {
        if (clientId.isNotBlank()) {
            try {
                val c = app.container.api.client(clientId)
                nombre = c.nombre
                direccion = c.direccion
                telefono = c.telefono
                tipo = c.tipoDocumento
                nro = c.nroDocumento
            } catch (_: Exception) {
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (clientId.isBlank()) "Nuevo cliente" else "Editar cliente") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Atrás") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Field(nombre, { nombre = it }, "Nombre completo")
            Field(direccion, { direccion = it }, "Dirección")
            Field(
                telefono,
                { telefono = it.filter { ch -> ch.isDigit() }.take(9) },
                "Teléfono",
                keyboardType = KeyboardType.Phone,
                supportingText = "9 dígitos",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tipo == "DNI", onClick = { tipo = "DNI" }, label = { Text("DNI") })
                FilterChip(selected = tipo == "CE", onClick = { tipo = "CE" }, label = { Text("Carné de extranjería") })
            }
            Field(
                nro,
                {
                    nro = if (tipo == "DNI") it.filter { ch -> ch.isDigit() }.take(8) else it.filter { ch -> ch.isLetterOrDigit() }.take(12)
                },
                if (tipo == "DNI") "DNI (8 dígitos)" else "CE",
                keyboardType = if (tipo == "DNI") KeyboardType.Number else KeyboardType.Ascii,
            )
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            PrimaryButton(
                if (clientId.isBlank()) "Guardar cliente" else "Actualizar",
                loading = loading,
                onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        if (telefono.length != 9) {
                            error = "El teléfono debe tener 9 dígitos"
                            loading = false
                            return@launch
                        }
                        if (tipo == "DNI" && nro.length != 8) {
                            error = "El DNI debe tener 8 dígitos"
                            loading = false
                            return@launch
                        }
                        val body = ClientBody(nombre.trim(), direccion.trim(), telefono.trim(), tipo, nro.trim())
                        try {
                            if (clientId.isBlank()) app.container.api.createClient(body)
                            else app.container.api.updateClient(clientId, body)
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
    }
}
