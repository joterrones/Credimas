import { money, splitInstallments, startOfDay } from "./money";

type Inst = {
  nro: number;
  monto: unknown;
  recargoAcumulado: unknown;
  montoPagado: unknown;
  estado: string;
};

type LoanLike = {
  capital: unknown;
  semanas: number;
  totalPagar: unknown;
  installments: Inst[];
};

function isPaid(estado: string) {
  return estado === "pagada" || estado === "pagada_con_atraso";
}

function isUnpaid(estado: string) {
  return estado === "pendiente" || estado === "atrasada";
}

export function computeCartera(loans: LoanLike[]) {
  let desembolsadoHistorico = 0;
  let montosCobrados = 0;
  let cuentasPorCobrar = 0;
  let enMora = 0;
  let capitalEnCalle = 0;
  let utilidadCobrada = 0;
  let utilidadPendiente = 0;

  for (const loan of loans) {
    const cap = Number(loan.capital);
    desembolsadoHistorico += cap;
    const shares = splitInstallments(cap, loan.semanas);

    for (const inst of loan.installments) {
      const capitalLetra = shares[inst.nro - 1] ?? 0;
      const interesLetra = Number(inst.monto) - capitalLetra;
      const recargo = Number(inst.recargoAcumulado);

      if (isPaid(inst.estado)) {
        montosCobrados += Number(inst.montoPagado);
        utilidadCobrada += interesLetra + recargo;
      } else if (isUnpaid(inst.estado)) {
        const saldo = Number(inst.monto) + recargo;
        cuentasPorCobrar += saldo;
        capitalEnCalle += capitalLetra;
        utilidadPendiente += interesLetra;
        if (inst.estado === "atrasada") {
          enMora += saldo;
        }
      }
    }
  }

  const parPct =
    cuentasPorCobrar > 0
      ? money((enMora / cuentasPorCobrar) * 100)
      : 0;

  return {
    /** @deprecated usar desembolsadoHistorico; se mantiene por compatibilidad */
    capital: money(desembolsadoHistorico),
    desembolsadoHistorico: money(desembolsadoHistorico),
    montosCobrados: money(montosCobrados),
    cuentasPorCobrar: money(cuentasPorCobrar),
    enMora: money(enMora),
    capitalEnCalle: money(capitalEnCalle),
    /** @deprecated alias de utilidadCobrada */
    interes: money(utilidadCobrada),
    utilidadCobrada: money(utilidadCobrada),
    utilidadPendiente: money(utilidadPendiente),
    parPct,
  };
}

export function parseFechaInicio(value?: string | Date | null): Date | undefined {
  if (!value) return undefined;
  if (value instanceof Date && !Number.isNaN(value.getTime())) {
    return startOfDay(value);
  }
  if (typeof value !== "string") return undefined;
  const iso = value.length >= 10 ? value.slice(0, 10) : value;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(iso)) return undefined;
  const d = new Date(`${iso}T00:00:00.000Z`);
  return Number.isNaN(d.getTime()) ? undefined : d;
}
