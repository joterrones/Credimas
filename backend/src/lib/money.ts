import { Decimal } from "@prisma/client/runtime/library";

export function money(n: number): number {
  return Math.round((n + Number.EPSILON) * 100) / 100;
}

export function toNumber(value: Decimal | number | string): number {
  return Number(value);
}

export function totalPagar(capital: number, interesPct: number): number {
  return money(capital * (1 + interesPct / 100));
}

export function tasaSemanalFraction(interesPct: number, semanas: number): number {
  return interesPct / 100 / semanas;
}

/** Split total into N weekly amounts; last installment absorbs rounding remainder. */
export function splitInstallments(total: number, semanas: number): number[] {
  const base = money(total / semanas);
  const amounts = Array.from({ length: semanas }, () => base);
  const allocated = money(base * (semanas - 1));
  amounts[semanas - 1] = money(total - allocated);
  return amounts;
}

export function toJson(value: unknown): unknown {
  if (value instanceof Decimal) return value.toNumber();
  if (value instanceof Date) return value.toISOString();
  if (Array.isArray(value)) return value.map(toJson);
  if (value && typeof value === "object") {
    const out: Record<string, unknown> = {};
    for (const [k, v] of Object.entries(value as Record<string, unknown>)) {
      out[k] = toJson(v);
    }
    return out;
  }
  return value;
}

const APP_TZ = process.env.APP_TZ ?? "America/Lima";

function pad2(n: number) {
  return String(n).padStart(2, "0");
}

function utcYmd(date: Date): string {
  return `${date.getUTCFullYear()}-${pad2(date.getUTCMonth() + 1)}-${pad2(date.getUTCDate())}`;
}

function zoneYmd(date: Date, timeZone = APP_TZ): string {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
}

/** Día calendario (YYYY-MM-DD). Prisma `@db.Date` llega como medianoche UTC. */
export function calendarYmd(value: Date | string): string {
  if (typeof value === "string") {
    const s = value.trim();
    if (/^\d{4}-\d{2}-\d{2}/.test(s)) return s.slice(0, 10);
    const parsed = new Date(s);
    return Number.isNaN(parsed.getTime()) ? zoneYmd(new Date()) : calendarYmd(parsed);
  }
  const isUtcMidnight =
    value.getUTCHours() === 0 &&
    value.getUTCMinutes() === 0 &&
    value.getUTCSeconds() === 0 &&
    value.getUTCMilliseconds() === 0;
  return isUtcMidnight ? utcYmd(value) : zoneYmd(value);
}

export function startOfDay(date: Date): Date {
  const [y, m, d] = calendarYmd(date).split("-").map(Number);
  return new Date(Date.UTC(y, m - 1, d));
}

export function addDays(date: Date, days: number): Date {
  const [y, m, d] = calendarYmd(date).split("-").map(Number);
  return new Date(Date.UTC(y, m - 1, d + days));
}
