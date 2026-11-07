# Spec 001 — Préstamo: lo construido + cronograma resumido compartible

**Estado:** feature B **implementada** (compartir = imagen + saludo)  
**Objetivo:** Documentar el préstamo existente y el **reporte resumido del cronograma** con **estado por letra** y **compartir por WhatsApp**.

---

## Parte A — Lo ya construido (préstamo)

### A.1 Ciclo de vida

| Paso | Qué hace | Dónde |
|------|----------|--------|
| Alta vía presupuesto | Quote → convertir con fecha inicio + foto opcional | Presupuestos / Cliente |
| Alta directa | Formulario “Nuevo préstamo” | Cliente / quotes |
| Preview al crear | `GET /preview/installments` → tarjeta **Fechas de pago** | `QuotesScreens` |
| Listado | Filtros Todos / Al día / Atrasados / Pagados | Tab Préstamos |
| Detalle | Cliente, capital, interés, total, foto; letras; **Ver cronograma** | `LoanDetailScreen` |
| Cronograma | Resumen + WhatsApp (imagen) | `LoanCronogramaScreen` |
| Cobro | Fecha de pago, recargo editable (S/10 × semana), comprobante | `PayScreen` |
| Calendario de cobros | Tab aparte | Spec `001_plan_calendario.md` |

### A.2 Reglas vigentes

- Interés flat; letras semanales; recargo sugerido S/10/semana (editable).
- Estados letra: `pendiente` \| `atrasada` \| `pagada` \| `pagada_con_atraso`.
- Fechas: `dueDateLabel` (día en español). Zona America/Lima.

---

## Parte B — Feature: reporte + WhatsApp (implementada)

### Decisiones cerradas

| # | Decisión |
|---|----------|
| 1 | Saludo: `Estimado(a) <Nombre>, le enviamos su cronograma de pagos` |
| 2 | WhatsApp: selector de chat (`com.whatsapp` / `com.whatsapp.w4b`) |
| 3 | Pantalla dedicada |
| 4 | Recargo en UI si `atrasada` / `pagada_con_atraso` |
| 5 | Compartir solo desde el cronograma |
| 6 | **Compartir = imagen** de la tarjeta “Fechas de pago” (hasta la última letra) + **solo el saludo** como texto |

### Contenido UI

- Cabecera cliente, desde, capital, %, semanas, total a devolver.
- Por letra: fecha con día, monto, chip de estado, semanas de atraso si aplica, recargo si aplica.
- Estados UI: Pendiente / Con retraso / Pagado / Pagado con retraso.

### Compartir

1. Captura Compose (`rememberGraphicsLayer`) de la tarjeta **Fechas de pago**.
2. PNG en `cacheDir` vía FileProvider.
3. `ACTION_SEND` `image/png` + `EXTRA_TEXT` = saludo.

### Archivos

| Archivo | Rol |
|---------|-----|
| `LoanCronogramaScreen.kt` | UI + captura + share |
| `LoanCronogramaShare.kt` | `buildLoanCronogramaGreeting`, `saveBitmapToCache`, `shareImageAndTextViaWhatsApp` |
| `Format.kt` | etiquetas de estado |
| `LoansScreens.kt` | Botón Ver cronograma |
| `NavGraph.kt` | `loan/{id}/cronograma` |
| `AndroidManifest.xml` | `<queries>` WhatsApp |

---

## Parte C — Criterios de aceptación

- [x] Cronograma resumido desde el préstamo.
- [x] Total, fechas (día ES), montos, estados, retraso/recargo.
- [x] Compartir por WhatsApp.
- [x] Imagen del bloque Fechas de pago + único texto = saludo.
- [x] Selector de chat; aviso si no hay WhatsApp.
- [x] Sin endpoint nuevo.

---

## Parte D — Cómo verificar

1. Instalar APK (`versionCode` 2 / `1.1`).
2. Préstamo → **Ver cronograma** → **Compartir por WhatsApp**.
3. En el chat: imagen del cronograma + caption del saludo (sin listado en texto).

---

## Parte E — Relación con otros specs

| Spec | Tema |
|------|------|
| `001_plan_calendario.md` | Vista mensual de cobros |
| `001_plan_prestamo.md` (este) | Préstamo + cronograma compartible |
| `002_plan_cliente.md` | Pendiente |
