import { dueDateForInstallment, type PaymentFrequency } from "./frequency";
import { money, splitInstallments, startOfDay, tasaSemanalFraction, totalPagar } from "./money";
import { prisma } from "./prisma";

export type LoanInput = {
  clientId: string;
  quoteId?: string;
  capital: number;
  interesPct: number;
  /** Número de letras / periodos. */
  semanas: number;
  frecuencia?: PaymentFrequency;
  fechaInicio?: Date;
  imagenUrl?: string | null;
};

export async function createLoanWithInstallments(input: LoanInput) {
  const capital = money(input.capital);
  const interesPct = money(input.interesPct);
  const plazos = input.semanas;
  const frecuencia: PaymentFrequency = input.frecuencia ?? "semanal";
  const total = totalPagar(capital, interesPct);
  const tasa = tasaSemanalFraction(interesPct, plazos);
  const amounts = splitInstallments(total, plazos);
  const fechaInicio = startOfDay(input.fechaInicio ?? new Date());

  return prisma.loan.create({
    data: {
      clientId: input.clientId,
      quoteId: input.quoteId,
      capital,
      interesPct,
      semanas: plazos,
      frecuencia,
      totalPagar: total,
      tasaSemanal: tasa,
      fechaInicio,
      imagenUrl: input.imagenUrl,
      estado: "al_dia",
      installments: {
        create: amounts.map((monto, i) => ({
          nro: i + 1,
          fechaVencimiento: dueDateForInstallment(fechaInicio, i + 1, frecuencia),
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
