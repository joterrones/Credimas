# Dominio — Credimax

Reglas de negocio que implementa el backend. Un especialista debe poder implementar o auditar cálculos leyendo este archivo y `backend/src/lib/`.

## Actores

Un **administrador**. Identifica **clientes**, propone **presupuestos**, emite **préstamos**, cobra **letras** y consulta **cartera**.

## Cliente

Identificación operativa:

| Campo | Regla |
|-------|--------|
| `codigo` | Entero correlativo (`autoincrement`). Es el “número” de cliente |
| `nombre`, `direccion` | Obligatorios |
| `telefono` | 9 dígitos |
| `tipoDocumento` | `DNI` o `CE` |
| `nroDocumento` | Único. DNI: 8 dígitos. CE: 8–12 alfanumérico |
| `estado` | `activo` \| `inactivo` |

## Presupuesto (Quote)

Propuesta previa al desembolso. No genera obligaciones hasta convertirse.

| Campo | Significado |
|-------|-------------|
| `capital` | Monto a prestar (PEN) |
| `interesPct` | Interés flat sobre el capital (ej. `10` = 10%) |
| `frecuencia` | `semanal` (default) \| `quincenal` \| `mensual`. Una sola por operación; no se mezcla |
| `semanas` | Número de letras / periodos (aunque el nombre sea histórico). Semanal 1–52, quincenal 1–26, mensual 1–36 |
| `notas` | Opcional |
| `estado` | `borrador` → `convertido` o `rechazado` |

Estados `convertido` y `rechazado` no se editan. Convertir un rechazado está prohibido.

Al convertir: se crea el `Loan` ligado por `quoteId` (1:1) y el presupuesto pasa a `convertido`.

También se puede crear un préstamo directo (`POST /loans`) sin presupuesto.

## Préstamo — interés flat

El interés no es compuesto ni se aplica saldo decreciente.

```
totalPagar = round2(capital * (1 + interesPct / 100))
tasaPeriodo = (interesPct / 100) / N     // N = nro de letras; campo DB histórico tasaSemanal
letra_i     = round2(totalPagar / N)
letra_N     = totalPagar - suma(letra_1..letra_{N-1})   // absorbe redondeo
```

**Ejemplo canónico:** S/ 1 000 al 10% en 4 semanas.

- `totalPagar` = 1 100
- 4 letras de **275**
- `tasaSemanal` = 0.025

Mismo split para quincenal o mensual: el interés flat se reparte en N letras; solo cambian las fechas de vencimiento.

Implementación: `totalPagar`, `tasaSemanalFraction`, `splitInstallments` en `backend/src/lib/money.ts`; vencimientos en `backend/src/lib/frequency.ts`.

`fechaInicio` la elige el operador (por defecto hoy). La letra `n` vence según frecuencia:

| Frecuencia | Vencimiento letra `n` |
|------------|------------------------|
| `semanal` | `fechaInicio + 7n` días |
| `quincenal` | `fechaInicio + 14n` días |
| `mensual` | `fechaInicio + n` meses calendario (día clamp al fin de mes) |

Estado del préstamo (derivado, no se setea a mano salvo el alta):

| Estado | Condición |
|--------|-----------|
| `pagado` | Todas las letras `pagada` o `pagada_con_atraso` |
| `atrasado` | Alguna letra `atrasada` |
| `al_dia` | Hay pendientes y ninguna atrasada |
| `activo` | Valor de schema; el alta escribe `al_dia` |

## Letra (Installment)

Una obligación por periodo (semana, quincena o mes). Cada letra acumula recargo **por su cuenta**.

| Estado | Significado |
|--------|-------------|
| `pendiente` | No vencida o vencida el mismo día (sin recargo) |
| `atrasada` | Vencida; recargo > 0 |
| `pagada` | Cobrada a tiempo (recargo 0) |
| `pagada_con_atraso` | Cobrada con recargo congelado |

Al pagar se **congela** `recargoAcumulado` y `montoPagado = monto + recargo`. No hay pagos parciales.

## Recargo por atraso

Monto fijo: **S/10 por cada semana de atraso**, independiente del capital o de la letra. No se capitaliza sobre recargos anteriores.

El **mismo día de vencimiento no genera recargo**. Las fechas de letra son solo día (sin hora); se comparan en calendario `America/Lima` para no marcar “atrasada” por el desfase UTC.

```
semanasAtraso:
  si fechaPago <= vencimiento → 0
  si no:
    weeks = floor(días_calendario / 7)
    si weeks == 0 → 1     // ya venció, aunque no complete 7 días
    si no → weeks

recargoSugerido = 10 * semanasAtraso
totalAPagarLetra = montoLetra + recargo
```

Al cobrar, `fechaPago` la elige el operador (por defecto hoy; no puede ser futura). El recargo **sugerido** se calcula contra esa fecha (S/10 por semana). El operador puede editar el recargo al cobrar (incluso 0). Si no envía `recargo`, el servidor usa el cálculo. Así un cobro real a tiempo no genera mora si se anota días después.

**Ejemplo:** 1 semana de atraso → **S/10**. Dos semanas → **S/20**.

Si la letra 1 se atrasa y la 2 se paga al día, solo la 1 acumula. Cubierto en `backend/src/lib/lateFee.ts` y `scripts/test-calculations.ts`.

## Pago

Registro único por letra (en la práctica: una letra no pagada acepta un `POST .../pay`).

- Si el cliente envía `recargo` (≥ 0), el servidor lo aplica. Si no, lo calcula con `fechaPago`. El monto de la letra no lo envía el cliente.
- `Payment.fecha` y `Installment.pagadaEn` guardan esa fecha de pago.
- El comprobante (foto o PDF) es opcional.
- Al emitir un préstamo se puede adjuntar una foto opcional (`Loan.imagenUrl`).
- `Payment.monto` = letra + recargo; `Payment.recargo` = recargo de esa operación.

## Settings

Fila única `id = "default"`:

- `interesPctDefault` (seed: 10)
- `frecuenciaDefault` (seed: `semanal`)
- `semanasDefault` (seed: 4) + `plazosPermitidos` (seed: `[4, 6, 8, 10, 12, 16]`) — atajos semanales (1–52)
- `quincenasDefault` (seed: 2) + `plazosQuincenales` (seed: `[2, 3, 4, 6]`) — atajos quincenales (1–26)
- `mesesDefault` (seed: 1) + `plazosMensuales` (seed: `[1, 2, 3, 6, 12]`) — atajos mensuales (1–36)
- `moneda` = `PEN`

En la UI solo se muestran los atajos de la frecuencia elegida (no se mezclan).

## Reporte de cartera

Indicadores en `computeCartera` / `GET /reports/dashboard` → `cartera` (tras `refreshOverdue`).

| Indicador | Definición |
|-----------|------------|
| **Cartera por cobrar** (`cuentasPorCobrar`) | Σ (`monto` + `recargoAcumulado`) de letras impagas |
| **En mora** (`enMora`) | Igual, solo letras `atrasada` |
| **Cobrado** (`montosCobrados`) | Σ `montoPagado` de letras pagadas |
| **Utilidad cobrada** (`utilidadCobrada` / `interes`) | En letras pagadas: (monto − capital de la letra) + recargo |
| **Capital en calle** (`capitalEnCalle`) | Σ capital de letras impagas (`splitInstallments`) |
| **Desembolsado histórico** (`desembolsadoHistorico` / `capital`) | Σ `loan.capital` de todos los préstamos |
| **Utilidad pendiente** | Interés de letras impagas (sin mora) |
| **PAR %** (`parPct`) | `enMora / cuentasPorCobrar × 100` (0 si no hay cartera) |

Inicio muestra: Cartera por cobrar, En mora, Cobrado, Utilidad cobrada. El desembolsado histórico y el capital en calle van en Reportes.

## Reportes (definiciones)

Calculados en `GET /reports/dashboard` **después** de `refreshOverdue`.

| KPI | Definición |
|-----|------------|
| `interesPactado` | Σ (`totalPagar` − `capital`) de todos los préstamos |
| `recargosCobrados` | Σ `recargoAcumulado` de letras ya pagadas |
| `interesGanado` | `interesPactado` + `recargosCobrados` |
| `deudasPorCobrar` | Σ (monto + recargo) de letras `pendiente` o `atrasada` |
| `cobrosRetrasados` | Igual, solo letras `atrasada` |
| `prestamosActivos` | Conteo `al_dia` + `atrasado` + `activo` |

`GET /reports/loans` agrega por préstamo: `interesPactado`, `cobrado`, `pendiente`, `recargosCobrados`.

## Fuera de alcance (v1)

Pagos parciales, capitalización de mora, app del prestatario, multi-asesor, notificaciones push, web admin.
