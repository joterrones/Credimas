import { backendRoot } from "../loadEnv";
import multer from "multer";
import path from "path";
import fs from "fs";

const uploadDir = path.isAbsolute(process.env.UPLOAD_DIR ?? "")
  ? String(process.env.UPLOAD_DIR)
  : path.resolve(backendRoot, process.env.UPLOAD_DIR ?? "uploads");
fs.mkdirSync(uploadDir, { recursive: true });

const storage = multer.diskStorage({
  destination: (_req, _file, cb) => cb(null, uploadDir),
  filename: (_req, file, cb) => {
    const ext = path.extname(file.originalname) || ".jpg";
    cb(null, `${Date.now()}-${Math.random().toString(36).slice(2, 8)}${ext}`);
  },
});

export const upload = multer({
  storage,
  limits: { fileSize: 8 * 1024 * 1024 },
  fileFilter: (_req, file, cb) => {
    const ok = /^(image\/|application\/pdf)/.test(file.mimetype);
    if (!ok) cb(new Error("Solo se permiten imágenes o PDF"));
    else cb(null, true);
  },
});

export function uploadedUrl(file?: Express.Multer.File) {
  return file ? `/uploads/${file.filename}` : undefined;
}
