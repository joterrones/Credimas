import { Router } from "express";
import { prisma } from "../lib/prisma";
import { toJson, money, totalPagar } from "../lib/money";
import { refreshOverdue } from "../lib/loanStatus";
import { computeCartera } from "../lib/cartera";
import { asyncHandler } from "../middleware/error";

export const reportsRouter = Router();

reportsRouter.get(
  "/dashboard",
  asyncHandler(async (_req, res) => {
    await refreshOverdue();

    const loans = await prisma.loan.findMany({
      include: { installments: true, client: true },
    });

    let interesPactado = 0;
    let recargosCobrados = 0;
    let deudasPorCobrar = 0;
    let cobrosRetrasadosMonto = 0;

    const porEstado = { al_dia: 0, atrasado: 0, pagado: 0, activo: 0 };

    for (const loan of loans) {
      interesPactado += Number(loan.totalPagar) - Number(loan.capital);
      porEstado[loan.estado] += 1;

      for (const inst of loan.installments) {
        recargosCobrados +=
          inst.estado === "pagada" || inst.estado === "pagada_con_atraso"
            ? Number(inst.recargoAcumulado)
            : 0;

        if (inst.estado === "pendiente" || inst.estado === "atrasada") {
          deudasPorCobrar += Number(inst.monto) + Number(inst.recargoAcumulado);
        }
        if (inst.estado === "atrasada") {
          cobrosRetrasadosMonto += Number(inst.monto) + Number(inst.recargoAcumulado);
        }
      }
    }

    const overdueInstallments = await prisma.installment.findMany({
      where: { estado: "atrasada" },
      include: {
        loan: { include: { client: true } },
      },
      orderBy: { fechaVencimiento: "asc" },
    });

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const weekEnd = new Date(today);
    weekEnd.setDate(weekEnd.getDate() + 7);

    const vencimientosSemana = await prisma.installment.findMany({
      where: {
        estado: { in: ["pendiente", "atrasada"] },
        fechaVencimiento: { gte: today, lte: weekEnd },
      },
      include: { loan: { include: { client: true } } },
      orderBy: { fechaVencimiento: "asc" },
    });

    res.json(
      toJson({
        cartera: computeCartera(loans),
        kpis: {
          interesGanado: money(interesPactado + recargosCobrados),
          interesPactado: money(interesPactado),
          recargosCobrados: money(recargosCobrados),
          deudasPorCobrar: money(deudasPorCobrar),
          cobrosRetrasados: money(cobrosRetrasadosMonto),
          prestamosActivos: porEstado.al_dia + porEstado.atrasado + porEstado.activo,
        },
        prestamosPorEstado: porEstado,
        cobrosRetrasados: overdueInstallments.map((i) => ({
          ...i,
          totalAdeudado: money(Number(i.monto) + Number(i.recargoAcumulado)),
        })),
        vencimientosSemana,
      }),
    );
  }),
);

reportsRouter.get(
  "/loans",
  asyncHandler(async (req, res) => {
    await refreshOverdue();
    const estado = typeof req.query.estado === "string" ? req.query.estado : undefined;
    const loans = await prisma.loan.findMany({
      where: estado ? { estado: estado as never } : undefined,
      include: {
        client: true,
        installments: { orderBy: { nro: "asc" } },
      },
      orderBy: { createdAt: "desc" },
    });

    const rows = loans.map((loan) => {
      const cobrado = loan.installments
        .filter((i) => i.estado === "pagada" || i.estado === "pagada_con_atraso")
        .reduce((s, i) => s + Number(i.montoPagado), 0);
      const pendiente = loan.installments
        .filter((i) => i.estado === "pendiente" || i.estado === "atrasada")
        .reduce((s, i) => s + Number(i.monto) + Number(i.recargoAcumulado), 0);
      const recargos = loan.installments.reduce(
        (s, i) =>
          i.estado === "pagada" || i.estado === "pagada_con_atraso"
            ? s + Number(i.recargoAcumulado)
            : s,
        0,
      );
      return {
        ...loan,
        interesPactado: money(Number(loan.totalPagar) - Number(loan.capital)),
        cobrado: money(cobrado),
        pendiente: money(pendiente),
        recargosCobrados: money(recargos),
      };
    });

    res.json(toJson(rows));
  }),
);

reportsRouter.get(
  "/cartera",
  asyncHandler(async (_req, res) => {
    await refreshOverdue();
    const loans = await prisma.loan.findMany({
      include: { installments: true },
    });
    res.json(toJson({ kpis: computeCartera(loans) }));
  }),
);

reportsRouter.get(
  "/quotes",
  asyncHandler(async (_req, res) => {
    const quotes = await prisma.quote.findMany({
      where: { estado: { in: ["borrador", "aprobado"] } },
      include: { client: true },
      orderBy: { createdAt: "desc" },
    });

    let capitalPotencial = 0;
    let totalPotencial = 0;
    const rows = quotes.map((q) => {
      const capital = Number(q.capital);
      const total = totalPagar(capital, Number(q.interesPct));
      capitalPotencial += capital;
      totalPotencial += total;
      return {
        ...q,
        totalPagar: total,
        interesProyectado: money(total - capital),
      };
    });

    res.json(
      toJson({
        kpis: {
          abiertos: quotes.length,
          capitalPotencial: money(capitalPotencial),
          interesProyectado: money(totalPotencial - capitalPotencial),
          totalPotencial: money(totalPotencial),
        },
        quotes: rows,
      }),
    );
  }),
);
