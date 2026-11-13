import { NextResponse } from "next/server";
import mongoose from "mongoose";
import { z } from "zod";
import { connectDB } from "@/lib/mongodb";
import Report from "@/models/Report";
import { requireAdmin } from "@/lib/apiAuth";

const updateSchema = z.object({
  status: z.enum(["pending", "selesai", "diabaikan"]).optional(),
  catatanAdmin: z.string().max(500).optional(),
});

export async function PATCH(request, { params }) {
  const { response } = await requireAdmin(request);
  if (response) return response;

  const { id } = await params;
  if (!mongoose.Types.ObjectId.isValid(id)) {
    return NextResponse.json({ error: "ID laporan tidak valid" }, { status: 400 });
  }

  let body;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Body JSON tidak valid" }, { status: 400 });
  }

  const parsed = updateSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.issues[0]?.message || "Input tidak valid" },
      { status: 400 }
    );
  }

  try {
    await connectDB();

    const report = await Report.findByIdAndUpdate(
      id,
      { $set: parsed.data },
      { new: true }
    );

    if (!report) {
      return NextResponse.json({ error: "Laporan tidak ditemukan" }, { status: 404 });
    }

    return NextResponse.json({ message: "Status laporan berhasil diperbarui", data: report });
  } catch (error) {
    console.error("PATCH /api/reports/[id] gagal:", error);
    return NextResponse.json({ error: "Gagal memperbarui status laporan" }, { status: 500 });
  }
}

export async function DELETE(request, { params }) {
  const { response } = await requireAdmin(request);
  if (response) return response;

  const { id } = await params;
  if (!mongoose.Types.ObjectId.isValid(id)) {
    return NextResponse.json({ error: "ID laporan tidak valid" }, { status: 400 });
  }

  try {
    await connectDB();
    await Report.findByIdAndDelete(id);
    return NextResponse.json({ message: "Laporan berhasil dihapus" });
  } catch (error) {
    console.error("DELETE /api/reports/[id] gagal:", error);
    return NextResponse.json({ error: "Gagal menghapus laporan" }, { status: 500 });
  }
}
