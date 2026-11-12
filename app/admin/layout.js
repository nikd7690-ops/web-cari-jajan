import Link from "next/link";
import { redirect } from "next/navigation";
import { getSession } from "@/lib/auth";
import { apakahAdmin } from "@/lib/admin";

export const metadata = {
  title: "Admin - Cari Jajan",
};

export default async function AdminLayout({ children }) {
  const session = await getSession();

  if (!session?.user?.id) {
    redirect("/masuk");
  }

  if (!(await apakahAdmin(session.user.id))) {
    redirect("/");
  }

  return (
    <div className="min-h-[100dvh] bg-zinc-50 pb-24">
      <header className="border-b border-zinc-200 bg-white">
        <div className="mx-auto w-full max-w-2xl px-4 py-3">
          <div className="flex items-center justify-between gap-3">
            <p className="text-sm font-semibold uppercase tracking-wide text-emerald-700">
              Admin
            </p>
            <Link
              href="/dashboard"
              className="text-sm font-medium text-zinc-600 underline"
            >
              Dashboard
            </Link>
          </div>
          <nav className="mt-3 flex gap-2" aria-label="Navigasi admin">
            <Link
              href="/admin"
              className="rounded-lg bg-emerald-50 px-3 py-2 text-sm font-medium text-emerald-700"
            >
              Overview
            </Link>
            <Link
              href="/admin/kurasi"
              className="rounded-lg px-3 py-2 text-sm font-medium text-zinc-700 active:bg-zinc-100"
            >
              Kurasi
            </Link>
          </nav>
        </div>
      </header>

      {children}
    </div>
  );
}