import { lateFee, weeksLate } from "../src/lib/lateFee";
import { splitInstallments, tasaSemanalFraction, totalPagar } from "../src/lib/money";

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

console.log("Cálculos de crédito OK");
