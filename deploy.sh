#!/bin/bash
set -e

cd /proyecto/Credimas/
git pull origin master

sudo bash -c '
  export NVM_DIR="/root/.nvm"
  [ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh"
  nvm use 22.13.1

  cd /proyecto/Credimas/backend
  npm install --legacy-peer-deps
  npx prisma generate
  npx prisma migrate deploy
  npm run build

  pm2 restart 8
'

echo "Deploy terminado correctamente."
