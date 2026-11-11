"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";

function IkonPeta({ className = "h-5 w-5" }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden="true"
    >
      <path d="M9 4 3 6v14l6-2 6 2 6-2V4l-6 2-6-2Z" />
      <path d="M9 4v14M15 6v14" />
    </svg>
  );
}

function IkonLokasi({ className = "h-5 w-5" }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden="true"
    >
      <path d="M12 21s7-6 7-12a7 7 0 1 0-14 0c0 6 7 12 7 12Z" />
      <circle cx="12" cy="9" r="2.5" />
    </svg>
  );
}

function IkonAkun({ className = "h-5 w-5" }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden="true"
    >
      <circle cx="12" cy="8" r="3.5" />
      <path d="M5 20a7 7 0 0 1 14 0" />
    </svg>
  );
}

export default function BottomNav({ loggedIn = false }) {
  const pathname = usePathname();
  const router = useRouter();
  const [promptMasuk, setPromptMasuk] = useState(false);

  if (pathname.startsWith("/warung")) return null;

  const diBeranda = pathname === "/";
  const diAkun =
    pathname.startsWith("/dashboard") || pathname === "/masuk";

  function bukaLokasi() {
    if (diBeranda) {
      window.dispatchEvent(new Event("carijajan:pilih-area"));
      return;
    }

    router.push("/?pilihArea=1");
  }

  function tambah() {
    if (loggedIn) {
      router.push("/dashboard/tambah");
      return;
    }

    setPromptMasuk(true);
  }

  return (
    <>
      <nav className="fixed inset-x-0 bottom-0 z-[1100] border-t border-zinc-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex max-w-md items-center justify-around px-4 pb-[calc(env(safe-area-inset-bottom)+0.5rem)] pt-2">
          <Link
            href="/"
            aria-current={diBeranda ? "page" : undefined}
            className={`flex flex-1 flex-col items-center gap-0.5 ${
              diBeranda ? "text-emerald-700" : "text-zinc-600"
            }`}
          >
            <IkonPeta />
            <span className="text-[11px] font-medium">Peta</span>
          </Link>

          <button
            type="button"
            onClick={bukaLokasi}
            className="flex flex-1 flex-col items-center gap-0.5 text-zinc-600"
          >
            <IkonLokasi />
            <span className="text-[11px] font-medium">Lokasi</span>
          </button>

          <button
            type="button"
            onClick={tambah}
            aria-label="Tambah warung"
            className="-mt-7 flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-emerald-600 text-3xl leading-none text-white shadow-lg active:bg-emerald-700"
          >
            +
          </button>

          <Link
            href={loggedIn ? "/dashboard" : "/masuk"}
            aria-current={diAkun ? "page" : undefined}
            className={`flex flex-1 flex-col items-center gap-0.5 ${
              diAkun ? "text-emerald-700" : "text-zinc-600"
            }`}
          >
            <IkonAkun />
            <span className="text-[11px] font-medium">Akun</span>
          </Link>
        </div>
      </nav>

      {promptMasuk ? (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="judul-prompt-masuk"
          className="fixed inset-0 z-[1200] flex items-center justify-center bg-black/40 p-4"
        >
          <div className="w-full max-w-xs rounded-2xl bg-white p-5 shadow-xl">
            <h2
              id="judul-prompt-masuk"
              className="text-base font-semibold text-zinc-900"
            >
              Masuk dulu
            </h2>
            <p className="mt-1 text-sm text-zinc-600">
              Kamu perlu masuk untuk menambah warung.
            </p>
            <div className="mt-4 flex gap-2">
              <button
                type="button"
                onClick={() => setPromptMasuk(false)}
                className="flex-1 rounded-lg border border-zinc-300 px-3 py-2 text-sm font-medium text-zinc-700"
              >
                Nanti
              </button>
              <Link
                href="/masuk"
                className="flex-1 rounded-lg bg-emerald-600 px-3 py-2 text-center text-sm font-semibold text-white"
              >
                Masuk
              </Link>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}