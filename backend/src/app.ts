import express from "express";
import cors from "cors";
import path from "path";
import { authRouter } from "./routes/auth";
import { clientsRouter } from "./routes/clients";
import { quotesRouter } from "./routes/quotes";
import { loansRouter } from "./routes/loans";
import { reportsRouter } from "./routes/reports";
import { settingsRouter } from "./routes/settings";
import { previewRouter } from "./routes/preview";
import { auth } from "./middleware/auth";
import { errorHandler } from "./middleware/error";

export function createApp() {
  const app = express();
  const uploadDir = path.resolve(process.env.UPLOAD_DIR ?? "uploads");

  app.use(cors());
  app.use(express.json());
  app.use("/uploads", express.static(uploadDir));

  app.get("/health", (_req, res) => {
    res.json({ ok: true, name: "Credimax API" });
  });

  app.use("/auth", authRouter);
  app.use("/clients", auth, clientsRouter);
  app.use("/quotes", auth, quotesRouter);
  app.use("/loans", auth, loansRouter);
  app.use("/reports", auth, reportsRouter);
  app.use("/preview", auth, previewRouter);
  app.use("/settings", auth, settingsRouter);

  app.use(errorHandler);
  return app;
}
