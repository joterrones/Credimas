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
    const user = await prisma.user.findUnique({ where: { email: email.toLowerCase() } });
    if (!user || !(await bcrypt.compare(password, user.password))) {
      throw new HttpError(401, "Correo o contraseña incorrectos");
    }
    const payload = { id: user.id, email: user.email, name: user.name };
    res.json({ token: signToken(payload), user: payload });
  }),
);
