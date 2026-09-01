import { Router } from "express";
import bcrypt from "bcryptjs";
import { z } from "zod";
import { prisma } from "../lib/prisma";
import { signToken } from "../middleware/auth";
import { asyncHandler, HttpError } from "../middleware/error";

export const authRouter = Router();

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
});

authRouter.post(
  "/login",
  asyncHandler(async (req, res) => {
    const { email, password } = loginSchema.parse(req.body);
    // #region agent log
    fetch("http://127.0.0.1:7871/ingest/80ee1cf7-1984-4e93-892b-0cd955cf56ad", {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Debug-Session-Id": "cc0d77" },
      body: JSON.stringify({
        sessionId: "cc0d77",
        location: "backend/src/routes/auth.ts:login",
        message: "login start",
        data: { hasEmail: Boolean(email) },
        timestamp: Date.now(),
        hypothesisId: "A",
      }),
    }).catch(() => {});
    // #endregion
    let user;
    try {
      user = await prisma.user.findUnique({ where: { email: email.toLowerCase() } });
    } catch (dbErr) {
      // #region agent log
      fetch("http://127.0.0.1:7871/ingest/80ee1cf7-1984-4e93-892b-0cd955cf56ad", {
        method: "POST",
        headers: { "Content-Type": "application/json", "X-Debug-Session-Id": "cc0d77" },
        body: JSON.stringify({
          sessionId: "cc0d77",
          location: "backend/src/routes/auth.ts:findUnique",
          message: "prisma login query failed",
          data: {
            name: dbErr instanceof Error ? dbErr.name : "unknown",
            message: dbErr instanceof Error ? dbErr.message : String(dbErr),
            code: (dbErr as { code?: string }).code,
          },
          timestamp: Date.now(),
          hypothesisId: "A",
        }),
      }).catch(() => {});
      // #endregion
      throw dbErr;
    }
    if (!user || !(await bcrypt.compare(password, user.password))) {
      throw new HttpError(401, "Correo o contraseña incorrectos");
    }
    let token: string;
    try {
      token = signToken({ id: user.id, email: user.email, name: user.name });
    } catch (jwtErr) {
      // #region agent log
      fetch("http://127.0.0.1:7871/ingest/80ee1cf7-1984-4e93-892b-0cd955cf56ad", {
        method: "POST",
        headers: { "Content-Type": "application/json", "X-Debug-Session-Id": "cc0d77" },
        body: JSON.stringify({
          sessionId: "cc0d77",
          location: "backend/src/routes/auth.ts:signToken",
          message: "jwt sign failed",
          data: {
            name: jwtErr instanceof Error ? jwtErr.name : "unknown",
            message: jwtErr instanceof Error ? jwtErr.message : String(jwtErr),
          },
          timestamp: Date.now(),
          hypothesisId: "B",
        }),
      }).catch(() => {});
      // #endregion
      throw jwtErr;
    }
    const payload = { id: user.id, email: user.email, name: user.name };
    res.json({ token, user: payload });
  }),
);
