import Link from "next/link";
import Image from "next/image";
import { redirect } from "next/navigation";
import { getSession } from "@/lib/auth";
import { connectDB } from "@/lib/mongodb";
import { apakahAdmin } from "@/lib/admin";
import User from "@/models/User";
import Vendor from "@/models/Vendor";
import LogoutButton from "@/components/LogoutButton";
import DeleteWarungButton from "@/components/DeleteWarungButton";
import KategoriIcon from "@/components/KategoriIcon";
import { labelKategori } from "@/lib/kategori";

const STATUS_WARUNG = {
  pending: {
    label: "Menunggu kurasi",
    className: "bg-amber-100 text-amber-700",
  },
  approved: {
    label: "Tayang",
    className: "bg-emerald-100 text-emerald-700",
  },
  rejected: {
    label: "Ditolak",
    className: "bg-red-100 text-red-700",
  },
};

export default async function DashboardPage() {
  const session = await getSession();

  if (!session?.user?.id) {
    redirect("/masuk");
  }

  await connectDB();

  const [admin, userProfil] = await Promise.all([
    apakahAdmin(session.user.id),
    User.findById(session.user.id).select("xp level tipeAkun").lean(),
  ]);

  const isMitra = userProfil?.tipeAkun === "mitra";

  const vendors = await Vendor.find(
    isMitra
      ? { owner: session.user.id }
      : { submittedBy: session.user.id }
  )
    .sort({ createdAt: -1 })
    .lean();

  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-6 pb-24">
      <header className="mb-4 flex items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-zinc-900">
            {isMitra ? "Dashboard Mitra Warung" : "Dashboard Pengguna / Member"}
          </h1>
          <p className="text-sm text-zinc-600">
            Halo, {session.user.name || session.user.email} •{" "}
            {isMitra ? "Pemilik Warung Resmi" : "Kontributor Komunitas"}
          </p>
          {!isMitra ? (
            <div className="mt-1 flex items-center gap-2 text-xs">
              <span className="rounded-full bg-blue-50 px-2 py-0.5 font-medium text-blue-700">
                Level {userProfil?.level ?? 1}
              </span>
              <span className="text-zinc-500">{userProfil?.xp ?? 0} XP Total</span>
            </div>
          ) : null}
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
        <div className="mb-2 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-zinc-800">
            {isMitra
              ? "Warung Resmi Milikmu"
              : "Warung yang Pernah Kamu Ajukan / Kontribusikan"}
          </h2>
          {!isMitra ? (
            <Link
              href="/dashboard/tambah"
              className="text-xs font-semibold text-blue-700 underline"
            >
              + Ajukan Warung Baru
            </Link>
          ) : vendors.length === 0 ? (
            <Link
              href="/dashboard/tambah"
              className="text-xs font-semibold text-emerald-700 underline"
            >
              + Daftarkan Warung
            </Link>
          ) : null}
        </div>

        {isMitra && vendors.length >= 1 ? (
          <p className="mb-3 rounded-lg border border-emerald-200 bg-emerald-50/70 p-2.5 text-xs text-emerald-800">
            📌 <strong>Ketentuan Mitra:</strong> 1 akun mitra mengelola 1 warung resmi. Hubungi admin bila ingin mendaftarkan cabang baru.
          </p>
        ) : null}

        {vendors.length === 0 ? (
          <p className="rounded-xl border border-zinc-200 bg-white p-4 text-sm text-zinc-600">
            {isMitra
              ? "Belum ada warung terdaftar. Daftarkan warung resmi Anda sekarang."
              : "Belum ada warung yang diajukan. Temukan UMKM terdekat dan jadilah kontributor pertama!"}
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
                        <div className="flex items-center gap-2">
                          <p className="line-clamp-1 font-semibold text-zinc-900">
                            {vendor.namaToko}
                          </p>
                          <span
                            className={`shrink-0 rounded-full px-2 py-0.5 text-[11px] font-medium ${
                              (STATUS_WARUNG[vendor.status] ??
                                STATUS_WARUNG.pending).className
                            }`}
                          >
                            {
                              (STATUS_WARUNG[vendor.status] ??
                                STATUS_WARUNG.pending).label
                            }
                          </span>
                        </div>
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
                        {isMitra ? (
                          <>
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
                          </>
                        ) : null}
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