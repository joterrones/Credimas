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
| `semanas` | Plazo; presets de settings suelen ser 4, 6, 8; la API acepta 1–52 |
| `notas` | Opcional |
| `estado` | `borrador` → `convertido` o `rechazado` |

Estados `convertido` y `rechazado` no se editan. Convertir un rechazado está prohibido.

Al convertir: se crea el `Loan` ligado por `quoteId` (1:1) y el presupuesto pasa a `convertido`.

También se puede crear un préstamo directo (`POST /loans`) sin presupuesto.

## Préstamo — interés flat

El interés no es compuesto ni se aplica saldo decreciente.

```
totalPagar = round2(capital * (1 + interesPct / 100))
tasaSemanal = (interesPct / 100) / semanas     // fracción, no porcentaje
letra_i     = round2(totalPagar / semanas)
letra_N     = totalPagar - suma(letra_1..letra_{N-1})   // absorbe redondeo
```

**Ejemplo canónico:** S/ 1 000 al 10% en 4 semanas.

- `totalPagar` = 1 100
- 4 letras de **275**
- `tasaSemanal` = 0.025

Implementación: `totalPagar`, `tasaSemanalFraction`, `splitInstallments` en `backend/src/lib/money.ts`.

`fechaInicio` la elige el operador al crear el presupuesto o el préstamo (por defecto hoy). La letra `n` vence en `fechaInicio + 7n` días (primera letra a la semana).

Estado del préstamo (derivado, no se setea a mano salvo el alta):

| Estado | Condición |
|--------|-----------|
| `pagado` | Todas las letras `pagada` o `pagada_con_atraso` |
| `atrasado` | Alguna letra `atrasada` |
| `al_dia` | Hay pendientes y ninguna atrasada |
| `activo` | Valor de schema; el alta escribe `al_dia` |

## Letra (Installment)

Una obligación semanal. Cada letra acumula recargo **por su cuenta**.

| Estado | Significado |
|--------|-------------|
| `pendiente` | No vencida o vencida el mismo día (sin recargo) |
| `atrasada` | Vencida; recargo > 0 |
| `pagada` | Cobrada a tiempo (recargo 0) |
| `pagada_con_atraso` | Cobrada con recargo congelado |

Al pagar se **congela** `recargoAcumulado` y `montoPagado = monto + recargo`. No hay pagos parciales.

## Recargo por atraso

Sobre el **monto original de la letra**, no sobre recargos anteriores.

El **mismo día de vencimiento no genera recargo**. Las fechas de letra son solo día (sin hora); se comparan en calendario `America/Lima` para no marcar “atrasada” por el desfase UTC.

```
semanasAtraso:
  si fechaPago <= vencimiento → 0
  si no:
    weeks = floor(días_calendario / 7)
    si weeks == 0 → 1     // ya venció, aunque no complete 7 días
    si no → weeks

recargo = round2(montoLetra * tasaSemanal * semanasAtraso)
totalAPagarLetra = montoLetra + recargo
```

Al cobrar, `fechaPago` la elige el operador (por defecto hoy; no puede ser futura). El recargo **sugerido** se calcula contra esa fecha, no contra el momento del registro. El operador puede editar el recargo al cobrar (incluso 0). Si no envía `recargo`, el servidor usa el cálculo. Así un cobro real a tiempo no genera mora si se anota días después.

**Ejemplo:** letra 275, tasa 0.025, 1 semana de atraso → **6.88**. Dos semanas → **13.75**.

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
- `semanasDefault` (seed: 4) — plazo inicial al crear presupuesto/préstamo
- `plazosPermitidos` (seed: `[4, 6, 8, 10, 12, 16]`) — atajos de UI; se editan como lista (ej. `4, 8, 12, 16`). El plazo real de cada operación es 1–52
- `moneda` = `PEN`

## Reporte de cartera

| Indicador | Definición |
|-----------|------------|
| **Capital** | Suma del dinero desembolsado (`loan.capital`) |
| **Montos cobrados** | Suma de `montoPagado` de letras pagadas (incluye interés de la letra y recargo si hubo atraso) |
| **Cuentas por cobrar** | Letras aún no cobradas (`monto` + recargo actual) |
| **Interés** | Utilidad cobrada: interés de cada letra pagada + recargos cobrados (sin capital) |

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
