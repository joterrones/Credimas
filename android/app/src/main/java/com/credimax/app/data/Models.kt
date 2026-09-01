package com.credimax.app.data

data class LoginRequest(val email: String, val password: String)

data class AuthResponse(val token: String, val user: UserDto)

data class UserDto(val id: String, val email: String, val name: String)

data class ApiError(val error: String?)

data class ClientDto(
    val id: String = "",
    val codigo: Int = 0,
    val nombre: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val tipoDocumento: String = "DNI",
    val nroDocumento: String = "",
    val estado: String = "activo",
)

data class ClientBody(
    val nombre: String,
    val direccion: String,
    val telefono: String,
    val tipoDocumento: String,
    val nroDocumento: String,
)

data class QuoteDto(
    val id: String = "",
    val clientId: String = "",
    val capital: Double = 0.0,
    val interesPct: Double = 0.0,
    val semanas: Int = 4,
    val fechaInicio: String = "",
    val notas: String? = null,
    val estado: String = "borrador",
    val totalPagar: Double = 0.0,
    val client: ClientDto? = null,
    val loan: LoanRef? = null,
    val interesProyectado: Double? = null,
)

data class LoanRef(val id: String)

data class QuoteBody(
    val clientId: String,
    val capital: Double,
    val interesPct: Double,
    val semanas: Int,
    val fechaInicio: String? = null,
    val notas: String?,
)

data class LoanBody(
    val clientId: String,
    val capital: Double,
    val interesPct: Double,
    val semanas: Int,
    val fechaInicio: String? = null,
)

data class LoanDto(
    val id: String = "",
    val clientId: String = "",
    val capital: Double = 0.0,
    val interesPct: Double = 0.0,
    val semanas: Int = 0,
    val totalPagar: Double = 0.0,
    val tasaSemanal: Double = 0.0,
    val fechaInicio: String = "",
    val imagenUrl: String? = null,
    val estado: String = "al_dia",
    val client: ClientDto? = null,
    val installments: List<InstallmentDto> = emptyList(),
    val interesPactado: Double? = null,
    val cobrado: Double? = null,
    val pendiente: Double? = null,
    val recargosCobrados: Double? = null,
)

data class InstallmentDto(
    val id: String = "",
    val loanId: String = "",
    val nro: Int = 0,
    val fechaVencimiento: String = "",
    val monto: Double = 0.0,
    val recargoAcumulado: Double = 0.0,
    val montoPagado: Double = 0.0,
    val estado: String = "pendiente",
    val pagadaEn: String? = null,
    val payments: List<PaymentDto> = emptyList(),
    val totalAdeudado: Double? = null,
    val loan: LoanDto? = null,
)

data class PaymentDto(
    val id: String = "",
    val monto: Double = 0.0,
    val recargo: Double = 0.0,
    val fecha: String = "",
    val comprobanteUrl: String? = null,
    val notas: String? = null,
)

data class PayResponse(val payment: PaymentDto?, val loan: LoanDto?)

data class SettingsDto(
    val id: String = "default",
    val interesPctDefault: Double = 10.0,
    val semanasDefault: Int = 4,
    val plazosPermitidos: List<Int> = listOf(4, 6, 8, 10, 12, 16),
    val moneda: String = "PEN",
)

data class SettingsBody(
    val interesPctDefault: Double,
    val semanasDefault: Int,
    val plazosPermitidos: List<Int>,
)

data class KpisDto(
    val interesGanado: Double = 0.0,
    val interesPactado: Double = 0.0,
    val recargosCobrados: Double = 0.0,
    val deudasPorCobrar: Double = 0.0,
    val cobrosRetrasados: Double = 0.0,
    val prestamosActivos: Int = 0,
)

data class PrestamosPorEstado(
    val al_dia: Int = 0,
    val atrasado: Int = 0,
    val pagado: Int = 0,
    val activo: Int = 0,
)

data class CarteraDto(
    val capital: Double = 0.0,
    val montosCobrados: Double = 0.0,
    val cuentasPorCobrar: Double = 0.0,
    val interes: Double = 0.0,
)

data class DashboardDto(
    val kpis: KpisDto = KpisDto(),
    val cartera: CarteraDto = CarteraDto(),
    val prestamosPorEstado: PrestamosPorEstado = PrestamosPorEstado(),
    val cobrosRetrasados: List<InstallmentDto> = emptyList(),
    val vencimientosSemana: List<InstallmentDto> = emptyList(),
)

data class PreviewLetraDto(
    val nro: Int = 0,
    val fechaVencimiento: String = "",
    val monto: Double = 0.0,
)

data class PreviewDto(
    val totalPagar: Double = 0.0,
    val letras: List<PreviewLetraDto> = emptyList(),
)

data class QuotesReportKpis(
    val abiertos: Int = 0,
    val capitalPotencial: Double = 0.0,
    val interesProyectado: Double = 0.0,
    val totalPotencial: Double = 0.0,
)

data class QuotesReportDto(
    val kpis: QuotesReportKpis = QuotesReportKpis(),
    val quotes: List<QuoteDto> = emptyList(),
)

data class CarteraReportDto(
    val kpis: CarteraDto = CarteraDto(),
)
