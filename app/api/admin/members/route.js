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
      .select("name email role tipeAkun xp level createdAt emailVerified isBanned")
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
      const idStr = k.userId ? k.userId.toString() : (k._id ? k._id.toString() : "");
      if (idStr) mapPengajuan.set(idStr, k.totalPengajuan);
    });

    const membersDenganPengajuan = users.map((u) => {
      const idStr = u._id.toString();
      return {
        _id: idStr,
        id: idStr,
        name: u.name || "Member Cari Jajan",
        email: u.email || "",
        role: u.role || "user",
        tipeAkun: u.tipeAkun || "pengguna",
        xp: Number(u.xp) || 0,
        level: Number(u.level) || 1,
        emailVerified: Boolean(u.emailVerified),
        isBanned: Boolean(u.isBanned),
        totalPengajuan: mapPengajuan.get(idStr) || 0,
        createdAt: u.createdAt ? new Date(u.createdAt).toISOString() : null,
      };
    });

    const topKontributorFormatted = kontributorStats.map((k) => ({
      userId: k.userId ? k.userId.toString() : (k._id ? k._id.toString() : ""),
      name: k.name || "Warga Komunitas",
      email: k.email || "-",
      xp: Number(k.xp) || 0,
      level: Number(k.level) || 1,
      totalPengajuan: Number(k.totalPengajuan) || 0,
      diterima: Number(k.diterima) || 0,
      menunggu: Number(k.menunggu) || 0,
    }));

    return NextResponse.json({
      data: {
        members: membersDenganPengajuan,
        topKontributor: topKontributorFormatted,
      },
    });
  } catch (error) {
    console.error("GET /api/admin/members gagal:", error);
    return NextResponse.json({ error: "Gagal memuat data member" }, { status: 500 });
  }
}
