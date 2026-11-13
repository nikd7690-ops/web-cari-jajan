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

    const formatted = vendors.map((v) => {
      let namaPengaju = "Warga Komunitas";
      let submittedById = null;

      if (v.submittedBy && typeof v.submittedBy === "object") {
        namaPengaju = v.submittedBy.name || v.submittedBy.email || "Warga Komunitas";
        submittedById = v.submittedBy._id ? v.submittedBy._id.toString() : null;
      } else if (v.submittedBy) {
        submittedById = v.submittedBy.toString();
      }

      return {
        ...v,
        _id: v._id.toString(),
        owner: v.owner ? v.owner.toString() : null,
        submittedBy: submittedById,
        namaKontributor: v.namaKontributor || namaPengaju,
        createdAt: v.createdAt ? new Date(v.createdAt).toISOString() : null,
      };
    });

    return NextResponse.json({ data: formatted });
  } catch (error) {
    console.error("GET /api/admin/vendors gagal:", error);
    return NextResponse.json(
      { error: "Gagal mengambil daftar warung" },
      { status: 500 }
    );
  }
}
