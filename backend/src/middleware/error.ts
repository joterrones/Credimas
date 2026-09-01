import type { Request, Response, NextFunction } from "express";
import { ZodError } from "zod";

export class HttpError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
  }
}

export function errorHandler(
  err: unknown,
  _req: Request,
  res: Response,
  _next: NextFunction,
): void {
  if (err instanceof HttpError) {
    res.status(err.status).json({ error: err.message });
    return;
  }
  if (err instanceof ZodError) {
    res.status(400).json({
      error: "Datos inválidos",
      details: err.issues.map((i) => ({ path: i.path.join("."), message: i.message })),
    });
    return;
  }
  const detail = (() => {
    if (err instanceof Error) {
      const code = (err as { code?: string }).code;
      return { name: err.name, message: err.message, code };
    }
    return { message: String(err) };
  })();
  // #region agent log
  fetch("http://127.0.0.1:7871/ingest/80ee1cf7-1984-4e93-892b-0cd955cf56ad", {
    method: "POST",
    headers: { "Content-Type": "application/json", "X-Debug-Session-Id": "cc0d77" },
    body: JSON.stringify({
      sessionId: "cc0d77",
      location: "backend/src/middleware/error.ts:errorHandler",
      message: "unhandled 500",
      data: detail,
      timestamp: Date.now(),
      hypothesisId: "A",
    }),
  }).catch(() => {});
  // #endregion
  console.error(err);
  res.status(500).json({ error: "Error interno del servidor", detail });
}

export function asyncHandler(
  fn: (req: Request, res: Response, next: NextFunction) => Promise<unknown>,
) {
  return (req: Request, res: Response, next: NextFunction) => {
    fn(req, res, next).catch(next);
  };
}
