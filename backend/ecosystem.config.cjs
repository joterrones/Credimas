/** PM2 — API Credimax en Linux (sin Docker). */
module.exports = {
  apps: [
    {
      name: "credimax-api",
      cwd: "/proyecto/Credimas/backend",
      script: "dist/index.js",
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
