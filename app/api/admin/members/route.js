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

    // 1. Ambil daftar semua member
    const users = await User.find({})
      .select("name email role tipeAkun xp level createdAt emailVerified")
      .sort({ createdAt: -1 })
      .lean();

    // 2. Agregasi untuk menghitung member yang paling sering mengajukan toko/warung
    const kontributorStats = await Vendor.aggregate([
      {
        $match: {
          submittedBy: { $ne: null },
        },
      },
      {
        $group: {
          _id: "$submittedBy",
          totalPengajuan: { $sum: 1 },
          diterima: {
            $sum: { $cond: [{ $eq: ["$status", "approved"] }, 1, 0] },
          },
          menunggu: {
            $sum: { $cond: [{ $eq: ["$status", "pending"] }, 1, 0] },
          },
        },
      },
      {
        $sort: { totalPengajuan: -1 },
      },
      {
        $limit: 20,
      },
      {
        $lookup: {
          from: "users",
          localField: "_id",
          foreignField: "_id",
          as: "user",
        },
      },
      {
        $unwind: {
          path: "$user",
          preserveNullAndEmptyArrays: true,
        },
      },
      {
        $project: {
          userId: "$_id",
          name: { $ifNull: ["$user.name", "Warga Komunitas"] },
          email: { $ifNull: ["$user.email", "-"] },
          xp: { $ifNull: ["$user.xp", 0] },
          level: { $ifNull: ["$user.level", 1] },
          tipeAkun: { $ifNull: ["$user.tipeAkun", "pengguna"] },
          totalPengajuan: 1,
          diterima: 1,
          menunggu: 1,
        },
      },
    ]);

    // Map total pengajuan ke setiap user di list member
    const mapPengajuan = new Map();
    kontributorStats.forEach((k) => {
      if (k.userId) mapPengajuan.set(k.userId.toString(), k.totalPengajuan);
    });

    const membersDenganPengajuan = users.map((u) => ({
      ...u,
      _id: u._id.toString(),
      totalPengajuan: mapPengajuan.get(u._id.toString()) || 0,
    }));

    return NextResponse.json({
      data: {
        members: membersDenganPengajuan,
        topKontributor: kontributorStats,
      },
    });
  } catch (error) {
    console.error("GET /api/admin/members gagal:", error);
    return NextResponse.json({ error: "Gagal memuat data member" }, { status: 500 });
  }
}
