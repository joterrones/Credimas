package com.credimax.app.ui

import com.credimax.app.ui.theme.AmberAlert
import com.credimax.app.ui.theme.Danger
import com.credimax.app.ui.theme.SlateMuted
import com.credimax.app.ui.theme.Success
import com.credimax.app.ui.theme.TealDeep
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.round

private val lima = ZoneId.of("America/Lima")
private val esPe = Locale("es", "PE")
private val pen = NumberFormat.getCurrencyInstance(esPe)
private val dayFmt = DateTimeFormatter.ofPattern("dd MMM yyyy", esPe)
private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE

fun todayLima(): LocalDate = LocalDate.now(lima)

fun weekdayEs(date: LocalDate): String {
    val raw = date.dayOfWeek.getDisplayName(TextStyle.FULL, esPe)
    return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(esPe) else it.toString() }
}

fun money(value: Double): String = pen.format(value)

fun parsePlazos(raw: String): List<Int> {
    return raw.split(',', ';', ' ', '\n')
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 1..52 }
        .distinct()
        .sorted()
}

fun parseLocalDate(iso: String): java.time.LocalDate? {
    return try {
        if (iso.length >= 10 && iso[4] == '-') LocalDate.parse(iso.take(10), isoDate)
        else Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate()
    } catch (_: Exception) {
        null
    }
}

fun shortDate(iso: String): String {
    val date = parseLocalDate(iso) ?: return iso.take(10)
    return date.format(dayFmt)
}

/** Vencimiento de letra: "Lunes, 08 sept. 2026". */
fun dueDateLabel(iso: String): String {
    val date = parseLocalDate(iso) ?: return iso.take(10)
    return "${weekdayEs(date)}, ${date.format(dayFmt)}"
}

fun weeksLateNow(dueIso: String, today: LocalDate = todayLima()): Int {
    val due = parseLocalDate(dueIso) ?: return 0
    return weeksLate(due, today)
}

fun weeksLateText(weeks: Int): String? = when {
    weeks <= 0 -> null
    weeks == 1 -> "1 semana de atraso"
    else -> "$weeks semanas de atraso"
}

fun loanStatusLabel(estado: String) = when (estado) {
    "al_dia" -> "Al día"
    "atrasado" -> "Atrasado"
    "pagado" -> "Pagado"
    "activo" -> "Activo"
    else -> estado
}

fun quoteStatusLabel(estado: String) = when (estado) {
    "borrador" -> "Borrador"
    "aprobado" -> "Aprobado"
    "rechazado" -> "Rechazado"
    "convertido" -> "Convertido"
    else -> estado
}

fun installmentStatusLabel(estado: String) = when (estado) {
    "pendiente" -> "Pendiente"
    "atrasada" -> "Atrasada"
    "pagada" -> "Pagada"
    "pagada_con_atraso" -> "Pagada con atraso"
    else -> estado
}

fun weeksLate(due: LocalDate, paidOn: LocalDate): Int {
    if (!paidOn.isAfter(due)) return 0
    val weeks = (ChronoUnit.DAYS.between(due, paidOn) / 7).toInt()
    return if (weeks == 0) 1 else weeks
}

const val RECARGO_POR_SEMANA = 10.0

fun lateFee(due: LocalDate, paidOn: LocalDate): Double {
    val w = weeksLate(due, paidOn)
    if (w <= 0) return 0.0
    return round((RECARGO_POR_SEMANA * w + 1e-10) * 100.0) / 100.0
}

fun statusColor(estado: String) = when (estado) {
    "al_dia", "pagada", "convertido", "activo" -> Success
    "atrasado", "atrasada", "pagada_con_atraso" -> AmberAlert
    "rechazado" -> Danger
    "pagado" -> TealDeep
    else -> SlateMuted
}
