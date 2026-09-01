/**
 * Referencia. En el servidor el proceso real es `node_credimax`
 * en el ecosystem compartido (junto a solredes, adinelsa, else).
 *
 * En ese archivo, el bloque Credimax debe quedar así:
 *
 * {
 *   name: "node_credimax",
 *   script: "/proyecto/Credimas/backend/dist/index.js",
 *   cwd: "/proyecto/Credimas/backend",
 *   env: { NODE_ENV: "production", PORT: "3700" },
 * }
 */
module.exports = {
  apps: [
    {
      name: "node_credimax",
      cwd: "/proyecto/Credimas/backend",
      script: "/proyecto/Credimas/backend/dist/index.js",
      instances: 1,
      exec_mode: "fork",
      autorestart: true,
      max_memory_restart: "400M",
      env: {
        NODE_ENV: "production",
        PORT: "3700",
      },
    },
  ],
};
