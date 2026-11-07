import { computeCartera } from "../src/lib/cartera";
import { dueDateForInstallment } from "../src/lib/frequency";
import { lateFee, weeksLate } from "../src/lib/lateFee";
import { calendarYmd, money, splitInstallments, tasaSemanalFraction, totalPagar } from "../src/lib/money";
import { previewInstallments } from "../src/lib/preview";

function assert(cond: boolean, msg: string) {
  if (!cond) throw new Error(msg);
}

const total = totalPagar(1000, 10);
assert(total === 1100, `totalPagar expected 1100 got ${total}`);

const amounts = splitInstallments(1100, 4);
assert(amounts.length === 4, "4 letras");
assert(amounts.every((a) => a === 275), `letras ${amounts}`);

const tasa = tasaSemanalFraction(10, 4);
assert(Math.abs(tasa - 0.025) < 1e-10, `tasa ${tasa}`);

const due = new Date("2026-08-24T00:00:00");
const oneWeek = new Date("2026-08-31T00:00:00");
assert(weeksLate(due, oneWeek) === 1, `weeksLate 1 got ${weeksLate(due, oneWeek)}`);
assert(weeksLate(due, due) === 0, "el día de vencimiento no genera recargo");
assert(
  weeksLate(new Date("2026-08-31T00:00:00.000Z"), new Date("2026-08-31T23:30:00-05:00")) === 0,
  "vence hoy (DATE UTC vs Lima) no genera recargo",
);

const fee = lateFee(due, oneWeek);
assert(fee === 10, `recargo esperado 10 got ${fee}`);

const twoWeeks = new Date("2026-09-07T00:00:00");
const fee2 = lateFee(due, twoWeeks);
assert(fee2 === 20, `recargo 2 semanas esperado 20 got ${fee2}`);

// Préstamo 1000 / 10% / 4 sem → total 1100, letras 275, capital/letra 250
const cartera = computeCartera([
  {
    capital: 1000,
    semanas: 4,
    totalPagar: 1100,
    installments: [
      { nro: 1, monto: 275, recargoAcumulado: 0, montoPagado: 275, estado: "pagada" },
      { nro: 2, monto: 275, recargoAcumulado: 10, montoPagado: 0, estado: "atrasada" },
      { nro: 3, monto: 275, recargoAcumulado: 0, montoPagado: 0, estado: "pendiente" },
      { nro: 4, monto: 275, recargoAcumulado: 0, montoPagado: 0, estado: "pendiente" },
    ],
  },
]);
assert(cartera.desembolsadoHistorico === 1000, `desembolsado ${cartera.desembolsadoHistorico}`);
assert(cartera.montosCobrados === 275, `cobrado ${cartera.montosCobrados}`);
assert(cartera.cuentasPorCobrar === 835, `por cobrar ${cartera.cuentasPorCobrar}`);
assert(cartera.enMora === 285, `en mora ${cartera.enMora}`);
assert(cartera.capitalEnCalle === 750, `capital calle ${cartera.capitalEnCalle}`);
assert(cartera.utilidadCobrada === 25, `utilidad cobrada ${cartera.utilidadCobrada}`);
assert(cartera.parPct === money((285 / 835) * 100), `par ${cartera.parPct}`);

const start = new Date("2026-01-15T00:00:00.000Z");
assert(calendarYmd(dueDateForInstallment(start, 1, "semanal")) === "2026-01-22", "semanal +7");
assert(calendarYmd(dueDateForInstallment(start, 1, "quincenal")) === "2026-01-29", "quincenal +14");
assert(calendarYmd(dueDateForInstallment(start, 1, "mensual")) === "2026-02-15", "mensual +1m");
assert(calendarYmd(dueDateForInstallment(new Date("2026-01-31T00:00:00.000Z"), 1, "mensual")) === "2026-02-28", "mensual clamp");

const prevQ = previewInstallments(1000, 10, 2, start, "quincenal");
assert(prevQ.letras.length === 2, "2 quincenas");
assert(prevQ.letras[0].fechaVencimiento === "2026-01-29", `1ª quincena ${prevQ.letras[0].fechaVencimiento}`);
assert(prevQ.letras[1].fechaVencimiento === "2026-02-12", `2ª quincena ${prevQ.letras[1].fechaVencimiento}`);
assert(prevQ.totalPagar === 1100, "total quincenal flat igual");

const prevM = previewInstallments(1000, 10, 3, start, "mensual");
assert(prevM.letras.map((l) => l.fechaVencimiento).join(",") === "2026-02-15,2026-03-15,2026-04-15", "mensual fechas");

console.log("Cálculos de crédito OK");
