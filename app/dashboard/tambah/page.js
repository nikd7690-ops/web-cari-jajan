import Link from "next/link";
import { redirect } from "next/navigation";
import { getSession } from "@/lib/auth";
import WarungForm from "@/components/WarungForm";

export default async function TambahWarungPage() {
  const session = await getSession();

  if (!session?.user?.id) {
    redirect("/masuk");
  }

  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-6 pb-24">
      <Link
        href="/dashboard"
        className="text-sm font-medium text-emerald-700 underline"
      >
        &larr; Kembali ke dashboard
      </Link>

      <h1 className="mt-3 mb-4 text-2xl font-bold text-zinc-900">
        Tambah warung
      </h1>

      <WarungForm mode="tambah" />
    </main>
  );
}