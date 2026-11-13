import { connectDB } from "@/lib/mongodb";
import Report from "@/models/Report";
import Vendor from "@/models/Vendor";
import User from "@/models/User";
import LaporanAdminList from "@/components/LaporanAdminList";

export const metadata = {
  title: "Admin Laporan - Cari Jajan",
};

export default async function AdminLaporanPage() {
  await connectDB();

  const reports = await Report.find({})
    .sort({ createdAt: -1 })
    .populate({ path: "vendorId", select: "nama alamat status foto" })
    .populate({ path: "userId", select: "name email" })
    .lean();

  const data = JSON.parse(JSON.stringify(reports));

  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-5">
      <h1 className="text-xl font-bold text-zinc-900">Laporan Komunitas</h1>
      <p className="mt-0.5 text-sm text-zinc-600">
        Periksa laporan warung yang tidak sesuai dengan titik peta, tutup permanen, atau kesalahan data.
      </p>

      <div className="mt-5">
        <LaporanAdminList initialReports={data} />
      </div>
    </main>
  );
}
