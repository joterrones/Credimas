import { Router } from "express";
import { z } from "zod";
import { prisma } from "../lib/prisma";
import { toJson, totalPagar } from "../lib/money";
import { createLoanWithInstallments } from "../lib/createLoan";
import { parseFechaInicio } from "../lib/cartera";
import { upload, uploadedUrl } from "../lib/upload";
import { asyncHandler, HttpError } from "../middleware/error";

export const quotesRouter = Router();

const quoteBody = z.object({
  clientId: z.string().uuid(),
  capital: z.number().positive(),
  interesPct: z.number().min(0).max(100),
  semanas: z.number().int().min(1).max(52),
  fechaInicio: z.string().optional(),
  notas: z.string().optional(),
});

quotesRouter.get(
  "/",
  asyncHandler(async (req, res) => {
    const estado = typeof req.query.estado === "string" ? req.query.estado : undefined;
    const clientId = typeof req.query.clientId === "string" ? req.query.clientId : undefined;
    const quotes = await prisma.quote.findMany({
      where: {
        ...(estado ? { estado: estado as never } : {}),
        ...(clientId ? { clientId } : {}),
      },
      include: { client: true, loan: { select: { id: true } } },
      orderBy: { createdAt: "desc" },
    });
    res.json(
      toJson(
        quotes.map((q) => ({
          ...q,
          totalPagar: totalPagar(Number(q.capital), Number(q.interesPct)),
        })),
      ),
    );
  }),
);

quotesRouter.get(
  "/:id",
  asyncHandler(async (req, res) => {
    const quote = await prisma.quote.findUnique({
      where: { id: String(req.params.id) },
      include: { client: true, loan: true },
    });
    if (!quote) throw new HttpError(404, "Presupuesto no encontrado");
    res.json(
      toJson({
        ...quote,
        totalPagar: totalPagar(Number(quote.capital), Number(quote.interesPct)),
      }),
    );
  }),
);

quotesRouter.post(
  "/",
  asyncHandler(async (req, res) => {
    const data = quoteBody.parse(req.body);
    const client = await prisma.client.findUnique({ where: { id: data.clientId } });
    if (!client) throw new HttpError(404, "Cliente no encontrado");
    const quote = await prisma.quote.create({
      data: {
        clientId: data.clientId,
        capital: data.capital,
        interesPct: data.interesPct,
        semanas: data.semanas,
        fechaInicio: parseFechaInicio(data.fechaInicio) ?? new Date(),
        notas: data.notas,
      },
      include: { client: true },
    });
    res.status(201).json(
      toJson({
        ...quote,
        totalPagar: totalPagar(data.capital, data.interesPct),
      }),
    );
  }),
);

quotesRouter.put(
  "/:id",
  asyncHandler(async (req, res) => {
    const data = quoteBody.partial().parse(req.body);
    const quote = await prisma.quote.findUnique({ where: { id: String(req.params.id) } });
    if (!quote) throw new HttpError(404, "Presupuesto no encontrado");
    if (quote.estado === "convertido" || quote.estado === "rechazado") {
      throw new HttpError(400, "No se puede editar un presupuesto convertido o rechazado");
    }
    const { fechaInicio, ...rest } = data;
    const updated = await prisma.quote.update({
      where: { id: String(req.params.id) },
      data: {
        ...rest,
        ...(fechaInicio !== undefined
          ? { fechaInicio: parseFechaInicio(fechaInicio) ?? new Date() }
          : {}),
      },
      include: { client: true },
    });
    res.json(
      toJson({
        ...updated,
        totalPagar: totalPagar(Number(updated.capital), Number(updated.interesPct)),
      }),
    );
  }),
);

quotesRouter.post(
  "/:id/reject",
  asyncHandler(async (req, res) => {
    const quote = await prisma.quote.findUnique({ where: { id: String(req.params.id) } });
    if (!quote) throw new HttpError(404, "Presupuesto no encontrado");
    if (quote.estado === "convertido") {
      throw new HttpError(400, "El presupuesto ya fue convertido en préstamo");
    }
    const updated = await prisma.quote.update({
      where: { id: String(req.params.id) },
      data: { estado: "rechazado" },
      include: { client: true },
    });
    res.json(toJson(updated));
  }),
);

quotesRouter.post(
  "/:id/convert",
  upload.single("imagen"),
  asyncHandler(async (req, res) => {
    const quote = await prisma.quote.findUnique({ where: { id: String(req.params.id) } });
    if (!quote) throw new HttpError(404, "Presupuesto no encontrado");
    if (quote.estado === "convertido") {
      throw new HttpError(400, "El presupuesto ya fue convertido");
    }
    if (quote.estado === "rechazado") {
      throw new HttpError(400, "No se puede convertir un presupuesto rechazado");
    }

    const bodyFecha =
      req.body && typeof req.body === "object"
        ? parseFechaInicio((req.body as { fechaInicio?: string }).fechaInicio)
        : undefined;
    const loan = await createLoanWithInstallments({
      clientId: quote.clientId,
      quoteId: quote.id,
      capital: Number(quote.capital),
      interesPct: Number(quote.interesPct),
      semanas: quote.semanas,
      fechaInicio: bodyFecha ?? quote.fechaInicio,
      imagenUrl: uploadedUrl(req.file),
    });

    await prisma.quote.update({
      where: { id: quote.id },
      data: { estado: "convertido" },
    });

    res.status(201).json(toJson(loan));
  }),
);
