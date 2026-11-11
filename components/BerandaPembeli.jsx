"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import dynamic from "next/dynamic";
import PilihAreaSheet from "@/components/PilihAreaSheet";
import { cekJembatan } from "@/lib/platform";

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

export default function BerandaPembeli() {
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
  const inputCariRef = useRef(null);
  const [jembatan, setJembatan] = useState(null);

  useEffect(() => {
    async function cek() {
      await Promise.resolve();
      setJembatan(await cekJembatan());
    }
    cek();
  }, []);

  useEffect(() => {
    if (cariTerbuka) {
      inputCariRef.current?.focus();
    }
  }, [cariTerbuka]);

  useEffect(() => {
    async function bukaDariQuery() {
      await Promise.resolve();

      const params = new URLSearchParams(window.location.search);
      if (params.get("pilihArea") === "1") {
        setAreaTerbuka(true);
        window.history.replaceState(null, "", "/");
      }
    }

    bukaDariQuery();
  }, []);

  useEffect(() => {
    function buka() {
      setAreaTerbuka(true);
    }

    window.addEventListener("carijajan:pilih-area", buka);
    return () => window.removeEventListener("carijajan:pilih-area", buka);
  }, []);

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
      {jembatan && jembatan !== "web" ? (
        <div
          data-spike={jembatan}
          className="absolute left-3 top-3 z-[1250] rounded-full bg-black/70 px-2 py-1 text-[10px] font-mono text-white"
        >
          SPIKE: {jembatan}
        </div>
      ) : null}
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
    </main>
  );
}