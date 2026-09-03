/**
 * Flujo E2E contra la API local: login → cliente → presupuesto → préstamo → pago → atraso.
 */
import "dotenv/config";
import fs from "fs";
import path from "path";
import { prisma } from "../src/lib/prisma";
import { lateFee } from "../src/lib/lateFee";

const BASE = process.env.API_URL ?? "http://localhost:3700";

async function json(res: Response) {
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) throw new Error(`${res.status} ${text}`);
  return data;
}

async function main() {
  const login = await json(
    await fetch(`${BASE}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: "admin@credimax.pe", password: "admin123" }),
    }),
  );
  const auth = { Authorization: `Bearer ${login.token}`, "Content-Type": "application/json" };
  const suffix = Date.now().toString().slice(-8);

  const client = await json(
    await fetch(`${BASE}/clients`, {
      method: "POST",
      headers: auth,
      body: JSON.stringify({
        nombre: "Ana Pérez",
        direccion: "Av. Arequipa 123, Lima",
        telefono: "999888777",
        tipoDocumento: "DNI",
        nroDocumento: suffix.padStart(8, "1").slice(0, 8),
      }),
    }),
  );

  const quote = await json(
    await fetch(`${BASE}/quotes`, {
      method: "POST",
      headers: auth,
      body: JSON.stringify({
        clientId: client.id,
        capital: 1000,
        interesPct: 10,
        semanas: 4,
        notas: "Prueba E2E",
      }),
    }),
  );
  if (quote.totalPagar !== 1100) throw new Error(`totalPagar ${quote.totalPagar}`);

  const loan = await json(
    await fetch(`${BASE}/quotes/${quote.id}/convert`, { method: "POST", headers: auth }),
  );
  if (loan.installments.length !== 4) throw new Error("se esperaban 4 letras");
  if (Number(loan.installments[0].monto) !== 275) throw new Error("letra != 275");

  const jpeg = Buffer.from(
    "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////2wBDAf//////////////////////////////////////////////////////////////////////////////////////wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAb/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIQAxAAAAGfAD//2Q==",
    "base64",
  );
  const tmp = path.join(process.cwd(), "uploads", "e2e.jpg");
  fs.writeFileSync(tmp, jpeg);

  const form = new FormData();
  form.append("comprobante", new Blob([jpeg], { type: "image/jpeg" }), "voucher.jpg");
  form.append("notas", "Pago de prueba");

  const paid = await json(
    await fetch(`${BASE}/loans/${loan.id}/installments/${loan.installments[0].id}/pay`, {
      method: "POST",
      headers: { Authorization: `Bearer ${login.token}` },
      body: form,
    }),
  );
  if (paid.payment.recargo !== 0) throw new Error("la primera letra no debía tener recargo");

  const due = new Date();
  due.setDate(due.getDate() - 8);
  due.setHours(0, 0, 0, 0);
  await prisma.installment.update({
    where: { id: loan.installments[1].id },
    data: { fechaVencimiento: due },
  });

  const refreshed = await json(await fetch(`${BASE}/loans/${loan.id}`, { headers: auth }));
  const late = refreshed.installments.find((i: { nro: number }) => i.nro === 2);
  const expected = lateFee(new Date(late.fechaVencimiento));
  if (Number(late.recargoAcumulado) !== expected) {
    throw new Error(`recargo ${late.recargoAcumulado} != ${expected}`);
  }
  if (late.estado !== "atrasada") throw new Error("letra 2 debía estar atrasada");

  const dash = await json(await fetch(`${BASE}/reports/dashboard`, { headers: auth }));
  if (dash.kpis.deudasPorCobrar <= 0) throw new Error("deudas por cobrar vacío");
  if (dash.kpis.cobrosRetrasados <= 0) throw new Error("cobros retrasados vacío");

  const settings = await json(await fetch(`${BASE}/settings`, { headers: auth }));
  if (settings.interesPctDefault !== 10) throw new Error("interés default");

  console.log("Flujo E2E OK", {
    cliente: client.codigo,
    prestamo: loan.id,
    recargoLetra2: late.recargoAcumulado,
    kpis: dash.kpis,
  });
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
