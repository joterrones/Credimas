import type { Request, Response, NextFunction } from "express";
import jwt from "jsonwebtoken";

export type AuthUser = { id: string; email: string; name: string };

declare global {
  namespace Express {
    interface Request {
      user?: AuthUser;
    }
  }
}

export function signToken(user: AuthUser): string {
  const secret = process.env.JWT_SECRET?.trim() || "credimax-dev-secret-change-me";
  const expiresIn = process.env.JWT_EXPIRES_IN?.trim() || "7d";
  return jwt.sign(user, secret, {
    expiresIn: expiresIn as jwt.SignOptions["expiresIn"],
  });
}

export function auth(req: Request, res: Response, next: NextFunction): void {
  const header = req.headers.authorization;
  const token = header?.startsWith("Bearer ") ? header.slice(7) : undefined;
  if (!token) {
    res.status(401).json({ error: "No autorizado" });
    return;
  }
  const secret = process.env.JWT_SECRET?.trim() || "credimax-dev-secret-change-me";
  try {
    req.user = jwt.verify(token, secret) as AuthUser;
    next();
  } catch {
    res.status(401).json({ error: "Token inválido" });
  }
}
