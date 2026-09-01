import { money, splitInstallments } from "./money";

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

export function computeCartera(loans: LoanLike[]) {
  let capital = 0;
  let montosCobrados = 0;
  let cuentasPorCobrar = 0;
  let interes = 0;

  for (const loan of loans) {
    const cap = Number(loan.capital);
    capital += cap;
    const shares = splitInstallments(cap, loan.semanas);

    for (const inst of loan.installments) {
      const paid = inst.estado === "pagada" || inst.estado === "pagada_con_atraso";
      if (paid) {
        montosCobrados += Number(inst.montoPagado);
        const capitalLetra = shares[inst.nro - 1] ?? 0;
        interes += Number(inst.monto) - capitalLetra + Number(inst.recargoAcumulado);
      } else {
        cuentasPorCobrar += Number(inst.monto) + Number(inst.recargoAcumulado);
      }
    }
  }

  return {
    capital: money(capital),
    montosCobrados: money(montosCobrados),
    cuentasPorCobrar: money(cuentasPorCobrar),
    interes: money(interes),
  };
}

export function parseFechaInicio(value?: string | Date | null): Date | undefined {
  if (!value) return undefined;
  if (value instanceof Date && !Number.isNaN(value.getTime())) return value;
  if (typeof value !== "string") return undefined;
  const iso = value.length >= 10 ? value.slice(0, 10) : value;
  const d = new Date(`${iso}T00:00:00`);
  return Number.isNaN(d.getTime()) ? undefined : d;
}
