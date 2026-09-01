#!/usr/bin/env bash
# Despliegue en Linux + PM2 (sin Docker).
# Uso: bash scripts/deploy-pm2.sh
set -euo pipefail

cd /proyecto/credimax/backend

node -v
npm install --legacy-peer-deps
npx prisma generate
npx prisma migrate deploy
npm run build
mkdir -p uploads

pm2 restart 1
pm2 restart 0
pm2 save
pm2 status
