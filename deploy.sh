#!/bin/bash
set -e

cd /proyecto/Credimas/
git pull origin master

sudo bash -c '
  export NVM_DIR="/root/.nvm"
  [ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh"
  nvm use 22.13.1

  cd /proyecto/Credimas/backend

  if [ ! -f .env ]; then
    cp .env.example .env
    echo "ERROR: se creó backend/.env desde .env.example."
    echo "Edita DATABASE_URL y JWT_SECRET (copia los valores de tu .env local) y vuelve a desplegar."
    exit 1
  fi

  npm install --legacy-peer-deps
  npx prisma generate
  npx prisma migrate deploy
  npm run build

  pm2 restart node_credimax
'

echo "Deploy terminado correctamente."
