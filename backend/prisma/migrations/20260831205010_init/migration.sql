-- CreateEnum
CREATE TYPE "DocumentType" AS ENUM ('DNI', 'CE');

-- CreateEnum
CREATE TYPE "ClientStatus" AS ENUM ('activo', 'inactivo');

-- CreateEnum
CREATE TYPE "QuoteStatus" AS ENUM ('borrador', 'aprobado', 'rechazado', 'convertido');

-- CreateEnum
CREATE TYPE "LoanStatus" AS ENUM ('activo', 'al_dia', 'atrasado', 'pagado');

-- CreateEnum
CREATE TYPE "InstallmentStatus" AS ENUM ('pendiente', 'atrasada', 'pagada', 'pagada_con_atraso');

-- CreateTable
CREATE TABLE "User" (
    "id" TEXT NOT NULL,
    "email" TEXT NOT NULL,
    "password" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "User_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Client" (
    "id" TEXT NOT NULL,
    "codigo" SERIAL NOT NULL,
    "nombre" TEXT NOT NULL,
    "direccion" TEXT NOT NULL,
    "telefono" TEXT NOT NULL,
    "tipoDocumento" "DocumentType" NOT NULL,
    "nroDocumento" TEXT NOT NULL,
    "estado" "ClientStatus" NOT NULL DEFAULT 'activo',
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Client_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Quote" (
    "id" TEXT NOT NULL,
    "clientId" TEXT NOT NULL,
    "capital" DECIMAL(12,2) NOT NULL,
    "interesPct" DECIMAL(6,2) NOT NULL,
    "semanas" INTEGER NOT NULL,
    "notas" TEXT,
    "estado" "QuoteStatus" NOT NULL DEFAULT 'borrador',
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Quote_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Loan" (
    "id" TEXT NOT NULL,
    "clientId" TEXT NOT NULL,
    "quoteId" TEXT,
    "capital" DECIMAL(12,2) NOT NULL,
    "interesPct" DECIMAL(6,2) NOT NULL,
    "semanas" INTEGER NOT NULL,
    "totalPagar" DECIMAL(12,2) NOT NULL,
    "tasaSemanal" DECIMAL(10,8) NOT NULL,
    "fechaInicio" DATE NOT NULL,
    "estado" "LoanStatus" NOT NULL DEFAULT 'activo',
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Loan_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Installment" (
    "id" TEXT NOT NULL,
    "loanId" TEXT NOT NULL,
    "nro" INTEGER NOT NULL,
    "fechaVencimiento" DATE NOT NULL,
    "monto" DECIMAL(12,2) NOT NULL,
    "recargoAcumulado" DECIMAL(12,2) NOT NULL DEFAULT 0,
    "montoPagado" DECIMAL(12,2) NOT NULL DEFAULT 0,
    "estado" "InstallmentStatus" NOT NULL DEFAULT 'pendiente',
    "pagadaEn" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Installment_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Payment" (
    "id" TEXT NOT NULL,
    "installmentId" TEXT NOT NULL,
    "monto" DECIMAL(12,2) NOT NULL,
    "recargo" DECIMAL(12,2) NOT NULL,
    "fecha" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "comprobanteUrl" TEXT NOT NULL,
    "notas" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "Payment_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Settings" (
    "id" TEXT NOT NULL DEFAULT 'default',
    "interesPctDefault" DECIMAL(6,2) NOT NULL,
    "plazosPermitidos" INTEGER[],
    "moneda" TEXT NOT NULL DEFAULT 'PEN',
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Settings_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "User_email_key" ON "User"("email");

-- CreateIndex
CREATE UNIQUE INDEX "Client_codigo_key" ON "Client"("codigo");

-- CreateIndex
CREATE UNIQUE INDEX "Client_nroDocumento_key" ON "Client"("nroDocumento");

-- CreateIndex
CREATE INDEX "Quote_clientId_idx" ON "Quote"("clientId");

-- CreateIndex
CREATE INDEX "Quote_estado_idx" ON "Quote"("estado");

-- CreateIndex
CREATE UNIQUE INDEX "Loan_quoteId_key" ON "Loan"("quoteId");

-- CreateIndex
CREATE INDEX "Loan_clientId_idx" ON "Loan"("clientId");

-- CreateIndex
CREATE INDEX "Loan_estado_idx" ON "Loan"("estado");

-- CreateIndex
CREATE INDEX "Installment_estado_idx" ON "Installment"("estado");

-- CreateIndex
CREATE INDEX "Installment_fechaVencimiento_idx" ON "Installment"("fechaVencimiento");

-- CreateIndex
CREATE UNIQUE INDEX "Installment_loanId_nro_key" ON "Installment"("loanId", "nro");

-- AddForeignKey
ALTER TABLE "Quote" ADD CONSTRAINT "Quote_clientId_fkey" FOREIGN KEY ("clientId") REFERENCES "Client"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Loan" ADD CONSTRAINT "Loan_clientId_fkey" FOREIGN KEY ("clientId") REFERENCES "Client"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Loan" ADD CONSTRAINT "Loan_quoteId_fkey" FOREIGN KEY ("quoteId") REFERENCES "Quote"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Installment" ADD CONSTRAINT "Installment_loanId_fkey" FOREIGN KEY ("loanId") REFERENCES "Loan"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Payment" ADD CONSTRAINT "Payment_installmentId_fkey" FOREIGN KEY ("installmentId") REFERENCES "Installment"("id") ON DELETE CASCADE ON UPDATE CASCADE;
