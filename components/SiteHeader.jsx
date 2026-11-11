import Link from "next/link";
import { getSession } from "@/lib/auth";
import LogoutButton from "@/components/LogoutButton";

export default async function SiteHeader() {
  const session = await getSession();

  return (
    <header className="sticky top-0 z-50 border-b border-zinc-200 bg-white/95 backdrop-blur">
      <div className="mx-auto flex min-h-14 w-full max-w-2xl flex-wrap items-center justify-between gap-2 px-4 py-2">
        <Link href="/" className="text-base font-bold text-zinc-900">
          Cari Jajan
        </Link>

        <nav
          aria-label="Navigasi utama"
          className="flex items-center gap-1 sm:gap-2"
        >
          <Link
            href="/"
            className="rounded-lg px-3 py-2 text-sm font-medium text-zinc-700 active:bg-zinc-100"
          >
            Beranda
          </Link>

          {session?.user ? (
            <>
              <Link
                href="/dashboard"
                className="rounded-lg px-3 py-2 text-sm font-medium text-emerald-700 active:bg-emerald-50"
              >
                Dashboard
              </Link>
              <LogoutButton />
            </>
          ) : (
            <>
              <Link
                href="/masuk"
                className="rounded-lg px-3 py-2 text-sm font-medium text-emerald-700 active:bg-emerald-50"
              >
                Masuk
              </Link>
              <Link
                href="/daftar"
                className="rounded-lg bg-emerald-600 px-3 py-2 text-sm font-semibold text-white active:bg-emerald-700"
              >
                Daftar
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}