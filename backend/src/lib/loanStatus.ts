import { InstallmentStatus, LoanStatus } from "@prisma/client";
import { prisma } from "./prisma";
import { lateFee } from "./lateFee";
import { toNumber } from "./money";

const UNPAID: InstallmentStatus[] = ["pendiente", "atrasada"];

export async function refreshOverdue(loanId?: string): Promise<void> {
  const today = new Date();

  const installments = await prisma.installment.findMany({
    where: {
      estado: { in: UNPAID },
      ...(loanId ? { loanId } : {}),
    },
  });

  for (const inst of installments) {
    const due = new Date(inst.fechaVencimiento);
    const recargo = lateFee(due, today);
    const overdue = recargo > 0;
    const nextStatus: InstallmentStatus = overdue ? "atrasada" : "pendiente";

    if (
      nextStatus !== inst.estado ||
      toNumber(inst.recargoAcumulado) !== recargo
    ) {
      await prisma.installment.update({
        where: { id: inst.id },
        data: { estado: nextStatus, recargoAcumulado: recargo },
      });
    }
  }

  const loanIds = loanId
    ? [loanId]
    : [...new Set(installments.map((i) => i.loanId))];

  const extraIds = loanId
    ? []
    : (
        await prisma.loan.findMany({
          where: { estado: { not: "pagado" } },
          select: { id: true },
        })
      ).map((l) => l.id);

  const allIds = [...new Set([...loanIds, ...extraIds])];
  for (const id of allIds) {
    await refreshLoanStatus(id);
  }
}

export async function refreshLoanStatus(loanId: string): Promise<LoanStatus> {
  const installments = await prisma.installment.findMany({
    where: { loanId },
  });

  const allPaid = installments.every(
    (i) => i.estado === "pagada" || i.estado === "pagada_con_atraso",
  );
  const anyOverdue = installments.some((i) => i.estado === "atrasada");

  let estado: LoanStatus;
  if (allPaid) estado = "pagado";
  else if (anyOverdue) estado = "atrasado";
  else estado = "al_dia";

  await prisma.loan.update({
    where: { id: loanId },
    data: { estado },
  });

  return estado;
}
