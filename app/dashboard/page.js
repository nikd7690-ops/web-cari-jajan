import Link from "next/link";
import { redirect } from "next/navigation";
import { getSession } from "@/lib/auth";
import { connectDB } from "@/lib/mongodb";
import Vendor from "@/models/Vendor";
import LogoutButton from "@/components/LogoutButton";
import DeleteWarungButton from "@/components/DeleteWarungButton";

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
    <main className="mx-auto w-full max-w-2xl px-4 py-6 pb-24">
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
            Belum ada warung terdaftar. Tekan tombol + untuk menambah.
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
                  <div className="flex flex-wrap items-center justify-end gap-2">
                    <Link
                      href={`/warung/${vendor._id}`}
                      className="rounded-lg px-2 py-2 text-sm font-medium text-zinc-600 underline"
                    >
                      Lihat
                    </Link>
                    <Link
                      href={`/dashboard/edit/${vendor._id}`}
                      className="rounded-lg px-2 py-2 text-sm font-medium text-emerald-700 underline"
                    >
                      Edit
                    </Link>
                    <DeleteWarungButton
                      vendorId={vendor._id.toString()}
                      namaToko={vendor.namaToko}
                    />
                  </div>
                </div>
                <p className="mt-1 text-sm text-zinc-600">
                  {vendor.alamatPatokan}
                </p>
              </li>
            ))}
          </ul>
        )}
      </section>

      <Link
        href="/dashboard/tambah"
        aria-label="Tambah warung"
        className="fixed bottom-5 right-5 flex h-14 w-14 items-center justify-center rounded-full bg-emerald-600 text-3xl leading-none text-white shadow-lg active:bg-emerald-700"
      >
        +
      </Link>
    </main>
  );
}