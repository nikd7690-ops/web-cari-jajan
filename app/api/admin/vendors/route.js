import { NextResponse } from "next/server";
import { z } from "zod";
import { connectDB } from "@/lib/mongodb";
import Vendor from "@/models/Vendor";
import { requireAdmin } from "@/lib/apiAuth";

const statusSchema = z.enum(["pending", "approved", "rejected", "semua"]);

export async function GET(request) {
  const { response } = await requireAdmin(request);
  if (response) return response;

  const status = request.nextUrl.searchParams.get("status") ?? "semua";
  const parsed = statusSchema.safeParse(status);

  if (!parsed.success) {
    return NextResponse.json(
      { error: "Status warung tidak valid" },
      { status: 400 }
    );
  }

  try {
    await connectDB();

    const filter = parsed.data === "semua" ? {} : { status: parsed.data };

    const vendors = await Vendor.find(filter)
      .sort({ createdAt: -1 })
      .limit(100)
      .populate({ path: "submittedBy", select: "name email" })
      .lean();

    return NextResponse.json({ data: vendors });
  } catch (error) {
    console.error("GET /api/admin/vendors gagal:", error);
    return NextResponse.json(
      { error: "Gagal mengambil daftar warung" },
      { status: 500 }
    );
  }
}
