import Link from "next/link";
import Image from "next/image";
import { redirect } from "next/navigation";
import { getSession } from "@/lib/auth";
import { connectDB } from "@/lib/mongodb";
import { apakahAdmin } from "@/lib/admin";
import Vendor from "@/models/Vendor";
import LogoutButton from "@/components/LogoutButton";
import DeleteWarungButton from "@/components/DeleteWarungButton";
import KategoriIcon from "@/components/KategoriIcon";
import { labelKategori } from "@/lib/kategori";

export default async function DashboardPage() {
  const session = await getSession();

  if (!session?.user?.id) {
    redirect("/masuk");
  }

  await connectDB();

  const admin = await apakahAdmin(session.user.id);

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
        <div className="flex items-center gap-2">
          {admin ? (
            <Link
              href="/admin"
              className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-sm font-medium text-emerald-700"
            >
              Admin
            </Link>
          ) : null}
          <LogoutButton />
        </div>
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
                className="rounded-xl border border-zinc-200 bg-white p-3"
              >
                <div className="flex items-start gap-3">
                  <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded-lg bg-zinc-100">
                    {vendor.foto ? (
                      <Image
                        src={vendor.foto}
                        alt={`Foto ${vendor.namaToko}`}
                        fill
                        sizes="64px"
                        className="object-cover"
                      />
                    ) : (
                      <div className="flex h-full items-center justify-center text-center text-[10px] leading-tight text-zinc-400">
                        Tanpa foto
                      </div>
                    )}
                  </div>

                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <div className="min-w-0">
                        <p className="line-clamp-1 font-semibold text-zinc-900">
                          {vendor.namaToko}
                        </p>
                        <p className="flex items-center gap-1 text-xs text-zinc-500">
                          <KategoriIcon
                            kategori={vendor.kategori}
                            className="h-3.5 w-3.5"
                          />
                          <span>
                            {`${labelKategori(vendor.kategori)} - ${
                              vendor.jamBuka
                            }-${vendor.jamTutup}`}
                          </span>
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
                    <p className="mt-1 line-clamp-1 text-sm text-zinc-600">
                      {vendor.alamatPatokan}
                    </p>
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}