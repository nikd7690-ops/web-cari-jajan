import { connectDB } from "@/lib/mongodb";
import User from "@/models/User";
import Vendor from "@/models/Vendor";
import AdminMemberList from "@/components/AdminMemberList";

export const metadata = {
  title: "Admin Member - Cari Jajan",
};

export default async function AdminMembersPage() {
  await connectDB();

  // 1. Top Kontributor yang paling sering mengajukan toko/warung
  const topSubmissions = await Vendor.aggregate([
    { $match: { submittedBy: { $exists: true, $ne: null } } },
    {
      $group: {
        _id: "$submittedBy",
        totalPengajuan: { $sum: 1 },
        approvedCount: {
          $sum: { $cond: [{ $eq: ["$status", "approved"] }, 1, 0] },
        },
      },
    },
    { $sort: { totalPengajuan: -1, approvedCount: -1 } },
    { $limit: 10 },
    {
      $lookup: {
        from: "users",
        localField: "_id",
        foreignField: "_id",
        as: "user",
      },
    },
    { $unwind: "$user" },
    {
      $project: {
        _id: 1,
        totalPengajuan: 1,
        approvedCount: 1,
        name: "$user.name",
        email: "$user.email",
        xp: "$user.xp",
        level: "$user.level",
        role: "$user.role",
        tipeAkun: "$user.tipeAkun",
      },
    },
  ]);

  // 2. Daftar semua member
  const members = await User.find({})
    .sort({ createdAt: -1 })
    .select("name email role tipeAkun xp level createdAt isBanned")
    .lean();

  const vendorCounts = await Vendor.aggregate([
    { $match: { submittedBy: { $exists: true, $ne: null } } },
    { $group: { _id: "$submittedBy", total: { $sum: 1 } } },
  ]);
  const vendorCountMap = new Map();
  for (const item of vendorCounts) {
    vendorCountMap.set(item._id.toString(), item.total);
  }

  const membersData = members.map((m) => ({
    _id: m._id.toString(),
    name: m.name || "",
    email: m.email || "",
    role: m.role || "user",
    tipeAkun: m.tipeAkun || "pengguna",
    xp: m.xp || 0,
    level: m.level || 1,
    isBanned: Boolean(m.isBanned),
    totalWarung: vendorCountMap.get(m._id.toString()) || 0,
  }));

  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-5">
      <h1 className="text-xl font-bold text-zinc-900">Member & Kontributor</h1>
      <p className="mt-0.5 text-sm text-zinc-600">
        Kelola dan pantau pengguna, mitra warung, moderasi akun, dan kontributor teraktif.
      </p>

      {/* TOP KONTRIBUTOR PENGAJU WARUNG */}
      <section className="mt-6">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-zinc-800">
            🏆 Top Kontributor Pengaju Warung
          </h2>
          <span className="text-xs text-zinc-500">Paling Sering Mengajukan</span>
        </div>

        {topSubmissions.length === 0 ? (
          <p className="mt-2 rounded-xl border border-zinc-200 bg-white p-4 text-xs text-zinc-500">
            Belum ada data pengajuan warung oleh member.
          </p>
        ) : (
          <div className="mt-3 space-y-2">
            {topSubmissions.map((top, idx) => (
              <div
                key={top._id.toString()}
                className="flex items-center justify-between gap-3 rounded-xl border border-amber-200 bg-amber-50/50 p-3"
              >
                <div className="flex items-center gap-2.5">
                  <span
                    className={`flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold ${
                      idx === 0
                        ? "bg-amber-400 text-amber-950"
                        : idx === 1
                          ? "bg-zinc-300 text-zinc-800"
                          : idx === 2
                            ? "bg-orange-300 text-orange-950"
                            : "bg-white text-zinc-700 border border-zinc-200"
                    }`}
                  >
                    {idx + 1}
                  </span>
                  <div>
                    <p className="text-sm font-semibold text-zinc-900">
                      {top.name || top.email}
                    </p>
                    <p className="text-xs text-zinc-500">
                      {top.email} • Level {top.level || 1} ({top.xp || 0} XP)
                    </p>
                  </div>
                </div>
                <div className="text-right">
                  <span className="inline-block rounded-lg bg-emerald-100 px-2 py-0.5 text-xs font-bold text-emerald-800">
                    {top.totalPengajuan} Warung
                  </span>
                  <p className="mt-0.5 text-[10px] text-zinc-500">
                    {top.approvedCount} tayang
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* SEMUA MEMBER & MODERASI */}
      <section className="mt-8">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-zinc-800">
            Daftar Semua Member & Moderasi ({members.length})
          </h2>
        </div>

        <AdminMemberList initialMembers={membersData} />
      </section>
    </main>
  );
}
