import { connectDB } from "@/lib/mongodb";
import Vendor from "@/models/Vendor";
import User from "@/models/User";
import AdminWarungList from "@/components/AdminWarungList";

export const metadata = {
  title: "Admin Toko & Warung - Cari Jajan",
};

export default async function AdminWarungPage() {
  await connectDB();

  const vendors = await Vendor.find({})
    .sort({ createdAt: -1 })
    .populate({ path: "submittedBy", select: "name email" })
    .lean();

  const data = JSON.parse(JSON.stringify(vendors));

  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-5">
      <h1 className="text-xl font-bold text-zinc-900">Semua Toko & Warung</h1>
      <p className="mt-0.5 text-sm text-zinc-600">
        Kelola, tinjau, kurasi, atau hapus warung yang terdaftar di peta Cari Jajan.
      </p>

      <div className="mt-5">
        <AdminWarungList initialVendors={data} />
      </div>
    </main>
  );
}
