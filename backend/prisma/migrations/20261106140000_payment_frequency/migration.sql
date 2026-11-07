-- CreateEnum
CREATE TYPE "PaymentFrequency" AS ENUM ('semanal', 'quincenal', 'mensual');

-- AlterTable Quote
ALTER TABLE "Quote" ADD COLUMN "frecuencia" "PaymentFrequency" NOT NULL DEFAULT 'semanal';

-- AlterTable Loan
ALTER TABLE "Loan" ADD COLUMN "frecuencia" "PaymentFrequency" NOT NULL DEFAULT 'semanal';

-- AlterTable Settings
ALTER TABLE "Settings" ADD COLUMN "frecuenciaDefault" "PaymentFrequency" NOT NULL DEFAULT 'semanal';
ALTER TABLE "Settings" ADD COLUMN "quincenasDefault" INTEGER NOT NULL DEFAULT 2;
ALTER TABLE "Settings" ADD COLUMN "plazosQuincenales" INTEGER[] DEFAULT ARRAY[2, 3, 4, 6]::INTEGER[];
ALTER TABLE "Settings" ADD COLUMN "mesesDefault" INTEGER NOT NULL DEFAULT 1;
ALTER TABLE "Settings" ADD COLUMN "plazosMensuales" INTEGER[] DEFAULT ARRAY[1, 2, 3, 6, 12]::INTEGER[];

UPDATE "Settings" SET
  "plazosQuincenales" = ARRAY[2, 3, 4, 6]::INTEGER[],
  "plazosMensuales" = ARRAY[1, 2, 3, 6, 12]::INTEGER[]
WHERE id = 'default';
