import { NextResponse } from "next/server";
import { z } from "zod";
import { requireUser } from "@/lib/apiAuth";
import { cekRateLimit } from "@/lib/rateLimit";
import { bacaMenuDariGambar } from "@/lib/gemini";

const TIPE_DIIZINKAN = ["image/jpeg", "image/png", "image/webp"];
const MAX_BYTES = 2 * 1024 * 1024;

const balasanSchema = z.object({
  items: z
    .array(
      z.object({
        namaMenu: z.string().optional(),
        harga: z.number().nullable().optional(),
        yakin: z.boolean().optional(),
      })
    )
    .optional(),
});

function ambilJson(teks) {
  const bersih = String(teks)
    .trim()
    .replace(/^```json/i, "")
    .replace(/^```/, "")
    .replace(/```$/, "")
    .trim();

  try {
    return JSON.parse(bersih);
  } catch {
    const awal = bersih.indexOf("{");
    const akhir = bersih.lastIndexOf("}");
    if (awal !== -1 && akhir > awal) {
      try {
        return JSON.parse(bersih.slice(awal, akhir + 1));
      } catch {
        return null;
      }
    }
    return null;
  }
}

export async function POST(request) {
  const { user, response } = await requireUser();
  if (response) return response;

  const batas = cekRateLimit(`menu-foto:${user.id}`, 5, 10 * 60 * 1000);

  if (!batas.ok) {
    return NextResponse.json(
      {
        error: `Terlalu banyak permintaan. Coba lagi dalam ${batas.sisaDetik} detik.`,
      },
      { status: 429 }
    );
  }

  let form;

  try {
    form = await request.formData();
  } catch {
    return NextResponse.json({ error: "Form tidak valid" }, { status: 400 });
  }

  const file = form.get("file");

  if (!file || typeof file === "string") {
    return NextResponse.json(
      { error: "Foto menu wajib diunggah" },
      { status: 400 }
    );
  }

  if (!TIPE_DIIZINKAN.includes(file.type)) {
    return NextResponse.json(
      { error: "Tipe file harus JPEG, PNG, atau WebP" },
      { status: 400 }
    );
  }

  if (file.size > MAX_BYTES) {
    return NextResponse.json(
      { error: "Ukuran file maksimal 2 MB" },
      { status: 400 }
    );
  }

  let teksBalasan;

  try {
    const base64 = Buffer.from(await file.arrayBuffer()).toString("base64");
    teksBalasan = await bacaMenuDariGambar({
      base64,
      mimeType: file.type,
    });
  } catch (error) {
    console.error("POST /api/menu-dari-foto gagal:", error);
    const pesan = error?.name === "AbortError"
      ? "AI terlalu lama merespons. Coba lagi atau isi menu manual."
      : "AI sedang tidak bisa membaca foto. Isi menu manual saja.";
    return NextResponse.json({ error: pesan }, { status: 502 });
  }

  const json = ambilJson(teksBalasan);
  const hasil = balasanSchema.safeParse(json);

  if (!hasil.success) {
    return NextResponse.json(
      {
        error:
          "AI tidak mengembalikan data yang bisa dibaca. Isi menu manual saja.",
      },
      { status: 422 }
    );
  }

  const items = (hasil.data.items ?? [])
    .map((item) => {
      const namaMenu = String(item.namaMenu ?? "").trim();
      const hargaValid =
        typeof item.harga === "number" && Number.isFinite(item.harga)
          ? Math.round(item.harga)
          : null;

      return {
        namaMenu,
        harga: hargaValid,
        yakin: hargaValid !== null && item.yakin !== false,
      };
    })
    .filter((item) => item.namaMenu !== "");

  if (items.length === 0) {
    return NextResponse.json(
      { error: "AI tidak menemukan menu di foto. Isi menu manual saja." },
      { status: 422 }
    );
  }

  return NextResponse.json({ items });
}