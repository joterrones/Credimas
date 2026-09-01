import { Router } from "express";
import { z } from "zod";
import { previewInstallments } from "../lib/preview";
import { toJson } from "../lib/money";
import { asyncHandler } from "../middleware/error";

export const previewRouter = Router();

const querySchema = z.object({
  capital: z.coerce.number().positive(),
  interesPct: z.coerce.number().min(0).max(100),
  semanas: z.coerce.number().int().min(1).max(52),
  fechaInicio: z.string().optional(),
});

previewRouter.get(
  "/installments",
  asyncHandler(async (req, res) => {
    const data = querySchema.parse(req.query);
    const from = data.fechaInicio
      ? new Date(`${data.fechaInicio.slice(0, 10)}T00:00:00`)
      : new Date();
    res.json(toJson(previewInstallments(data.capital, data.interesPct, data.semanas, from)));
  }),
);
