import { z } from "zod";
import { addDays, calendarYmd } from "./money";

export const FREQUENCIES = ["semanal", "quincenal", "mensual"] as const;
export type PaymentFrequency = (typeof FREQUENCIES)[number];

export const frecuenciaSchema = z.enum(FREQUENCIES);

/** Máximo de cuotas permitidas por frecuencia. */
export function maxPlazos(frecuencia: PaymentFrequency): number {
  switch (frecuencia) {
    case "quincenal":
      return 26;
    case "mensual":
      return 36;
    default:
      return 52;
  }
}

export function plazosSchema(frecuencia: PaymentFrequency) {
  return z.coerce.number().int().min(1).max(maxPlazos(frecuencia));
}

/** Días entre letras (null = meses calendario). */
export function daysPerPeriod(frecuencia: PaymentFrequency): number | null {
  switch (frecuencia) {
    case "semanal":
      return 7;
    case "quincenal":
      return 14;
    case "mensual":
      return null;
  }
}

/** Suma meses calendario preservando día (clamp al último día del mes). */
export function addMonths(date: Date, months: number): Date {
  const [y, m, d] = calendarYmd(date).split("-").map(Number);
  const totalMonths = y * 12 + (m - 1) + months;
  const ny = Math.floor(totalMonths / 12);
  const nm = totalMonths % 12;
  const lastDay = new Date(Date.UTC(ny, nm + 1, 0)).getUTCDate();
  return new Date(Date.UTC(ny, nm, Math.min(d, lastDay)));
}

/** Vencimiento de la letra `n` (1-based) desde fechaInicio. */
export function dueDateForInstallment(
  fechaInicio: Date,
  nro: number,
  frecuencia: PaymentFrequency,
): Date {
  const days = daysPerPeriod(frecuencia);
  if (days != null) return addDays(fechaInicio, days * nro);
  return addMonths(fechaInicio, nro);
}

export function frequencyLabel(frecuencia: PaymentFrequency, n: number): string {
  switch (frecuencia) {
    case "quincenal":
      return n === 1 ? "1 quincena" : `${n} quincenas`;
    case "mensual":
      return n === 1 ? "1 mes" : `${n} meses`;
    default:
      return n === 1 ? "1 semana" : `${n} semanas`;
  }
}

export function firstDueHint(frecuencia: PaymentFrequency): string {
  switch (frecuencia) {
    case "quincenal":
      return "La primera letra vence 14 días después";
    case "mensual":
      return "La primera letra vence 1 mes después";
    default:
      return "La primera letra vence 7 días después";
  }
}
