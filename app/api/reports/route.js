import { NextResponse } from "next/server";
import { z } from "zod";
import mongoose from "mongoose";
import { connectDB } from "@/lib/mongodb";
import Report from "@/models/Report";
import Vendor from "@/models/Vendor";
import { requireAdmin, requireUser } from "@/lib/apiAuth";

const reportInputSchema = z.object({
  vendorId: z.string().min(1, "ID warung wajib diisi"),
  alasan: z.enum(["lokasi_salah", "sudah_tutup", "menu_salah", "duplikat", "lainnya"]),
  deskripsi: z.string().max(500, "Deskripsi maksimal 500 karakter").optional().default(""),
});

export async function POST(request) {
  let body;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Body JSON tidak valid" }, { status: 400 });
  }

  const parsed = reportInputSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.issues[0]?.message || "Input laporan tidak valid" },
      { status: 400 }
    );
  }

  const { vendorId, alasan, deskripsi } = parsed.data;

  if (!mongoose.Types.ObjectId.isValid(vendorId)) {
    return NextResponse.json({ error: "ID warung tidak valid" }, { status: 400 });
  }

  try {
    await connectDB();

    const vendor = await Vendor.findById(vendorId).select("namaToko").lean();
    if (!vendor) {
      return NextResponse.json({ error: "Warung tidak ditemukan" }, { status: 404 });
    }

    const { user } = await requireUser(request).catch(() => ({ user: null }));

    const laporanBaru = await Report.create({
      vendorId,
      namaToko: vendor.namaToko,
      userId: user?.id || null,
      namaPelapor: user?.name || "Warga Komunitas",
      alasan,
      deskripsi: deskripsi.trim(),
      status: "pending",
    });

    return NextResponse.json(
      {
        message: "Laporan berhasil dikirim ke admin untuk ditindaklanjuti. Terima kasih atas kontribusi Anda!",
        data: laporanBaru,
      },
      { status: 201 }
    );
  } catch (error) {
    console.error("POST /api/reports gagal:", error);
    return NextResponse.json({ error: "Gagal mengirim laporan warung" }, { status: 500 });
  }
}

export async function GET(request) {
  const { response } = await requireAdmin(request);
  if (response) return response;

  try {
    await connectDB();

    const statusQuery = request.nextUrl.searchParams.get("status");
    const filter = {};
    if (statusQuery && ["pending", "selesai", "diabaikan"].includes(statusQuery)) {
      filter.status = statusQuery;
    }

    const reports = await Report.find(filter)
      .sort({ createdAt: -1 })
      .limit(100)
      .populate("vendorId", "namaToko alamatPatokan lokasi foto")
      .lean();

    return NextResponse.json({ data: reports });
  } catch (error) {
    console.error("GET /api/reports gagal:", error);
    return NextResponse.json({ error: "Gagal memuat daftar laporan" }, { status: 500 });
  }
}
