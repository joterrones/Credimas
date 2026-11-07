package com.credimax.app.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.credimax.app.CredimaxApp
import com.credimax.app.data.InstallmentDto
import com.credimax.app.data.LoanDto
import com.credimax.app.ui.WEEKDAY_HEADERS_ES
import com.credimax.app.ui.calendarMonthDays
import com.credimax.app.ui.components.AmountRow
import com.credimax.app.ui.components.CredimaxCard
import com.credimax.app.ui.components.EmptyText
import com.credimax.app.ui.components.LoadingBox
import com.credimax.app.ui.components.PrimaryButton
import com.credimax.app.ui.components.StatusChip
import com.credimax.app.ui.dueDateLabel
import com.credimax.app.ui.installmentStatusLabel
import com.credimax.app.ui.isUnpaidInstallment
import com.credimax.app.ui.money
import com.credimax.app.ui.monthTitleEs
import com.credimax.app.ui.parseLocalDate
import com.credimax.app.ui.theme.AmberAlert
import com.credimax.app.ui.theme.SlateMuted
import com.credimax.app.ui.theme.TealDeep
import com.credimax.app.ui.theme.TealSoft
import com.credimax.app.ui.todayLima
import com.credimax.app.ui.weeksLateNow
import com.credimax.app.ui.weeksLateText
import java.time.LocalDate
import java.time.YearMonth

data class CalendarLetter(
    val installment: InstallmentDto,
    val loanId: String,
    val clientName: String,
    val clientCodigo: Int,
)

fun unpaidLettersFromLoans(loans: List<LoanDto>): List<CalendarLetter> {
    return loans.flatMap { loan ->
        loan.installments.orEmpty()
            .filter { isUnpaidInstallment(it.estado) }
            .map { inst ->
                CalendarLetter(
                    installment = inst,
                    loanId = loan.id,
                    clientName = loan.client?.nombre ?: "Cliente",
                    clientCodigo = loan.client?.codigo ?: 0,
                )
            }
    }
}

fun lettersOnDate(letters: List<CalendarLetter>, date: LocalDate): List<CalendarLetter> {
    return letters.filter { parseLocalDate(it.installment.fechaVencimiento) == date }
        .sortedWith(compareBy({ it.clientName.lowercase() }, { it.installment.nro }))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onOpenDay: (LocalDate) -> Unit) {
    val app = LocalContext.current.applicationContext as CredimaxApp
    val today = todayLima()
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val yearMonth = remember(monthKey) { YearMonth.parse(monthKey) }
    var letters by remember { mutableStateOf<List<CalendarLetter>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        runCatching { app.container.api.loans() }
            .onSuccess { letters = unpaidLettersFromLoans(it) }
            .onFailure { error = it.message ?: "No se pudo cargar el calendario" }
        loading = false
    }

    val days = remember(yearMonth) { calendarMonthDays(yearMonth) }
    val byDay = remember(letters, yearMonth) {
        days.associateWith { d -> lettersOnDate(letters, d) }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Calendario") })
        when {
            loading -> LoadingBox()
            error != null -> EmptyText(error!!)
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = { monthKey = yearMonth.minusMonths(1).toString() }) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = "Mes anterior")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(monthTitleEs(yearMonth), style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { monthKey = YearMonth.from(today).toString() }) {
                            Text("Hoy")
                        }
                    }
                    IconButton(onClick = { monthKey = yearMonth.plusMonths(1).toString() }) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "Mes siguiente")
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    WEEKDAY_HEADERS_ES.forEach { label ->
                        Text(
                            label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = SlateMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                days.chunked(7).forEach { week ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                    ) {
                        week.forEach { day ->
                            val dayLetters = byDay[day].orEmpty()
                            DayCell(
                                date = day,
                                inMonth = day.month == yearMonth.month,
                                isToday = day == today,
                                count = dayLetters.size,
                                hasOverdue = dayLetters.any { it.installment.estado == "atrasada" },
                                onClick = { onOpenDay(day) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Solo letras pendientes y atrasadas. Toca un día para verlas.",
                    color = SlateMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    count: Int,
    hasOverdue: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val numberColor = when {
        !inMonth -> SlateMuted.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val markColor = when {
        count == 0 -> Color.Transparent
        hasOverdue -> AmberAlert
        else -> TealDeep
    }
    Box(
        modifier
            .height(62.dp)
            .padding(horizontal = 1.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isToday) Modifier.border(1.5.dp, TealDeep, RoundedCornerShape(10.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = numberColor,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(3.dp))
            if (count > 0) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(if (hasOverdue) AmberAlert.copy(alpha = 0.18f) else TealSoft)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        count.toString(),
                        color = markColor,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDayScreen(
    dateIso: String,
    onLoan: (String) -> Unit,
    onPay: (loanId: String, installmentId: String) -> Unit,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as CredimaxApp
    val date = parseLocalDate(dateIso) ?: todayLima()
    var letters by remember { mutableStateOf<List<CalendarLetter>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(dateIso) {
        loading = true
        error = null
        runCatching { app.container.api.loans() }
            .onSuccess { letters = lettersOnDate(unpaidLettersFromLoans(it), date) }
            .onFailure { error = it.message ?: "No se pudo cargar las letras" }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dueDateLabel(date.toString())) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Atrás") }
                },
            )
        },
    ) { padding ->
        when {
            loading -> LoadingBox()
            error != null -> EmptyText(error!!)
            letters.isEmpty() -> EmptyText("No hay letras pendientes este día.")
            else -> LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        "${letters.size} letra${if (letters.size == 1) "" else "s"} por cobrar",
                        color = SlateMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                items(letters, key = { it.installment.id }) { item ->
                    DayLetterCard(
                        item = item,
                        onLoan = { onLoan(item.loanId) },
                        onPay = { onPay(item.loanId, item.installment.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayLetterCard(
    item: CalendarLetter,
    onLoan: () -> Unit,
    onPay: () -> Unit,
) {
    val inst = item.installment
    val weeks = weeksLateText(weeksLateNow(inst.fechaVencimiento))
    val due = inst.monto + inst.recargoAcumulado
    CredimaxCard(onClick = onLoan) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(item.clientName, style = MaterialTheme.typography.titleMedium)
                    if (item.clientCodigo > 0) {
                        Text("#${item.clientCodigo}", color = SlateMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                StatusChip(installmentStatusLabel(inst.estado), inst.estado)
            }
            Text("Letra ${inst.nro}", color = SlateMuted)
            if (weeks != null) {
                Text(weeks, color = AmberAlert, style = MaterialTheme.typography.titleSmall)
            }
            AmountRow("Cuota", money(inst.monto))
            if (inst.recargoAcumulado > 0) AmountRow("Recargo", money(inst.recargoAcumulado))
            AmountRow("Por pagar", money(due), emphasis = true)
            Spacer(Modifier.height(4.dp))
            PrimaryButton("Registrar pago", onClick = onPay)
        }
    }
}
