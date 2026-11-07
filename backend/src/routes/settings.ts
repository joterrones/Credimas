import { Router } from "express";
import { z } from "zod";
import { prisma } from "../lib/prisma";
import { toJson } from "../lib/money";
import { frecuenciaSchema } from "../lib/frequency";
import { asyncHandler, HttpError } from "../middleware/error";

export const settingsRouter = Router();

const plazosList = (max: number) => z.array(z.number().int().min(1).max(max)).min(1);

const updateSchema = z.object({
  interesPctDefault: z.number().min(0).max(100).optional(),
  frecuenciaDefault: frecuenciaSchema.optional(),
  semanasDefault: z.number().int().min(1).max(52).optional(),
  plazosPermitidos: plazosList(52).optional(),
  quincenasDefault: z.number().int().min(1).max(26).optional(),
  plazosQuincenales: plazosList(26).optional(),
  mesesDefault: z.number().int().min(1).max(36).optional(),
  plazosMensuales: plazosList(36).optional(),
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
