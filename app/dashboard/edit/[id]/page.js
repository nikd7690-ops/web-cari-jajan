import Link from "next/link";
import mongoose from "mongoose";
import { redirect, notFound } from "next/navigation";
import { getSession } from "@/lib/auth";
import { connectDB } from "@/lib/mongodb";
import Vendor from "@/models/Vendor";
import WarungForm from "@/components/WarungForm";

export default async function EditWarungPage({ params }) {
  const session = await getSession();

  if (!session?.user?.id) {
    redirect("/masuk");
  }

  const { id } = await params;

  if (!mongoose.Types.ObjectId.isValid(id)) {
    notFound();
  }

  await connectDB();

  const vendor = await Vendor.findById(id).lean();

  if (!vendor) {
    notFound();
  }

  if (vendor.owner.toString() !== session.user.id) {
    redirect("/dashboard");
  }

  const initial = {
    namaToko: vendor.namaToko,
    kategori: vendor.kategori,
    alamatPatokan: vendor.alamatPatokan,
    jamBuka: vendor.jamBuka,
    jamTutup: vendor.jamTutup,
    nomorWA: vendor.nomorWA,
    layananAntar: vendor.layananAntar,
    foto: vendor.foto || "",
    lat: vendor.lokasi.coordinates[1],
    lng: vendor.lokasi.coordinates[0],
    menus: vendor.menus.map((menu) => ({
      namaMenu: menu.namaMenu,
      harga: menu.harga,
    })),
  };

  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-6 pb-24">
      <Link
        href="/dashboard"
        className="text-sm font-medium text-emerald-700 underline"
      >
        &larr; Kembali ke dashboard
      </Link>

      <h1 className="mt-3 mb-4 text-2xl font-bold text-zinc-900">
        Edit warung
      </h1>

      <WarungForm mode="edit" vendorId={id} initial={initial} />
    </main>
  );
}