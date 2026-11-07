package com.credimax.app.ui

import com.credimax.app.data.SettingsDto
import com.credimax.app.ui.theme.AmberAlert
import com.credimax.app.ui.theme.Danger
import com.credimax.app.ui.theme.SlateMuted
import com.credimax.app.ui.theme.Success
import com.credimax.app.ui.theme.TealDeep
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.round

private val lima = ZoneId.of("America/Lima")
private val esPe = Locale("es", "PE")
private val pen = NumberFormat.getCurrencyInstance(esPe)
private val dayFmt = DateTimeFormatter.ofPattern("dd MMM yyyy", esPe)
private val monthTitleFmt = DateTimeFormatter.ofPattern("MMMM yyyy", esPe)
private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE

/** Cabeceras cortas Lun–Dom (grilla empieza en lunes). */
val WEEKDAY_HEADERS_ES = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

fun todayLima(): LocalDate = LocalDate.now(lima)

fun weekdayEs(date: LocalDate): String {
    val raw = date.dayOfWeek.getDisplayName(TextStyle.FULL, esPe)
    return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(esPe) else it.toString() }
}

fun money(value: Double): String = pen.format(value)

fun parsePlazos(raw: String, max: Int = 52): List<Int> {
    return raw.split(',', ';', ' ', '\n')
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 1..max }
        .distinct()
        .sorted()
}

const val FREQ_SEMANAL = "semanal"
const val FREQ_QUINCENAL = "quincenal"
const val FREQ_MENSUAL = "mensual"

/** Gson ignora defaults de Kotlin; normaliza null/vacío a semanal. */
fun normalizeFrequency(frecuencia: String?): String = when (frecuencia?.trim()?.lowercase()) {
    FREQ_QUINCENAL -> FREQ_QUINCENAL
    FREQ_MENSUAL -> FREQ_MENSUAL
    else -> FREQ_SEMANAL
}

fun maxPlazos(frecuencia: String?): Int = when (normalizeFrequency(frecuencia)) {
    FREQ_QUINCENAL -> 26
    FREQ_MENSUAL -> 36
    else -> 52
}

fun frequencyLabel(frecuencia: String?): String = when (normalizeFrequency(frecuencia)) {
    FREQ_QUINCENAL -> "Quincenal"
    FREQ_MENSUAL -> "Mensual"
    else -> "Semanal"
}

fun plazoUnitShort(frecuencia: String?): String = when (normalizeFrequency(frecuencia)) {
    FREQ_QUINCENAL -> "quinc."
    FREQ_MENSUAL -> "mes"
    else -> "sem."
}

fun plazoLabel(frecuencia: String?, n: Int): String = when (normalizeFrequency(frecuencia)) {
    FREQ_QUINCENAL -> if (n == 1) "1 quincena" else "$n quincenas"
    FREQ_MENSUAL -> if (n == 1) "1 mes" else "$n meses"
    else -> if (n == 1) "1 semana" else "$n semanas"
}

fun firstDueHint(frecuencia: String?): String = when (normalizeFrequency(frecuencia)) {
    FREQ_QUINCENAL -> "La primera letra vence 14 días después"
    FREQ_MENSUAL -> "La primera letra vence 1 mes después"
    else -> "La primera letra vence 7 días después"
}

fun SettingsDto.plazosFor(frecuencia: String?): List<Int> = when (normalizeFrequency(frecuencia)) {
    FREQ_QUINCENAL -> plazosQuincenales.orEmpty().ifEmpty { listOf(2, 3, 4, 6) }
    FREQ_MENSUAL -> plazosMensuales.orEmpty().ifEmpty { listOf(1, 2, 3, 6, 12) }
    else -> plazosPermitidos.orEmpty().ifEmpty { listOf(4, 6, 8, 10, 12, 16) }
}

fun SettingsDto.defaultPlazo(frecuencia: String?): Int {
    val freq = normalizeFrequency(frecuencia)
    val fallback = plazosFor(freq).first()
    return when (freq) {
        FREQ_QUINCENAL -> quincenasDefault.takeIf { it in 1..26 } ?: fallback
        FREQ_MENSUAL -> mesesDefault.takeIf { it in 1..36 } ?: fallback
        else -> semanasDefault.takeIf { it in 1..52 } ?: fallback
    }
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

fun monthTitleEs(yearMonth: YearMonth): String {
    val raw = yearMonth.atDay(1).format(monthTitleFmt)
    return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(esPe) else it.toString() }
}

/**
 * Días visibles de la grilla mensual (Lun–Dom), incluyendo borde del mes anterior/siguiente.
 */
fun calendarMonthDays(yearMonth: YearMonth): List<LocalDate> {
    val first = yearMonth.atDay(1)
    val start = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val last = yearMonth.atEndOfMonth()
    val end = last.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val days = mutableListOf<LocalDate>()
    var d = start
    while (!d.isAfter(end)) {
        days += d
        d = d.plusDays(1)
    }
    return days
}

fun isUnpaidInstallment(estado: String): Boolean =
    estado == "pendiente" || estado == "atrasada"

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

/** Etiquetas del cronograma compartible (spec 001 préstamo). */
fun cronogramaStatusLabel(estado: String) = when (estado) {
    "pendiente" -> "Pendiente"
    "atrasada" -> "Con retraso"
    "pagada" -> "Pagado"
    "pagada_con_atraso" -> "Pagado con retraso"
    else -> estado
}

fun cronogramaStatusDetail(estado: String, fechaVencimiento: String): String {
    val base = cronogramaStatusLabel(estado)
    if (estado != "atrasada") return base
    val weeks = weeksLateText(weeksLateNow(fechaVencimiento)) ?: return base
    return "$base ($weeks)"
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
