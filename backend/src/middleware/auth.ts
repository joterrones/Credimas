import type { Request, Response, NextFunction } from "express";
import jwt from "jsonwebtoken";

const JWT_SECRET = process.env.JWT_SECRET ?? "credimax-dev-secret-change-me";

export type AuthUser = { id: string; email: string; name: string };

declare global {
  namespace Express {
    interface Request {
      user?: AuthUser;
    }
  }
}

export function signToken(user: AuthUser): string {
  return jwt.sign(user, JWT_SECRET, {
    expiresIn: (process.env.JWT_EXPIRES_IN ?? "7d") as jwt.SignOptions["expiresIn"],
  });
}

export function auth(req: Request, res: Response, next: NextFunction): void {
  const header = req.headers.authorization;
  const token = header?.startsWith("Bearer ") ? header.slice(7) : undefined;
  if (!token) {
    res.status(401).json({ error: "No autorizado" });
    return;
  }
  try {
    req.user = jwt.verify(token, JWT_SECRET) as AuthUser;
    next();
  } catch {
    res.status(401).json({ error: "Token inválido" });
  }
}
