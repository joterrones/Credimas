import { addDays, splitInstallments, totalPagar } from "./money";

export function previewInstallments(
  capital: number,
  interesPct: number,
  semanas: number,
  from: Date = new Date(),
) {
  const total = totalPagar(capital, interesPct);
  const amounts = splitInstallments(total, semanas);
  return {
    totalPagar: total,
    letras: amounts.map((monto, i) => ({
      nro: i + 1,
      fechaVencimiento: addDays(from, 7 * (i + 1)),
      monto,
    })),
  };
}
