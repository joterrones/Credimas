import { Router } from "express";
import { z } from "zod";
import { prisma } from "../lib/prisma";
import { toJson } from "../lib/money";
import { asyncHandler, HttpError } from "../middleware/error";

export const clientsRouter = Router();

const documentSchema = z
  .object({
    tipoDocumento: z.enum(["DNI", "CE"]),
    nroDocumento: z.string().trim().min(1),
  })
  .superRefine((data, ctx) => {
    if (data.tipoDocumento === "DNI" && !/^\d{8}$/.test(data.nroDocumento)) {
      ctx.addIssue({
        code: "custom",
        path: ["nroDocumento"],
        message: "El DNI debe tener 8 dígitos",
      });
    }
    if (data.tipoDocumento === "CE" && !/^[A-Za-z0-9]{8,12}$/.test(data.nroDocumento)) {
      ctx.addIssue({
        code: "custom",
        path: ["nroDocumento"],
        message: "El CE debe tener entre 8 y 12 caracteres alfanuméricos",
      });
    }
  });

const createSchema = z
  .object({
    nombre: z.string().trim().min(2),
    direccion: z.string().trim().min(3),
    telefono: z.string().trim().min(6),
    tipoDocumento: z.enum(["DNI", "CE"]),
    nroDocumento: z.string().trim().min(1),
  })
  .and(documentSchema);

const updateSchema = z
  .object({
    nombre: z.string().trim().min(2).optional(),
    direccion: z.string().trim().min(3).optional(),
    telefono: z.string().trim().min(6).optional(),
    tipoDocumento: z.enum(["DNI", "CE"]).optional(),
    nroDocumento: z.string().trim().min(1).optional(),
    estado: z.enum(["activo", "inactivo"]).optional(),
  })
  .superRefine((data, ctx) => {
    if (data.tipoDocumento === "DNI" && data.nroDocumento && !/^\d{8}$/.test(data.nroDocumento)) {
      ctx.addIssue({
        code: "custom",
        path: ["nroDocumento"],
        message: "El DNI debe tener 8 dígitos",
      });
    }
  });

clientsRouter.get(
  "/",
  asyncHandler(async (req, res) => {
    const q = typeof req.query.q === "string" ? req.query.q.trim() : "";
    const where = q
      ? {
          OR: [
            { nombre: { contains: q, mode: "insensitive" as const } },
            { nroDocumento: { contains: q, mode: "insensitive" as const } },
            { telefono: { contains: q } },
            ...(/^\d+$/.test(q) ? [{ codigo: Number(q) }] : []),
          ],
        }
      : undefined;
    const clients = await prisma.client.findMany({
      where,
      orderBy: { codigo: "desc" },
    });
    res.json(toJson(clients));
  }),
);

clientsRouter.get(
  "/:id",
  asyncHandler(async (req, res) => {
    const client = await prisma.client.findUnique({ where: { id: String(req.params.id) } });
    if (!client) throw new HttpError(404, "Cliente no encontrado");
    res.json(toJson(client));
  }),
);

clientsRouter.post(
  "/",
  asyncHandler(async (req, res) => {
    const data = createSchema.parse(req.body);
    const exists = await prisma.client.findUnique({
      where: { nroDocumento: data.nroDocumento },
    });
    if (exists) throw new HttpError(409, "Ya existe un cliente con ese documento");
    const client = await prisma.client.create({ data });
    res.status(201).json(toJson(client));
  }),
);

clientsRouter.put(
  "/:id",
  asyncHandler(async (req, res) => {
    const data = updateSchema.parse(req.body);
    const client = await prisma.client.findUnique({ where: { id: String(req.params.id) } });
    if (!client) throw new HttpError(404, "Cliente no encontrado");
    if (data.nroDocumento && data.nroDocumento !== client.nroDocumento) {
      const exists = await prisma.client.findUnique({
        where: { nroDocumento: data.nroDocumento },
      });
      if (exists) throw new HttpError(409, "Ya existe un cliente con ese documento");
    }
    const updated = await prisma.client.update({
      where: { id: String(req.params.id) },
      data,
    });
    res.json(toJson(updated));
  }),
);
