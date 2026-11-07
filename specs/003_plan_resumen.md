# Spec 003 — Tab Inicio (resumen / cartera) + rediseño de KPIs

**Estado:** **MVP de KPIs implementado** (Inicio + Reportes + `computeCartera`)  
**Objetivo:** Documentar el tab **Inicio**, explicar el fallo del card **Capital**, y proponer KPIs alineados a práctica de microcrédito (cartera, mora, recaudo, utilidad).
---

## Parte A — Lo implementado hoy (Inicio)

### A.1 Entrada y datos

| Tema | Detalle |
|------|---------|
| Pantalla | [`HomeScreen.kt`](android/app/src/main/java/com/credimax/app/ui/screens/home/HomeScreen.kt) |
| API | `GET /reports/dashboard` (tras `refreshOverdue`) |
| DTO | `DashboardDto` → `cartera`, `cobrosRetrasados`, `vencimientosSemana`, `kpis`, … |
| Cálculo cartera | [`computeCartera`](backend/src/lib/cartera.ts) |

### A.2 Layout actual

1. Título **Cartera** + subtítulo “Resumen de microcréditos”.
2. **4 KPI tiles** (2×2):
   - Capital  
   - Montos cobrados  
   - Cuentas por cobrar  
   - Interés  
3. Acciones: **Nuevo cliente** · **Presupuesto**.
4. Bloque colapsable **Letras atrasadas** (cerrado por defecto) — contador, lista con cliente, letra, fecha (día ES), semanas de atraso, monto + recargo; toque → préstamo.
5. Bloque colapsable **Vence esta semana** (abierto por defecto) — próximas 7 días; enlace **Ver reportes**.

### A.3 Definiciones actuales de los 4 cards

| Card UI | Campo API | Fórmula real |
|---------|-----------|--------------|
| **Capital** | `cartera.capital` | Σ `loan.capital` de **todos** los préstamos (incluidos los ya `pagado`) |
| **Montos cobrados** | `cartera.montosCobrados` | Σ `montoPagado` de letras `pagada` / `pagada_con_atraso` |
| **Cuentas por cobrar** | `cartera.cuentasPorCobrar` | Σ (`monto` + `recargoAcumulado`) de letras `pendiente` / `atrasada` |
| **Interés** | `cartera.interes` | En letras pagadas: (monto letra − capital de esa letra) + recargo cobrado |

Documentado también en [`docs/DOMAIN.md`](docs/DOMAIN.md) (sección Reporte de cartera).

### A.4 Por qué Capital no refleja “dinero disponible”

- **No es caja.** Credimax no registra saldo inicial, depósitos ni retiros. No existe “efectivo en mano”.
- **Es histórico de desembolsos.** Suma todo lo prestado alguna vez. Un préstamo 100 % cobrado **sigue sumando** en Capital.
- **No baja al cobrar.** Cobrar letras sube “Montos cobrados” / “Interés”, pero Capital permanece.
- **Confunde control operativo.** Quien mira Inicio puede creer que ese monto está “fuera” o “disponible”, y ninguna de las dos lecturas es correcta.

Conclusión de producto: el card **Capital** (definición actual) **no sirve** como indicador de liquidez ni de dinero usable para nuevos créditos.

---

## Parte B — Qué necesita ver un operador (y un analista de microcrédito)

En microfinanzas (cartera de microcrédito semanal, un solo administrador) los indicadores útiles responden a cuatro preguntas:

| Pregunta | Familia de indicadores |
|----------|------------------------|
| ¿Cuánto me deben hoy? | **Cartera / saldo** |
| ¿Qué tan sana está esa deuda? | **Mora / riesgo (PAR)** |
| ¿Estoy cobrando bien? | **Recaudo** |
| ¿Cuánto gano y cuánto capital tengo expuesto? | **Rentabilidad y exposición** |

Credimax **no** tiene caja contable hoy: no se puede mostrar “efectivo disponible” sin un fondo. Los indicadores de abajo asumen solo préstamos, letras y pagos.

---

## Parte C — Propuesta de indicadores (criterio experto → Credimax)

Convenciones:

- Letra **impaga** = `pendiente` \| `atrasada`
- Letra **pagada** = `pagada` \| `pagada_con_atraso`
- `saldoLetra` = `monto` + `recargoAcumulado` (impagas)
- Capital de una letra = `splitInstallments(loan.capital, semanas)[nro-1]` (misma base que el interés en `computeCartera`)

### C.1 Tab Inicio — 4 cards primarios (recomendados)

Prioridad: lectura en 3 segundos, sin saturar el móvil.

| # | Indicador (UI) | Nombre técnico | Por qué lo usan los expertos | Fórmula en Credimax |
|---|----------------|----------------|------------------------------|---------------------|
| 1 | **Cartera por cobrar** | Portfolio outstanding | “¿Cuánto dinero está todavía en la calle (cuotas + mora)?” Es el saldo vivo de la operación. | Σ `saldoLetra` de letras impagas *(hoy: `cuentasPorCobrar`)* |
| 2 | **En mora** | PAR monto (simplificado) | “¿Cuánto de esa cartera ya venció y no se cobró?” Señal de riesgo y prioridad de cobro. | Σ `saldoLetra` de letras `atrasada` *(ya existe en dashboard como `kpis.cobrosRetrasados` / lista)* |
| 3 | **Cobrado** | Collections | “¿Cuánto efectivo ya entró por letras?” Control de recaudo acumulado. | Σ `montoPagado` de letras pagadas *(hoy: `montosCobrados`)* |
| 4 | **Utilidad cobrada** | Realized yield | “¿Cuánto gané de verdad?” Solo interés + recargos de lo ya pagado (no lo pactado a futuro). | (monto letra − capital letra) + recargo en letras pagadas *(hoy: `interes`)* |

**Se retira de Inicio:** “Capital” = Σ desembolsos históricos (confunde con liquidez).

**Sustituto de exposición de capital (si se insiste en 4.º tile distinto de mora):**  
**Capital en calle** = Σ capital de letras impagas. Complementa “Cartera por cobrar” (esta incluye interés pactado en la letra + mora; capital en calle es solo principal).

**Recomendación de layout Inicio (2×2):**

```
[ Cartera por cobrar ]  [ En mora          ]
[ Cobrado            ]  [ Utilidad cobrada ]
```

Justificación: la fila de arriba = **posición y riesgo**; la de abajo = **resultado ya realizado**. “Capital en calle” puede ir en Reportes o como 5.º dato secundario para no romper el grid.

### C.2 Tab Inicio — listas (ya alineadas a operación)

| Bloque | Indicador operativo | Uso experto |
|--------|---------------------|-------------|
| Letras atrasadas | Cola de cobranza en mora | Gestión diaria PAR |
| Vence esta semana | Pipeline de cobro 7 días | Anticipar caja de la semana |

Mantener colapsables (atrasadas cerradas / semana abierta).

### C.3 Reportes — indicadores secundarios (segunda capa)

No saturar Inicio; sí mostrar en **Reportes** (y/o ampliar `GET /reports/dashboard`).

| Indicador | Uso | Fórmula |
|-----------|-----|---------|
| **% Cartera en mora (PAR %)** | Salud de cartera (estándar MF) | `En mora / Cartera por cobrar` (0 si cartera = 0) |
| **Capital en calle** | Principal aún no recuperado | Σ capital de letras impagas |
| **Desembolsado histórico** | Volumen prestado alguna vez | Σ `loan.capital` *(el “Capital” actual, renombrado)* |
| **Interés pactado** | Utilidad contractual aún no toda cobrada | Σ (`totalPagar` − `capital`) *(ya `kpis.interesPactado`)* |
| **Utilidad pendiente** | Interés aún no cobrado en letras impagas | Σ (monto letra − capital letra) en impagas |
| **Préstamos activos** | Escala de la cartera | Conteo `al_dia` + `atrasado` *(ya `prestamosActivos`)* |
| **Ticket promedio** | Tamaño típico de operación | Desembolsado de activos / préstamos activos |
| **Recargos cobrados** | Ingreso por mora realizada | Σ recargo de letras pagadas *(ya en kpis)* |

### C.4 Indicadores que un experto pediría pero Credimax aún no puede (fase 2+)

| Indicador | Por qué | Requisito |
|-----------|---------|-----------|
| **Disponible para prestar / caja** | Liquidez real | Fondo inicial + movimientos (desembolso, cobro, retiro) |
| **PAR 7 / PAR 30** | Mora por antigüedad | Buckets por días/semanas de atraso (ya hay `weeksLate`) |
| **Tasa de cobro del periodo** | Cobrado en la semana / vencido en la semana | Filtro por `fechaPago` y vencimientos del periodo |
| **ROE / margen sobre capital** | Rentabilidad del dueño | Caja + costos operativos (no modelados) |
| **Clientes con refrendo / renovación** | Retención | Reglas de negocio de 2.º préstamo |

### C.5 Mapa “antes → después” (Inicio)

| Hoy | Problema | Después (recomendado) |
|-----|----------|------------------------|
| Capital | Histórico ≠ disponible ni exposición viva | Fuera de Inicio → “Desembolsado histórico” en Reportes |
| Montos cobrados | OK | **Cobrado** |
| Cuentas por cobrar | OK, nombre técnico | **Cartera por cobrar** |
| Interés | Nombre ambiguo (¿pactado o cobrado?) | **Utilidad cobrada** |
| *(no estaba)* | Falta riesgo en el grid | **En mora** |

### C.6 Colores sugeridos

- Cartera por cobrar → teal (posición).  
- En mora → ámbar (alerta).  
- Cobrado → verde.  
- Utilidad cobrada → verde / teal profundo.

Subtítulo: **“Posición, mora y resultado”**.

---

## Parte D — Plan de implementación (cuando se apruebe el set)

### D.1 MVP Inicio (sin caja)

1. Backend `computeCartera` / dashboard: exponer `enMora` (o reutilizar `kpis.cobrosRetrasados` en UI) y opcional `capitalEnCalle`; renombrar semántica en docs.  
2. Android `HomeScreen`: 4 tiles C.1; quitar Capital histórico.  
3. Reportes: mostrar PAR %, desembolsado histórico, préstamos activos (datos ya casi todos en API).  
4. `DOMAIN.md` + este spec; `test:calc` para capital en calle / PAR si se calculan nuevos campos.  
5. Subir `versionCode` del APK.

### D.2 Fase 2 (opcional)

- Fondo/caja → “Disponible para prestar”.  
- PAR por semanas de atraso.  
- Cobrado vs vencido de la semana calendario.

Sin endpoint nuevo obligatorio en MVP: `GET /reports/dashboard` (extender JSON de `cartera` si hace falta).

---

## Parte E — Decisiones aplicadas

1. Inicio 2×2: **Cartera por cobrar · En mora · Cobrado · Utilidad cobrada**.  
2. Sin caja en esta fase.  
3. Capital en calle, desembolsado histórico, PAR %, etc. en **Reportes**.  
4. Capital en calle = capital de cualquier letra impaga.

---

## Parte F — Criterios de aceptación (rediseño)

- [x] Inicio no muestra “Capital” histórico como KPI principal.
- [x] Inicio muestra los 4 indicadores de C.1 con labels claros.
- [x] “En mora” = suma de letras atrasadas (`cartera.enMora`).
- [x] Cobrado / Utilidad cobrada = letras pagadas.
- [x] Listas colapsables intactas.
- [x] DOMAIN + este spec actualizados.
- [x] `test:calc` cubre cartera (en mora, capital en calle, PAR).

### Archivos tocados

| Archivo | Cambio |
|---------|--------|
| `backend/src/lib/cartera.ts` | `enMora`, `capitalEnCalle`, `utilidadCobrada`, `parPct`, … |
| `backend/scripts/test-calculations.ts` | asserts de cartera |
| `HomeScreen.kt` | 4 tiles nuevos |
| `ReportsScreen.kt` | KPIs + detalle PAR / capital en calle / desembolsado |
| `Models.kt` | `CarteraDto` ampliado |
| `docs/DOMAIN.md` / `ARCHITECTURE.md` | definiciones |
---

## Parte G — Criterios de documentación

- [x] Inicio documentado según implementación.
- [x] Problema del card Capital explicado.
- [x] Propuesta de indicadores con criterio de microfinanzas y mapeo a Credimax.

---

## Parte H — Relación con otros specs

| Spec | Tema |
|------|------|
| `001_plan_calendario.md` | Calendario de vencimientos |
| `001_plan_prestamo.md` | Detalle préstamo + cronograma WhatsApp |
| `002_plan_cliente.md` | Cliente (pendiente) |
| `003_plan_resumen.md` (este) | Tab Inicio / cartera / KPIs |
