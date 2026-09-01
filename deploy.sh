#!/bin/bash
set -e

cd /proyecto/Credimas/
git pull origin main

sudo bash -c '
  export NVM_DIR="/root/.nvm"
  [ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh"
  nvm use 18.9.1

  cd /proyecto/Credimas/backend
  npm install --legacy-peer-deps

  pm2 restart 7
'

echo "Deploy terminado correctamente."