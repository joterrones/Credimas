# API HTTP — Credimax

Base URL de desarrollo: `http://localhost:3700`. JSON UTF-8 salvo el cobro (multipart). Decimales Prisma salen como `number`. Fechas ISO-8601.

Errores: `{ "error": "mensaje" }`. Validación Zod: `{ "error": "Datos inválidos", "details": [{ "path", "message" }] }`.

## Auth

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| GET | `/health` | No | `{ ok, name }` |
| POST | `/auth/login` | No | JWT |
| * | `/clients`, `/quotes`, `/loans`, `/reports`, `/preview`, `/settings` | Bearer | JWT en `Authorization: Bearer <token>` |

`POST /auth/login`

```json
{ "email": "admin@credimax.pe", "password": "admin123" }
```

```json
{
  "token": "<jwt>",
  "user": { "id": "...", "email": "admin@credimax.pe", "name": "Administrador" }
}
```

401 si credenciales inválidas o token ausente/vencido.

## Clientes

| Método | Ruta | Notas |
|--------|------|-------|
| GET | `/clients?q=` | Busca nombre, documento, teléfono; si `q` es numérico, también `codigo` |
| GET | `/clients/:id` | |
| POST | `/clients` | 201. 409 si documento duplicado |
| PUT | `/clients/:id` | Parcial |

Body de alta:

```json
{
  "nombre": "Ana Pérez",
  "direccion": "Av. Arequipa 123, Lima",
  "telefono": "999888777",
  "tipoDocumento": "DNI",
  "nroDocumento": "12345678"
}
```

`tipoDocumento`: `DNI` \| `CE`. DNI: `/^\d{8}$/`. CE: `/^[A-Za-z0-9]{8,12}$/`.

## Presupuestos

| Método | Ruta | Notas |
|--------|------|-------|
| GET | `/quotes?estado=&clientId=` | Incluye `client`, `totalPagar`. Filtros opcionales |
| GET | `/quotes/:id` | |
| POST | `/quotes` | 201, estado `borrador` |
| PUT | `/quotes/:id` | No si `convertido` o `rechazado` |
| POST | `/quotes/:id/reject` | → `rechazado` |
| POST | `/quotes/:id/convert` | 201. Crea préstamo + letras; quote → `convertido`. JSON o multipart: `fechaInicio` opcional, `imagen` opcional |

Body:

```json
{
  "clientId": "<uuid>",
  "capital": 1000,
  "interesPct": 10,
  "semanas": 4,
  "fechaInicio": "2026-09-01",
  "notas": "opcional"
}
```

`capital` > 0, `interesPct` 0–100, `semanas` entero 1–52.

## Préstamos

| Método | Ruta | Notas |
|--------|------|-------|
| GET | `/loans?estado=&clientId=` | Llama `refreshOverdue`. Filtros opcionales |
| GET | `/loans/:id` | Letras + pagos. Recalcula mora de ese préstamo |
| POST | `/loans` | Alta directa. JSON o multipart (`imagen` opcional). Body: `clientId`, `capital`, `interesPct`, `semanas`, `fechaInicio` opcional |
| POST | `/loans/:id/installments/:installmentId/pay` | Multipart. `comprobante` opcional |

Filtro `estado`: `al_dia` \| `atrasado` \| `pagado` \| `activo`.

### Cobro

`Content-Type: multipart/form-data`

| Parte | Tipo | Requerido |
|-------|------|-----------|
| `comprobante` | file image/* o application/pdf | No |
| `notas` | text | No |
| `fechaPago` | `YYYY-MM-DD` | No (default: hoy). No puede ser futura |

El recargo se calcula con `fechaPago` vs el vencimiento de la letra. 400 si ya está pagada o si la fecha es futura. `Loan.imagenUrl` y `Payment.comprobanteUrl` son rutas `/uploads/...` o `null`.

Respuesta 201: `{ "payment": {...}, "loan": {...} }`.

## Reportes y settings

| Método | Ruta | Notas |
|--------|------|-------|
| GET | `/reports/dashboard` | KPIs, `prestamosPorEstado`, `cobrosRetrasados[]`, `vencimientosSemana[]` |
| GET | `/reports/loans?estado=` | Cartera con `cobrado`, `pendiente`, `interesPactado`, `recargosCobrados` |
| GET | `/reports/quotes` | Solo `borrador` y `aprobado`. KPIs `abiertos`, `capitalPotencial`, `interesProyectado`, `totalPotencial` + lista |
| GET | `/reports/cartera` | `{ kpis: { capital, montosCobrados, cuentasPorCobrar, interes } }` |
| GET | `/preview/installments?capital=&interesPct=&semanas=&fechaInicio=` | Calendario desde `fechaInicio` (default hoy). Letra n vence 7n días después |
| GET | `/settings` | Fila `default` |
| PUT | `/settings` | `interesPctDefault`, `semanasDefault`, `plazosPermitidos[]`, `moneda` |

Definición de KPIs: [DOMAIN.md](DOMAIN.md#reportes-definiciones).

## Estáticos

`GET /uploads/<archivo>` — fotos de préstamo y comprobantes. Sin auth en esta versión (el path es opaco). Endurecer si se expone a internet.

## Cómo probar (PowerShell)

```powershell
Invoke-RestMethod http://localhost:3700/health

$login = Invoke-RestMethod http://localhost:3700/auth/login -Method POST `
  -ContentType 'application/json' `
  -Body '{"email":"admin@credimax.pe","password":"admin123"}'
$h = @{ Authorization = "Bearer $($login.token)" }

Invoke-RestMethod http://localhost:3700/reports/dashboard -Headers $h
```

Flujo automatizado (API debe estar arriba): `cd backend && npm run test:e2e`.
