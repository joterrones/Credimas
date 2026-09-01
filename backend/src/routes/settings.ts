import { Router } from "express";
import { z } from "zod";
import { prisma } from "../lib/prisma";
import { toJson } from "../lib/money";
import { asyncHandler, HttpError } from "../middleware/error";

export const settingsRouter = Router();

const updateSchema = z.object({
  interesPctDefault: z.number().min(0).max(100).optional(),
  semanasDefault: z.number().int().min(1).max(52).optional(),
  plazosPermitidos: z.array(z.number().int().min(1).max(52)).min(1).optional(),
  moneda: z.string().min(1).max(8).optional(),
});

settingsRouter.get(
  "/",
  asyncHandler(async (_req, res) => {
    const settings = await prisma.settings.findUnique({ where: { id: "default" } });
    if (!settings) throw new HttpError(404, "Configuración no encontrada");
    res.json(toJson(settings));
  }),
);

settingsRouter.put(
  "/",
  asyncHandler(async (req, res) => {
    const data = updateSchema.parse(req.body);
    const settings = await prisma.settings.update({
      where: { id: "default" },
      data,
    });
    res.json(toJson(settings));
  }),
);
