-- AlterTable
ALTER TABLE "Loan" ADD COLUMN "imagenUrl" TEXT;

-- AlterTable
ALTER TABLE "Payment" ALTER COLUMN "comprobanteUrl" DROP NOT NULL;
