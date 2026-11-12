import { connectDB } from "@/lib/mongodb";
import Vendor from "@/models/Vendor";
import KurasiList from "@/components/KurasiList";

export default async function AdminKurasiPage() {
  await connectDB();

  const vendors = await Vendor.find({ status: "pending" })
    .sort({ createdAt: 1 })
    .limit(100)
    .populate({ path: "submittedBy", select: "name email" })
    .lean();

  const data = JSON.parse(JSON.stringify(vendors));

  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-5">
      <h1 className="text-xl font-bold text-zinc-900">Kurasi warung</h1>
      <p className="mt-0.5 text-sm text-zinc-600">
        Setujui warung agar tayang di peta, atau tolak bila data tidak sesuai.
      </p>

      <div className="mt-4">
        <KurasiList vendors={data} />
      </div>
    </main>
  );
}
