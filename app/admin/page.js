import Link from "next/link";
import { connectDB } from "@/lib/mongodb";
import User from "@/models/User";
import Vendor from "@/models/Vendor";

export default async function AdminOverviewPage() {
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

  const kartu = [
    { label: "Total warung", nilai: total, kelas: "text-zinc-900" },
    { label: "Menunggu kurasi", nilai: pending, kelas: "text-amber-600" },
    { label: "Tayang", nilai: approved, kelas: "text-emerald-700" },
    { label: "Ditolak", nilai: rejected, kelas: "text-red-600" },
  ];

  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-5">
      <h1 className="text-xl font-bold text-zinc-900">Overview</h1>
      <p className="mt-0.5 text-sm text-zinc-600">
        Ringkasan warung dan kontribusi penjual.
      </p>

      <div className="mt-4 grid grid-cols-2 gap-2">
        {kartu.map((item) => (
          <div
            key={item.label}
            className="rounded-xl border border-zinc-200 bg-white p-3"
          >
            <p className="text-xs text-zinc-500">{item.label}</p>
            <p className={`mt-1 text-2xl font-bold ${item.kelas}`}>
              {item.nilai}
            </p>
          </div>
        ))}
      </div>

      {pending > 0 ? (
        <Link
          href="/admin/kurasi"
          className="mt-4 block rounded-xl bg-emerald-600 px-4 py-3 text-center text-sm font-semibold text-white active:bg-emerald-700"
        >
          Kurasi {pending} warung sekarang
        </Link>
      ) : null}

      <section className="mt-6">
        <h2 className="mb-2 text-sm font-semibold text-zinc-800">
          Leaderboard XP
        </h2>

        {leaderboard.length === 0 ? (
          <p className="rounded-xl border border-zinc-200 bg-white p-4 text-sm text-zinc-600">
            Belum ada XP. Setujui warung untuk memberi XP ke penjual.
          </p>
        ) : (
          <ol className="overflow-hidden rounded-xl border border-zinc-200 bg-white">
            {leaderboard.map((user, index) => (
              <li
                key={user._id.toString()}
                className="flex items-center justify-between gap-3 border-b border-zinc-100 px-3 py-2.5 last:border-b-0"
              >
                <div className="flex min-w-0 items-center gap-2">
                  <span className="w-5 shrink-0 text-sm font-semibold text-zinc-400">
                    {index + 1}
                  </span>
                  <p className="line-clamp-1 text-sm font-medium text-zinc-900">
                    {user.name}
                  </p>
                </div>
                <div className="flex shrink-0 items-center gap-2 text-xs">
                  <span className="rounded-full bg-emerald-50 px-2 py-0.5 font-medium text-emerald-700">
                    Level {user.level}
                  </span>
                  <span className="text-zinc-500">{user.xp} XP</span>
                </div>
              </li>
            ))}
          </ol>
        )}
      </section>
    </main>
  );
}
