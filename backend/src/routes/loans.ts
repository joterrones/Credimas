import { Router } from "express";
import { z } from "zod";
import { prisma } from "../lib/prisma";
import { toJson, money, startOfDay } from "../lib/money";
import { createLoanWithInstallments } from "../lib/createLoan";
import { refreshLoanStatus, refreshOverdue } from "../lib/loanStatus";
import { lateFee } from "../lib/lateFee";
import { parseFechaInicio } from "../lib/cartera";
import { upload, uploadedUrl } from "../lib/upload";
import { asyncHandler, HttpError } from "../middleware/error";

export const loansRouter = Router();

const createLoanSchema = z.object({
  clientId: z.string().uuid(),
  capital: z.coerce.number().positive(),
  interesPct: z.coerce.number().min(0).max(100),
  semanas: z.coerce.number().int().min(1).max(52),
  fechaInicio: z.string().optional(),
});

loansRouter.get(
  "/",
  asyncHandler(async (req, res) => {
    await refreshOverdue();
    const estado = typeof req.query.estado === "string" ? req.query.estado : undefined;
    const clientId = typeof req.query.clientId === "string" ? req.query.clientId : undefined;
    const loans = await prisma.loan.findMany({
      where: {
        ...(estado ? { estado: estado as never } : {}),
        ...(clientId ? { clientId } : {}),
      },
      include: {
        client: true,
        installments: { orderBy: { nro: "asc" } },
      },
      orderBy: { createdAt: "desc" },
    });
    res.json(toJson(loans));
  }),
);

loansRouter.get(
  "/:id",
  asyncHandler(async (req, res) => {
    await refreshOverdue(String(req.params.id));
    const loan = await prisma.loan.findUnique({
      where: { id: String(req.params.id) },
      include: {
        client: true,
        quote: true,
        installments: {
          orderBy: { nro: "asc" },
          include: { payments: { orderBy: { fecha: "desc" } } },
        },
      },
    });
    if (!loan) throw new HttpError(404, "Préstamo no encontrado");
    res.json(toJson(loan));
  }),
);

loansRouter.post(
  "/",
  upload.single("imagen"),
  asyncHandler(async (req, res) => {
    const data = createLoanSchema.parse(req.body);
    const client = await prisma.client.findUnique({ where: { id: data.clientId } });
    if (!client) throw new HttpError(404, "Cliente no encontrado");
    const loan = await createLoanWithInstallments({
      ...data,
      fechaInicio: parseFechaInicio(data.fechaInicio),
      imagenUrl: uploadedUrl(req.file),
    });
    res.status(201).json(toJson(loan));
  }),
);

loansRouter.post(
  "/:id/installments/:installmentId/pay",
  upload.single("comprobante"),
  asyncHandler(async (req, res) => {
    const loan = await prisma.loan.findUnique({ where: { id: String(req.params.id) } });
    if (!loan) throw new HttpError(404, "Préstamo no encontrado");

    const installment = await prisma.installment.findFirst({
      where: { id: String(req.params.installmentId), loanId: loan.id },
    });
    if (!installment) throw new HttpError(404, "Letra no encontrada");
    if (installment.estado === "pagada" || installment.estado === "pagada_con_atraso") {
      throw new HttpError(400, "Esta letra ya está pagada");
    }

    const rawFecha = typeof req.body.fechaPago === "string" ? req.body.fechaPago : undefined;
    const fechaPago = startOfDay(parseFechaInicio(rawFecha) ?? new Date());
    if (fechaPago > startOfDay(new Date())) {
      throw new HttpError(400, "La fecha de pago no puede ser futura");
    }

    const rawRecargo = req.body.recargo;
    const parsedRecargo =
      rawRecargo !== undefined && rawRecargo !== null && String(rawRecargo).trim() !== ""
        ? money(Number(String(rawRecargo).replace(",", ".")))
        : undefined;
    if (parsedRecargo !== undefined && (!Number.isFinite(parsedRecargo) || parsedRecargo < 0)) {
      throw new HttpError(400, "El recargo no es válido");
    }
    const recargo =
      parsedRecargo ?? lateFee(new Date(installment.fechaVencimiento), fechaPago);
    const totalDue = money(Number(installment.monto) + recargo);
    const notas = typeof req.body.notas === "string" ? req.body.notas : undefined;
    const comprobanteUrl = uploadedUrl(req.file) ?? null;
    const withLateFee = recargo > 0;

    const payment = await prisma.$transaction(async (tx) => {
      const p = await tx.payment.create({
        data: {
          installmentId: installment.id,
          monto: totalDue,
          recargo,
          fecha: fechaPago,
          comprobanteUrl,
          notas,
        },
      });
      await tx.installment.update({
        where: { id: installment.id },
        data: {
          recargoAcumulado: recargo,
          montoPagado: totalDue,
          estado: withLateFee ? "pagada_con_atraso" : "pagada",
          pagadaEn: fechaPago,
        },
      });
      return p;
    });

    await refreshLoanStatus(loan.id);

    const updated = await prisma.loan.findUnique({
      where: { id: loan.id },
      include: {
        client: true,
        installments: {
          orderBy: { nro: "asc" },
          include: { payments: { orderBy: { fecha: "desc" } } },
        },
      },
    });

    res.status(201).json(toJson({ payment, loan: updated }));
  }),
);
