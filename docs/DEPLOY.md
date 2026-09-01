# Despliegue Linux + PM2 (sin Docker)

Servidor: Node **18.9.1**, código en `/proyecto/credimax/backend`. La API no usa contenedores.

Prisma 6 recomienda Node 18.18+. En 18.9.1 suele funcionar; si `prisma generate` falla, sube a 18.20 LTS con `nvm`.

## Primera vez

```bash
cd /proyecto/credimax/backend
cp .env.example .env
# Edita DATABASE_URL, JWT_SECRET, PORT, HOST, UPLOAD_DIR
mkdir -p uploads

npm install --legacy-peer-deps
npx prisma migrate deploy
npx prisma db seed
npm run build

# Si aún no hay proceso PM2:
pm2 start ecosystem.config.cjs
pm2 save
pm2 startup
```

Usuario seed: `admin@credimax.pe` / `admin123`. Cambia la contraseña en producción.

## Actualizar (el flujo que usas)

```bash
cd /proyecto/credimax/backend
npm install --legacy-peer-deps
npx prisma migrate deploy
npm run build
pm2 restart 1
pm2 restart 0
```

`npm install` ya corre `prisma generate` (script `postinstall`).

O en un paso:

```bash
bash /proyecto/credimax/backend/scripts/deploy-pm2.sh
```

Comprueba con `pm2 list` cuál id es la API (`credimax-api` o el nombre que le hayas puesto). Reinicia esos ids.

## `.env` en el servidor

| Variable | Uso |
|----------|-----|
| `DATABASE_URL` | Postgres (RDS u otro). Añade `sslmode=require` si el servidor lo exige |
| `JWT_SECRET` | Secreto propio, no el de desarrollo |
| `JWT_EXPIRES_IN` | Default `7d` |
| `PORT` | Default `3000` |
| `HOST` | `0.0.0.0` para escuchar en la red |
| `UPLOAD_DIR` | Default `uploads` (ruta relativa al cwd de PM2) |
| `APP_TZ` | Default `America/Lima` |

No commitees `.env`.

## PM2 útil

```bash
pm2 list
pm2 logs 0
pm2 logs 1
curl -s http://127.0.0.1:3000/health
```

La app debe responder `{ "ok": true, "name": "Credimax API" }`.

## App Android

En `android/app/build.gradle.kts`, `BASE_URL` debe ser la URL pública del servidor (con `/` al final), no `10.0.2.2`.
