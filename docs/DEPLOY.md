# Despliegue Linux + PM2 (sin Docker)

Servidor: Node en `/proyecto/Credimas/backend`. PM2 app **`node_credimax`** (ecosystem compartido, no este `ecosystem.config.cjs`).

El bloque en tu `ecosystem` debe incluir `cwd` para que encuentre `.env` y `uploads`:

```js
{
  name: "node_credimax",
  script: "/proyecto/Credimas/backend/dist/index.js",
  cwd: "/proyecto/Credimas/backend",
  env: { NODE_ENV: "production", PORT: "3700" },
}
```

Tras cambiar el ecosystem: `pm2 delete node_credimax` y vuelve a `pm2 start` tu archivo, o `pm2 restart node_credimax --update-env`.

**Obligatorio:** en el servidor no basta `.env.example`. Crea el archivo real (no se sube a git):

```bash
cd /proyecto/Credimas/backend
cp .env.example .env
nano .env
```

Copia `DATABASE_URL` y `JWT_SECRET` desde tu `backend/.env` local. Sin eso el login da 500.

## Actualizar

```bash
bash /proyecto/Credimas/deploy.sh
# git pull, prisma generate, migrate, build, pm2 restart node_credimax
```

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
| `PORT` | `3700` |
| `HOST` | `0.0.0.0` para escuchar en la red |
| `UPLOAD_DIR` | Default `uploads` (ruta relativa al cwd de PM2) |
| `APP_TZ` | Default `America/Lima` |

No commitees `.env`.

## PM2 útil

```bash
pm2 list
pm2 logs 0
pm2 logs 1
curl -s http://127.0.0.1:3700/health
```

La app debe responder `{ "ok": true, "name": "Credimax API" }`.

## GitHub Actions (deploy automático)

Workflow: [`.github/workflows/main.yml`](../.github/workflows/main.yml). En push a `master` (o manual) corre SSH al EC2 y ejecuta `deploy.sh`.

### Error: `can't connect without a private SSH key or password`

El runner **sí arrancó**, pero el secret **`SSH_PRIVATE_KEY`** está vacío o no existe en el repo. No es que se hayan “roto” las claves en el servidor: Actions no recibió ninguna clave para conectar.

**Arreglo** (repo Credimax en GitHub):

1. Settings → Secrets and variables → Actions → **New repository secret**
2. Name exacto: `SSH_PRIVATE_KEY` (igual que en el workflow)
3. Value: contenido **completo** de la clave privada PEM que usa `ubuntu` en el EC2  
   (incluye las líneas `-----BEGIN ... PRIVATE KEY-----` y `-----END ...-----`)
4. Guarda y vuelve a lanzar el workflow (**Actions → Run workflow** o un push a `master`)

Comprueba que el secret exista en **este** repo (no solo en otro, p. ej. solredes-aws). Cada repositorio tiene sus propios secrets.

Si la clave pública correspondiente no está en `~ubuntu/.ssh/authorized_keys` del EC2, el siguiente error sería `Permission denied` (ahí sí es el servidor). El error actual es solo “no hay key ni password en el Action”.

## App Android

En `android/app/build.gradle.kts`, `BASE_URL` debe ser la URL pública del servidor (con `/` al final), no `10.0.2.2`.
