"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import dynamic from "next/dynamic";
import PilihAreaSheet from "@/components/PilihAreaSheet";

const MapComponent = dynamic(() => import("@/components/MapComponent"), {
  ssr: false,
  loading: () => (
    <div className="flex h-full w-full items-center justify-center bg-zinc-100 text-sm text-zinc-500">
      Memuat peta...
    </div>
  ),
});

const RADIUS = 3000;
const INDONESIA = { lat: -2.5, lng: 118 };

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

export default function BerandaPembeli({ loggedIn = false }) {
  const router = useRouter();
  const [lokasi, setLokasi] = useState(null);
  const [vendors, setVendors] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const [query, setQuery] = useState("");
  const [cariTerbuka, setCariTerbuka] = useState(false);
  const [areaTerbuka, setAreaTerbuka] = useState(false);
  const [areaAktif, setAreaAktif] = useState("");
  const [geoLoading, setGeoLoading] = useState(false);
  const [pesanLokasi, setPesanLokasi] = useState("");
  const [promptMasuk, setPromptMasuk] = useState(false);
  const inputCariRef = useRef(null);

  useEffect(() => {
    if (cariTerbuka) {
      inputCariRef.current?.focus();
    }
  }, [cariTerbuka]);

  useEffect(() => {
    async function mintaLokasi() {
      await Promise.resolve();

      if (typeof navigator === "undefined" || !navigator.geolocation) {
        return;
      }

      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setLoading(true);
          setLokasi({ lat: pos.coords.latitude, lng: pos.coords.longitude });
          setAreaAktif("Lokasi saya");
        },
        () => {
          // Izin ditolak/tidak tersedia: tetap di tampilan peta Indonesia.
        },
        { enableHighAccuracy: true, timeout: 10000 }
      );
    }

    mintaLokasi();
  }, []);

  useEffect(() => {
    if (!lokasi) return;

    let ignore = false;

    async function muat() {
      try {
        const res = await fetch(
          `/api/vendors?lat=${lokasi.lat}&lng=${lokasi.lng}&radius=${RADIUS}`
        );
        const json = await res.json();

        if (ignore) return;

        if (!res.ok) {
          throw new Error(json.error || "Gagal mengambil data warung");
        }

        setVendors(json.data);
        setSelectedId(json.data[0]?._id ?? null);
      } catch (err) {
        if (ignore) return;
        setError(err.message || "Terjadi kesalahan jaringan");
        setVendors([]);
        setSelectedId(null);
      } finally {
        if (!ignore) setLoading(false);
      }
    }

    muat();

    return () => {
      ignore = true;
    };
  }, [lokasi, reloadKey]);

  const vendorsTampil = useMemo(() => {
    const kata = query.trim().toLowerCase();
    if (!kata) return vendors;
    return vendors.filter((vendor) =>
      vendor.namaToko.toLowerCase().includes(kata)
    );
  }, [vendors, query]);

  const pilihArea = (area) => {
    setError("");
    setLoading(true);
    setPesanLokasi("");
    setLokasi({ lat: area.lat, lng: area.lng });
    setAreaAktif(area.label);
    setAreaTerbuka(false);
  };

  const mintaLokasiUlang = () => {
    setPesanLokasi("");

    if (typeof navigator === "undefined" || !navigator.geolocation) {
      setPesanLokasi("Perangkat tidak mendukung lokasi.");
      return;
    }

    setGeoLoading(true);

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setGeoLoading(false);
        setLoading(true);
        setLokasi({ lat: pos.coords.latitude, lng: pos.coords.longitude });
        setAreaAktif("Lokasi saya");
        setAreaTerbuka(false);
      },
      () => {
        setGeoLoading(false);
        setPesanLokasi("Izin lokasi ditolak. Pilih kawasan manual di bawah.");
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  };

  const targetAkun = loggedIn ? "/dashboard" : "/masuk";
  const pusat = lokasi
    ? [lokasi.lat, lokasi.lng]
    : [INDONESIA.lat, INDONESIA.lng];
  const zoomPeta = lokasi ? 15 : 5;

  const toggleCari = () => {
    if (cariTerbuka) setQuery("");
    setCariTerbuka((terbuka) => !terbuka);
  };

  return (
    <main className="relative h-[100dvh] w-full overflow-hidden bg-zinc-100">
      <div className="peta-beranda absolute inset-0">
        <MapComponent
          center={pusat}
          zoom={zoomPeta}
          userLocation={lokasi ? [lokasi.lat, lokasi.lng] : undefined}
          vendors={vendorsTampil}
          selectedId={selectedId}
          onSelectVendor={setSelectedId}
        />
      </div>

      <div className="absolute inset-x-0 top-0 z-[1050] p-3">
        <div className="mx-auto flex max-w-md items-center justify-end">
          <div
            className={`flex items-center overflow-hidden transition-all duration-300 ${
              cariTerbuka ? "mr-2 flex-1 opacity-100" : "w-0 opacity-0"
            }`}
          >
            <div className="flex w-full items-center gap-2 rounded-full border border-zinc-200 bg-white px-3 py-2 shadow-md">
              {query ? (
                <button
                  type="button"
                  onClick={() => setQuery("")}
                  aria-label="Hapus pencarian"
                  className="shrink-0 rounded-full px-1 text-zinc-500"
                >
                  x
                </button>
              ) : null}
              <input
                ref={inputCariRef}
                type="search"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Cari warung..."
                aria-label="Cari warung"
                className="w-full bg-transparent text-sm text-zinc-900 outline-none"
              />
            </div>
          </div>

          <button
            type="button"
            onClick={toggleCari}
            aria-label={cariTerbuka ? "Tutup pencarian" : "Buka pencarian"}
            aria-expanded={cariTerbuka}
            className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full border border-zinc-200 bg-white text-zinc-700 shadow-md active:bg-zinc-100"
          >
            {cariTerbuka ? (
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                className="h-5 w-5"
                aria-hidden="true"
              >
                <path d="M6 6l12 12M18 6 6 18" />
              </svg>
            ) : (
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
                strokeLinejoin="round"
                className="h-5 w-5"
                aria-hidden="true"
              >
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-3.5-3.5" />
              </svg>
            )}
          </button>
        </div>
      </div>

      {loading ? (
        <div className="absolute inset-0 z-[1020] flex items-center justify-center bg-white/70 text-sm text-zinc-600">
          Memuat warung...
        </div>
      ) : null}

      {!loading && error ? (
        <div className="absolute inset-0 z-[1020] flex items-center justify-center bg-white/85 p-4">
          <div className="max-w-sm rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            {error}
            <button
              onClick={() => {
                setError("");
                setLoading(true);
                setReloadKey((n) => n + 1);
              }}
              className="ml-2 font-medium underline"
            >
              Coba lagi
            </button>
          </div>
        </div>
      ) : null}

      {!loading && !error && lokasi && vendorsTampil.length === 0 ? (
        <div className="absolute inset-0 z-[1020] flex items-center justify-center p-6">
          <p className="max-w-xs rounded-xl bg-white/90 px-4 py-3 text-center text-sm text-zinc-600 shadow">
            {query
              ? "Tidak ada warung yang cocok."
              : `Belum ada warung dalam radius ${RADIUS / 1000} km.`}
          </p>
        </div>
      ) : null}

      <nav className="fixed inset-x-0 bottom-0 z-[1100] border-t border-zinc-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex max-w-md items-center justify-around px-6 pb-[calc(env(safe-area-inset-bottom)+0.5rem)] pt-2">
          <Link
            href="/"
            aria-current="page"
            className="flex flex-1 flex-col items-center gap-0.5 text-emerald-700"
          >
            <IkonPeta />
            <span className="text-[11px] font-medium">Peta</span>
          </Link>

          <button
            type="button"
            onClick={() => setAreaTerbuka(true)}
            className="flex flex-1 flex-col items-center gap-0.5 text-zinc-600"
          >
            <IkonLokasi />
            <span className="text-[11px] font-medium">Lokasi</span>
          </button>

          <button
            type="button"
            onClick={() =>
              loggedIn ? router.push("/dashboard/tambah") : setPromptMasuk(true)
            }
            aria-label="Tambah warung"
            className="-mt-7 flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-emerald-600 text-3xl leading-none text-white shadow-lg active:bg-emerald-700"
          >
            +
          </button>

          <Link
            href={targetAkun}
            className="flex flex-1 flex-col items-center gap-0.5 text-zinc-600"
          >
            <IkonAkun />
            <span className="text-[11px] font-medium">Akun</span>
          </Link>
        </div>
      </nav>

      {areaTerbuka ? (
        <PilihAreaSheet
          lokasiAktif={areaAktif}
          loadingLokasi={geoLoading}
          pesanLokasi={pesanLokasi}
          onGunakanLokasi={mintaLokasiUlang}
          onPilih={pilihArea}
          onClose={() => setAreaTerbuka(false)}
        />
      ) : null}

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
    </main>
  );
}