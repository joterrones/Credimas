import { Router } from "express";
import { z } from "zod";
import { previewInstallments } from "../lib/preview";
import { toJson, startOfDay } from "../lib/money";
import { parseFechaInicio } from "../lib/cartera";
import { frecuenciaSchema, maxPlazos } from "../lib/frequency";
import { asyncHandler, HttpError } from "../middleware/error";

export const previewRouter = Router();

const querySchema = z.object({
  capital: z.coerce.number().positive(),
  interesPct: z.coerce.number().min(0).max(100),
  semanas: z.coerce.number().int().min(1),
  frecuencia: frecuenciaSchema.optional(),
  fechaInicio: z.string().optional(),
});

previewRouter.get(
  "/installments",
  asyncHandler(async (req, res) => {
    const data = querySchema.parse(req.query);
    const frecuencia = data.frecuencia ?? "semanal";
    const max = maxPlazos(frecuencia);
    if (data.semanas > max) {
      throw new HttpError(400, `El plazo máximo para ${frecuencia} es ${max}`);
    }
    const from = parseFechaInicio(data.fechaInicio) ?? startOfDay(new Date());
    res.json(
      toJson(previewInstallments(data.capital, data.interesPct, data.semanas, from, frecuencia)),
    );
  }),
);
