import { addDays, money, splitInstallments, tasaSemanalFraction, totalPagar } from "./money";
import { prisma } from "./prisma";

export type LoanInput = {
  clientId: string;
  quoteId?: string;
  capital: number;
  interesPct: number;
  semanas: number;
  fechaInicio?: Date;
  imagenUrl?: string | null;
};

export async function createLoanWithInstallments(input: LoanInput) {
  const capital = money(input.capital);
  const interesPct = money(input.interesPct);
  const semanas = input.semanas;
  const total = totalPagar(capital, interesPct);
  const tasa = tasaSemanalFraction(interesPct, semanas);
  const amounts = splitInstallments(total, semanas);
  const fechaInicio = input.fechaInicio ?? new Date();

  return prisma.loan.create({
    data: {
      clientId: input.clientId,
      quoteId: input.quoteId,
      capital,
      interesPct,
      semanas,
      totalPagar: total,
      tasaSemanal: tasa,
      fechaInicio,
      imagenUrl: input.imagenUrl,
      estado: "al_dia",
      installments: {
        create: amounts.map((monto, i) => ({
          nro: i + 1,
          fechaVencimiento: addDays(fechaInicio, 7 * (i + 1)),
          monto,
        })),
      },
    },
    include: {
      client: true,
      installments: { orderBy: { nro: "asc" } },
    },
  });
}
