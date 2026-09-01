# Credimax

Plataforma de microcréditos semanales: API Node.js + PostgreSQL y app Android (Kotlin + Jetpack Compose).

Documentación para desarrollo:

- [Arquitectura](docs/ARCHITECTURE.md)
- [Dominio y cálculos](docs/DOMAIN.md)
- [API HTTP](docs/API.md)

## Requisitos

- Node.js 18.9+ (producción actual: 18.9.1). Prisma 6 recomienda 18.18+
- Android Studio (Hedgehog o superior) con JDK 17
- PostgreSQL (RDS u otra instancia; la URL va en `backend/.env`)

## Arranque rápido (backend)

Configura `DATABASE_URL` en `backend/.env` y luego:

```bash
cd backend
npm install
npx prisma migrate deploy
npx prisma db seed
npm run dev
```

API en `http://localhost:3000`. Usuario inicial: `admin@credimax.pe` / `admin123`.

Producción Linux + PM2 (sin Docker): [docs/DEPLOY.md](docs/DEPLOY.md).

## App Android

Abre la carpeta `android/` en Android Studio. Para el emulador, la URL por defecto es `http://10.0.2.2:3000` (localhost de tu PC). En un dispositivo físico, cambia `BASE_URL` en `app/build.gradle.kts` a la IP de tu máquina en la LAN.

## Reglas de crédito

Interés flat sobre el capital, repartido en N semanas. Recargo por atraso = letra × (interés% / N) × semanas de atraso.
