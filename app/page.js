"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import dynamic from "next/dynamic";

const MapComponent = dynamic(() => import("@/components/MapComponent"), {
  ssr: false,
  loading: () => (
    <div className="flex h-[420px] w-full items-center justify-center rounded-xl bg-zinc-100 text-sm text-zinc-500">
      Memuat peta...
    </div>
  ),
});

const KAWASAN = [
  { nama: "Alun-alun Jember", lat: -8.1689, lng: 113.702 },
  { nama: "Tegal Boto", lat: -8.164, lng: 113.717 },
  { nama: "Kaliwates", lat: -8.183, lng: 113.677 },
  { nama: "Sumbersari", lat: -8.158, lng: 113.715 },
  { nama: "Patrang", lat: -8.145, lng: 113.7 },
  { nama: "Pasar Tanjung", lat: -8.175, lng: 113.708 },
];

const RADIUS = 3000;

function formatJarak(meter) {
  if (meter < 1000) return `${Math.round(meter)} m`;
  return `${(meter / 1000).toFixed(1)} km`;
}

function KartuVendor({ vendor, aktif, onPilih }) {
  return (
    <div
      id={`kartu-${vendor._id}`}
      onClick={() => onPilih(vendor._id)}
      className={`w-60 shrink-0 snap-start cursor-pointer overflow-hidden rounded-xl border bg-white shadow-lg transition ${
        aktif
          ? "border-emerald-500 ring-2 ring-emerald-500"
          : "border-zinc-200"
      }`}
    >
      <div className="relative h-24 w-full bg-zinc-100">
        {vendor.foto ? (
          <Image
            src={vendor.foto}
            alt={`Foto ${vendor.namaToko}`}
            fill
            sizes="240px"
            className="object-cover"
          />
        ) : (
          <div className="flex h-full items-center justify-center text-xs text-zinc-400">
            Tanpa foto
          </div>
        )}
      </div>

      <div className="p-3">
        <div className="flex items-start justify-between gap-2">
          <p className="line-clamp-1 text-sm font-semibold text-zinc-900">
            {vendor.namaToko}
          </p>
          <span
            className={`shrink-0 rounded-full px-2 py-0.5 text-[11px] font-medium ${
              vendor.buka
                ? "bg-emerald-100 text-emerald-700"
                : "bg-red-100 text-red-700"
            }`}
          >
            {vendor.buka ? "Buka" : "Tutup"}
          </span>
        </div>
        <p className="text-xs text-zinc-500">
          {vendor.kategori} - {formatJarak(vendor.jarak)}
        </p>
        <p className="line-clamp-1 text-xs text-zinc-600">
          {vendor.alamatPatokan}
        </p>

        <Link
          href={`/warung/${vendor._id}`}
          onClick={(e) => e.stopPropagation()}
          className="mt-2 inline-block rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white active:bg-emerald-700"
        >
          Lihat &amp; pesan
        </Link>
      </div>
    </div>
  );
}

export default function Home() {
  const [lokasi, setLokasi] = useState(null);
  const [butuhManual, setButuhManual] = useState(false);
  const [vendors, setVendors] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    async function mintaLokasi() {
      await Promise.resolve();

      if (typeof navigator === "undefined" || !navigator.geolocation) {
        setButuhManual(true);
        return;
      }

      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setButuhManual(false);
          setLoading(true);
          setLokasi({ lat: pos.coords.latitude, lng: pos.coords.longitude });
        },
        () => {
          setButuhManual(true);
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

  const pilihLokasi = (titik) => {
    setError("");
    setLoading(true);
    setLokasi(titik);
  };

  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-6">
      <header className="mb-4">
        <h1 className="text-2xl font-bold text-zinc-900">Cari Jajan</h1>
        <p className="text-sm text-zinc-600">
          Warung UMKM terdekat di sekitarmu, pesan langsung via WhatsApp.
        </p>
        <p className="mt-1 text-xs text-zinc-500">
          Punya warung?{" "}
          <Link
            href="/masuk"
            className="font-medium text-emerald-700 underline"
          >
            Masuk
          </Link>{" "}
          atau{" "}
          <Link
            href="/daftar"
            className="font-medium text-emerald-700 underline"
          >
            Daftar
          </Link>
        </p>
      </header>

      {lokasi ? (
        <div className="relative mb-2 h-[420px] w-full overflow-hidden rounded-xl border border-zinc-200">
          <MapComponent
            center={[lokasi.lat, lokasi.lng]}
            userLocation={[lokasi.lat, lokasi.lng]}
            vendors={vendors}
            selectedId={selectedId}
            onSelectVendor={setSelectedId}
          />

          {loading ? (
            <div className="absolute inset-0 z-[900] flex items-center justify-center bg-white/70 text-sm text-zinc-600">
              Memuat warung...
            </div>
          ) : null}

          {!loading && error ? (
            <div className="absolute inset-0 z-[900] flex items-center justify-center bg-white/85 p-4">
              <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
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

          {!loading && !error && vendors.length === 0 ? (
            <div className="absolute inset-0 z-[900] flex items-center justify-center bg-white/75 p-4 text-center text-sm text-zinc-600">
              Belum ada warung dalam radius {RADIUS / 1000} km.
            </div>
          ) : null}

          {!loading && !error && vendors.length > 0 ? (
            <div className="pointer-events-none absolute inset-x-0 bottom-0 z-[1000] bg-gradient-to-t from-black/30 to-transparent pt-10">
              <div className="pointer-events-auto flex snap-x snap-mandatory gap-3 overflow-x-auto px-3 pb-3 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
                {vendors.map((vendor) => (
                  <KartuVendor
                    key={vendor._id}
                    vendor={vendor}
                    aktif={selectedId === vendor._id}
                    onPilih={setSelectedId}
                  />
                ))}
              </div>
            </div>
          ) : null}
        </div>
      ) : (
        <div className="mb-4 rounded-xl border border-zinc-200 bg-white p-4">
          <p className="text-sm text-zinc-700">
            Aktifkan lokasi untuk melihat warung terdekat, atau pilih kawasan
            manual di bawah.
          </p>
        </div>
      )}

      <p className="mb-4 text-xs text-zinc-500">
        Peta: &copy; OpenStreetMap contributors
      </p>

      {butuhManual ? (
        <section className="mb-4">
          <h2 className="mb-2 text-sm font-semibold text-zinc-800">
            Pilih kawasan manual
          </h2>
          <div className="flex flex-wrap gap-2">
            {KAWASAN.map((k) => (
              <button
                key={k.nama}
                onClick={() => pilihLokasi({ lat: k.lat, lng: k.lng })}
                className="rounded-full border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-700 active:bg-zinc-100"
              >
                {k.nama}
              </button>
            ))}
          </div>
        </section>
      ) : null}
    </main>
  );
}