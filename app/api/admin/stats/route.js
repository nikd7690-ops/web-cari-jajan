import { NextResponse } from "next/server";
import { connectDB } from "@/lib/mongodb";
import User from "@/models/User";
import Vendor from "@/models/Vendor";
import { requireAdmin } from "@/lib/apiAuth";

export async function GET(request) {
  const { response } = await requireAdmin(request);
  if (response) return response;

  try {
    await connectDB();

    const [total, pending, approved, rejected, leaderboard] = await Promise.all([
      Vendor.countDocuments({}),
      Vendor.countDocuments({ status: "pending" }),
      Vendor.countDocuments({ status: "approved" }),
      Vendor.countDocuments({ status: "rejected" }),
      User.find({ xp: { $gt: 0 } })
        .sort({ xp: -1, name: 1 })
        .limit(10)
        .select("name xp level")
        .lean(),
    ]);

    return NextResponse.json({
      data: { total, pending, approved, rejected, leaderboard },
    });
  } catch (error) {
    console.error("GET /api/admin/stats gagal:", error);
    return NextResponse.json(
      { error: "Gagal mengambil statistik admin" },
      { status: 500 }
    );
  }
}
