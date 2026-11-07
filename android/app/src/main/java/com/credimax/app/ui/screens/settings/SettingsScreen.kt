package com.credimax.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
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
import com.credimax.app.ui.FREQ_MENSUAL
import com.credimax.app.ui.FREQ_QUINCENAL
import com.credimax.app.ui.FREQ_SEMANAL
import com.credimax.app.ui.components.Field
import com.credimax.app.ui.components.PrimaryButton
import com.credimax.app.ui.frequencyLabel
import com.credimax.app.ui.normalizeFrequency
import com.credimax.app.ui.parsePlazos
import com.credimax.app.ui.plazoUnitShort
import com.credimax.app.ui.theme.SlateMuted
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen() {
    val app = LocalContext.current.applicationContext as CredimaxApp
    val scope = rememberCoroutineScope()
    var interes by remember { mutableStateOf("10") }
    var frecuenciaDefault by remember { mutableStateOf(FREQ_SEMANAL) }
    var semanasDefault by remember { mutableStateOf("4") }
    var plazosTxt by remember { mutableStateOf("4, 6, 8, 10, 12, 16") }
    var quincenasDefault by remember { mutableStateOf("2") }
    var plazosQuincTxt by remember { mutableStateOf("2, 3, 4, 6") }
    var mesesDefault by remember { mutableStateOf("1") }
    var plazosMesTxt by remember { mutableStateOf("1, 2, 3, 6, 12") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { app.container.api.settings() }.getOrNull()?.let {
            interes = it.interesPctDefault.toString().trimEnd('0').trimEnd('.')
            frecuenciaDefault = normalizeFrequency(it.frecuenciaDefault)
            semanasDefault = it.semanasDefault.coerceAtLeast(1).toString()
            if (!it.plazosPermitidos.isNullOrEmpty()) plazosTxt = it.plazosPermitidos.joinToString(", ")
            quincenasDefault = it.quincenasDefault.coerceAtLeast(1).toString()
            if (!it.plazosQuincenales.isNullOrEmpty()) plazosQuincTxt = it.plazosQuincenales.joinToString(", ")
            mesesDefault = it.mesesDefault.coerceAtLeast(1).toString()
            if (!it.plazosMensuales.isNullOrEmpty()) plazosMesTxt = it.plazosMensuales.joinToString(", ")
        }
    }

    val plazos = parsePlazos(plazosTxt, 52)
    val plazosQuinc = parsePlazos(plazosQuincTxt, 26)
    val plazosMes = parsePlazos(plazosMesTxt, 36)

    Scaffold(topBar = { TopAppBar(title = { Text("Configuración") }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Valores por defecto al crear un presupuesto o préstamo. La frecuencia no se mezcla: eliges semanal, quincenal o mensual.", color = SlateMuted)
            Field(interes, { interes = it }, "Interés por defecto (%)")

            Text("Frecuencia por defecto", color = SlateMuted, style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(FREQ_SEMANAL, FREQ_QUINCENAL, FREQ_MENSUAL).forEach { freq ->
                    FilterChip(
                        selected = frecuenciaDefault == freq,
                        onClick = { frecuenciaDefault = freq },
                        label = { Text(frequencyLabel(freq)) },
                    )
                }
            }

            Text("Semanal", style = MaterialTheme.typography.titleMedium)
            Field(semanasDefault, { semanasDefault = it }, "Plazo por defecto (semanas)")
            Field(plazosTxt, { plazosTxt = it }, "Plazos sugeridos (semanas)")
            if (plazos.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    plazos.forEach { n ->
                        FilterChip(selected = true, onClick = {}, label = { Text("$n ${plazoUnitShort(FREQ_SEMANAL)}") })
                    }
                }
            }

            Text("Quincenal", style = MaterialTheme.typography.titleMedium)
            Field(quincenasDefault, { quincenasDefault = it }, "Plazo por defecto (quincenas)")
            Field(plazosQuincTxt, { plazosQuincTxt = it }, "Plazos sugeridos (quincenas)")
            if (plazosQuinc.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    plazosQuinc.forEach { n ->
                        FilterChip(selected = true, onClick = {}, label = { Text("$n ${plazoUnitShort(FREQ_QUINCENAL)}") })
                    }
                }
            }

            Text("Mensual", style = MaterialTheme.typography.titleMedium)
            Field(mesesDefault, { mesesDefault = it }, "Plazo por defecto (meses)")
            Field(plazosMesTxt, { plazosMesTxt = it }, "Plazos sugeridos (meses)")
            Text(
                "Sepáralos con coma. Semanal 1–52, quincenal 1–26, mensual 1–36.",
                color = SlateMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (plazosMes.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    plazosMes.forEach { n ->
                        FilterChip(selected = true, onClick = {}, label = { Text("$n ${plazoUnitShort(FREQ_MENSUAL)}") })
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
                    val quin = quincenasDefault.toIntOrNull()
                    val mes = mesesDefault.toIntOrNull()
                    when {
                        pct == null || pct < 0 || pct > 100 -> error = "Indica un interés entre 0 y 100"
                        sem == null || sem !in 1..52 -> error = "Plazo semanal por defecto: 1 a 52"
                        quin == null || quin !in 1..26 -> error = "Plazo quincenal por defecto: 1 a 26"
                        mes == null || mes !in 1..36 -> error = "Plazo mensual por defecto: 1 a 36"
                        plazos.isEmpty() -> error = "Indica al menos un plazo semanal"
                        plazosQuinc.isEmpty() -> error = "Indica al menos un plazo quincenal"
                        plazosMes.isEmpty() -> error = "Indica al menos un plazo mensual"
                        else -> try {
                            val listaSem = if (sem in plazos) plazos else (plazos + sem).sorted()
                            val listaQuin = if (quin in plazosQuinc) plazosQuinc else (plazosQuinc + quin).sorted()
                            val listaMes = if (mes in plazosMes) plazosMes else (plazosMes + mes).sorted()
                            app.container.api.updateSettings(
                                SettingsBody(
                                    interesPctDefault = pct,
                                    frecuenciaDefault = frecuenciaDefault,
                                    semanasDefault = sem,
                                    plazosPermitidos = listaSem,
                                    quincenasDefault = quin,
                                    plazosQuincenales = listaQuin,
                                    mesesDefault = mes,
                                    plazosMensuales = listaMes,
                                ),
                            )
                            plazosTxt = listaSem.joinToString(", ")
                            plazosQuincTxt = listaQuin.joinToString(", ")
                            plazosMesTxt = listaMes.joinToString(", ")
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
