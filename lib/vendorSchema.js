import { z } from "zod";
import { normalisasiNomorWA } from "@/lib/keranjang";

const jamRegex = /^([01]\d|2[0-3]):[0-5]\d$/;

const menuSchema = z.object({
  namaMenu: z.string().trim().min(1, "Nama menu wajib diisi"),
  harga: z.coerce.number().min(0, "Harga tidak boleh negatif"),
  tersedia: z.boolean().optional().default(true),
});

const baseFields = {
  namaToko: z.string().trim().min(1, "Nama toko wajib diisi"),
  kategori: z.enum(["warung", "pkl", "rumah_makan", "keliling"]),
  lat: z.coerce.number().min(-90, "lat minimal -90").max(90, "lat maksimal 90"),
  lng: z.coerce
    .number()
    .min(-180, "lng minimal -180")
    .max(180, "lng maksimal 180"),
  alamatPatokan: z.string().trim().min(1, "Alamat atau patokan wajib diisi"),
  jamBuka: z.string().regex(jamRegex, "Format jamBuka harus HH:mm"),
  jamTutup: z.string().regex(jamRegex, "Format jamTutup harus HH:mm"),
  nomorWA: z
    .string()
    .trim()
    .optional()
    .default("")
    .transform((val) => (val ? normalisasiNomorWA(val) : ""))
    .refine((val) => !val || /^62\d{8,13}$/.test(val), "Nomor WA tidak valid"),
  layananAntar: z.boolean().optional().default(false),
  foto: z
    .string()
    .trim()
    .url("URL foto tidak valid")
    .or(z.literal(""))
    .optional()
    .default(""),
  menus: z.array(menuSchema).optional().default([]),
  userLat: z.coerce.number().min(-90).max(90).optional(),
  userLng: z.coerce.number().min(-180).max(180).optional(),
};

export const vendorInputSchema = z.object(baseFields);

export const vendorUpdateSchema = z.object({
  namaToko: baseFields.namaToko.optional(),
  kategori: baseFields.kategori.optional(),
  lat: baseFields.lat.optional(),
  lng: baseFields.lng.optional(),
  alamatPatokan: baseFields.alamatPatokan.optional(),
  jamBuka: baseFields.jamBuka.optional(),
  jamTutup: baseFields.jamTutup.optional(),
  nomorWA: baseFields.nomorWA.optional(),
  layananAntar: z.boolean().optional(),
  foto: z
    .string()
    .trim()
    .url("URL foto tidak valid")
    .or(z.literal(""))
    .optional(),
  menus: z.array(menuSchema).optional(),
});