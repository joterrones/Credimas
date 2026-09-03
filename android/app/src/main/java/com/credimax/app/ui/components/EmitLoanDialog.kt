package com.credimax.app.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.credimax.app.ui.shortDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmitLoanDialog(
    initialFecha: LocalDate,
    photo: PhotoActions,
    onDismiss: () -> Unit,
    onConfirm: (fechaInicio: LocalDate, photo: Uri?) -> Unit,
) {
    var fechaInicio by remember(initialFecha) { mutableStateOf(initialFecha) }
    var showDatePicker by remember { mutableStateOf(false) }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true),
        title = { Text("Emitir préstamo") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Se crearán las letras semanales. La primera vence 7 días después de la fecha de inicio.")
                OutlinedTextField(
                    value = shortDate(fechaInicio.toString()),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha de inicio") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { showDatePicker = true }) { Text("Cambiar") }
                    },
                )
                OptionalPhotoPicker(
                    actions = photo,
                    title = "Foto del préstamo (opcional)",
                    hint = "Galería o cámara. Puedes emitir sin foto.",
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(fechaInicio, photo.uri) }) { Text("Emitir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
