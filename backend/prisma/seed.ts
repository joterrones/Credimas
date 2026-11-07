import { PrismaClient } from "@prisma/client";
import bcrypt from "bcryptjs";

const prisma = new PrismaClient();

async function main() {
  const password = await bcrypt.hash("admin123", 10);

  await prisma.user.upsert({
    where: { email: "admin@credimax.pe" },
    update: {},
    create: {
      email: "admin@credimax.pe",
      password,
      name: "Administrador",
    },
  });

  await prisma.settings.upsert({
    where: { id: "default" },
    update: {},
    create: {
      id: "default",
      interesPctDefault: 10,
      frecuenciaDefault: "semanal",
      semanasDefault: 4,
      plazosPermitidos: [4, 6, 8, 10, 12, 16],
      quincenasDefault: 2,
      plazosQuincenales: [2, 3, 4, 6],
      mesesDefault: 1,
      plazosMensuales: [1, 2, 3, 6, 12],
      moneda: "PEN",
    },
  });

  console.log("Seed OK — admin@credimax.pe / admin123");
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
