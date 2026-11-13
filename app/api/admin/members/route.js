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

    // 2. Ambil agregasi statistik pengajuan warung secara aman tanpa $lookup kompleks
    let rawCounts = [];
    try {
      rawCounts = await Vendor.aggregate([
        { $match: { submittedBy: { $exists: true, $ne: null } } },
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
        { $sort: { totalPengajuan: -1 } },
        { $limit: 20 },
      ]);
    } catch (aggErr) {
      console.warn("Agregasi kontributor pengajuan warung gagal, fallback empty:", aggErr);
    }

    // Buat map user dari data users
    const userMap = new Map();
    users.forEach((u) => {
      userMap.set(u._id.toString(), u);
    });

    // Map total pengajuan ke setiap user di list member
    const mapPengajuan = new Map();
    rawCounts.forEach((k) => {
      if (k._id) {
        mapPengajuan.set(k._id.toString(), k.totalPengajuan);
      }
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

    const topKontributorFormatted = rawCounts.map((k) => {
      const idStr = k._id ? k._id.toString() : "";
      const u = userMap.get(idStr);
      return {
        userId: idStr,
        name: u?.name || "Warga Komunitas",
        email: u?.email || "-",
        xp: Number(u?.xp) || 0,
        level: Number(u?.level) || 1,
        totalPengajuan: Number(k.totalPengajuan) || 0,
        diterima: Number(k.diterima) || 0,
        menunggu: Number(k.menunggu) || 0,
      };
    });

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
