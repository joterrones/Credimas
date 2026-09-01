package com.credimax.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.credimax.app.CredimaxApp
import com.credimax.app.data.SettingsBody
import com.credimax.app.data.parseApiError
import com.credimax.app.ui.components.Field
import com.credimax.app.ui.components.PrimaryButton
import com.credimax.app.ui.parsePlazos
import com.credimax.app.ui.theme.SlateMuted
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen() {
    val app = LocalContext.current.applicationContext as CredimaxApp
    val scope = rememberCoroutineScope()
    var interes by remember { mutableStateOf("10") }
    var semanasDefault by remember { mutableStateOf("4") }
    var plazosTxt by remember { mutableStateOf("4, 6, 8, 10, 12, 16") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { app.container.api.settings() }.getOrNull()?.let {
            interes = it.interesPctDefault.toString().trimEnd('0').trimEnd('.')
            semanasDefault = it.semanasDefault.coerceAtLeast(1).toString()
            if (it.plazosPermitidos.isNotEmpty()) plazosTxt = it.plazosPermitidos.joinToString(", ")
        }
    }

    val plazos = parsePlazos(plazosTxt)

    Scaffold(topBar = { TopAppBar(title = { Text("Configuración") }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Valores por defecto al crear un presupuesto o préstamo.", color = SlateMuted)
            Field(interes, { interes = it }, "Interés por defecto (%)")
            Field(semanasDefault, { semanasDefault = it }, "Plazo por defecto (semanas)")
            Field(
                plazosTxt,
                { plazosTxt = it },
                "Plazos sugeridos (semanas)",
            )
            Text(
                "Sepáralos con coma, por ejemplo: 4, 6, 8, 10, 12, 16. Cada préstamo puede usar cualquiera de 1 a 52.",
                color = SlateMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (plazos.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    plazos.forEach { n ->
                        FilterChip(selected = true, onClick = {}, label = { Text("$n sem.") })
                    }
                }
            }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            if (message != null) Text(message!!, color = MaterialTheme.colorScheme.primary)
            PrimaryButton("Guardar", loading = loading, onClick = {
                scope.launch {
                    loading = true
                    message = null
                    error = null
                    val pct = interes.replace(",", ".").toDoubleOrNull()
                    val sem = semanasDefault.toIntOrNull()
                    when {
                        pct == null || pct < 0 || pct > 100 -> error = "Indica un interés entre 0 y 100"
                        sem == null || sem !in 1..52 -> error = "El plazo por defecto debe ser entre 1 y 52 semanas"
                        plazos.isEmpty() -> error = "Indica al menos un plazo (ej. 4, 8, 12, 16)"
                        else -> try {
                            val lista = if (sem in plazos) plazos else (plazos + sem).sorted()
                            app.container.api.updateSettings(SettingsBody(pct, sem, lista))
                            plazosTxt = lista.joinToString(", ")
                            message = "Configuración guardada"
                        } catch (e: HttpException) {
                            error = parseApiError(app.container.gson, e.response()?.errorBody()?.string())
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                    loading = false
                }
            })
        }
    }
}
