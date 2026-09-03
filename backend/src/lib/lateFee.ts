import { startOfDay } from "./money";

/**
 * Semanas de atraso de una letra.
 * El mismo día de vencimiento no genera recargo.
 * Si ya venció pero aún no pasa una semana completa, cuenta 1.
 */
export function weeksLate(dueDate: Date, today: Date = new Date()): number {
  const due = startOfDay(dueDate);
  const now = startOfDay(today);
  if (now.getTime() <= due.getTime()) return 0;
  const days = Math.floor((now.getTime() - due.getTime()) / 86_400_000);
  const weeks = Math.floor(days / 7);
  return weeks === 0 ? 1 : weeks;
}

/** Recargo sugerido por cada semana de atraso (PEN). El operador puede editarlo al cobrar. */
export const RECARGO_POR_SEMANA = 10;

export function lateFee(dueDate: Date, today: Date = new Date()): number {
  const w = weeksLate(dueDate, today);
  if (w <= 0) return 0;
  return Math.round((RECARGO_POR_SEMANA * w + Number.EPSILON) * 100) / 100;
}
