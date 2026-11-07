import { dueDateForInstallment, type PaymentFrequency } from "./frequency";
import { calendarYmd, splitInstallments, totalPagar } from "./money";

export function previewInstallments(
  capital: number,
  interesPct: number,
  plazos: number,
  from: Date = new Date(),
  frecuencia: PaymentFrequency = "semanal",
) {
  const total = totalPagar(capital, interesPct);
  const amounts = splitInstallments(total, plazos);
  return {
    totalPagar: total,
    frecuencia,
    letras: amounts.map((monto, i) => ({
      nro: i + 1,
      /** Solo día calendario — evita desfaces de zona en la app. */
      fechaVencimiento: calendarYmd(dueDateForInstallment(from, i + 1, frecuencia)),
      monto,
    })),
  };
}
