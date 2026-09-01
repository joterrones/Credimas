import path from "path";
import dotenv from "dotenv";

/** Carpeta `backend/`, aunque PM2 arranque sin cwd (script absoluto a dist/index.js). */
export const backendRoot = path.resolve(__dirname, "..");
const envPath = path.resolve(backendRoot, ".env");
dotenv.config({ path: envPath });

// #region agent log
fetch("http://127.0.0.1:7871/ingest/80ee1cf7-1984-4e93-892b-0cd955cf56ad", {
  method: "POST",
  headers: { "Content-Type": "application/json", "X-Debug-Session-Id": "cc0d77" },
  body: JSON.stringify({
    sessionId: "cc0d77",
    location: "backend/src/loadEnv.ts",
    message: "env loaded",
    data: {
      envPath,
      backendRoot,
      hasDatabaseUrl: Boolean(process.env.DATABASE_URL),
      cwd: process.cwd(),
    },
    timestamp: Date.now(),
    hypothesisId: "A",
  }),
}).catch(() => {});
// #endregion
