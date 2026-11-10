import Link from "next/link";
import { redirect } from "next/navigation";
import { getSession } from "@/lib/auth";
import { connectDB } from "@/lib/mongodb";
import Vendor from "@/models/Vendor";
import LogoutButton from "@/components/LogoutButton";

export default async function DashboardPage() {
  const session = await getSession();

  if (!session?.user?.id) {
    redirect("/masuk");
  }

  await connectDB();

  const vendors = await Vendor.find({ owner: session.user.id })
    .sort({ createdAt: -1 })
    .lean();

  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-6">
      <header className="mb-4 flex items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-zinc-900">Dashboard</h1>
          <p className="text-sm text-zinc-600">
            Halo, {session.user.name || session.user.email}
          </p>
        </div>
        <LogoutButton />
      </header>

      <section>
        <h2 className="mb-2 text-sm font-semibold text-zinc-800">
          Warung milikmu
        </h2>

        {vendors.length === 0 ? (
          <p className="rounded-xl border border-zinc-200 bg-white p-4 text-sm text-zinc-600">
            Belum ada warung terdaftar.
          </p>
        ) : (
          <ul className="space-y-2">
            {vendors.map((vendor) => (
              <li
                key={vendor._id.toString()}
                className="rounded-xl border border-zinc-200 bg-white p-4"
              >
                <div className="flex items-center justify-between gap-2">
                  <div>
                    <p className="font-semibold text-zinc-900">
                      {vendor.namaToko}
                    </p>
                    <p className="text-xs text-zinc-500">
                      {vendor.kategori} - {vendor.jamBuka}-{vendor.jamTutup}
                    </p>
                  </div>
                  <Link
                    href={`/warung/${vendor._id}`}
                    className="text-sm font-medium text-emerald-700 underline"
                  >
                    Lihat
                  </Link>
                </div>
                <p className="mt-1 text-sm text-zinc-600">
                  {vendor.alamatPatokan}
                </p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}