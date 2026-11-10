"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import dynamic from "next/dynamic";

const MapComponent = dynamic(() => import("@/components/MapComponent"), {
  ssr: false,
  loading: () => (
    <div className="flex h-80 items-center justify-center rounded-xl bg-zinc-100 text-sm text-zinc-500">
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

export default function Home() {
  const [lokasi, setLokasi] = useState(null);
  const [butuhManual, setButuhManual] = useState(false);
  const [vendors, setVendors] = useState([]);
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
      } catch (err) {
        if (ignore) return;
        setError(err.message || "Terjadi kesalahan jaringan");
        setVendors([]);
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
      </header>

      {lokasi ? (
        <div className="mb-4">
          <MapComponent
            center={[lokasi.lat, lokasi.lng]}
            userLocation={[lokasi.lat, lokasi.lng]}
            vendors={vendors}
          />
          <p className="mt-1 text-xs text-zinc-500">
            Peta: &copy; OpenStreetMap contributors
          </p>
        </div>
      ) : (
        <div className="mb-4 rounded-xl border border-zinc-200 bg-white p-4">
          <p className="text-sm text-zinc-700">
            Aktifkan lokasi untuk melihat warung terdekat, atau pilih kawasan
            manual di bawah.
          </p>
        </div>
      )}

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
                className="rounded-full border border-zinc-300 bg-white px-3 py-1.5 text-sm text-zinc-700 active:bg-zinc-100"
              >
                {k.nama}
              </button>
            ))}
          </div>
        </section>
      ) : null}

      <section>
        <h2 className="mb-2 text-sm font-semibold text-zinc-800">
          Warung terdekat
        </h2>

        {loading ? (
          <p className="py-6 text-center text-sm text-zinc-500">
            Memuat warung...
          </p>
        ) : error ? (
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
        ) : !lokasi ? (
          <p className="py-6 text-center text-sm text-zinc-500">
            Menunggu lokasi atau pilihan kawasan.
          </p>
        ) : vendors.length === 0 ? (
          <p className="py-6 text-center text-sm text-zinc-500">
            Belum ada warung dalam radius {RADIUS / 1000} km.
          </p>
        ) : (
          <ul className="space-y-3">
            {vendors.map((vendor) => (
              <li key={vendor._id}>
                <Link
                  href={`/warung/${vendor._id}`}
                  className="block rounded-xl border border-zinc-200 bg-white p-4 active:bg-zinc-50"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className="font-semibold text-zinc-900">
                        {vendor.namaToko}
                      </p>
                      <p className="text-xs text-zinc-500">
                        {vendor.kategori} - {formatJarak(vendor.jarak)}
                      </p>
                    </div>
                    <span
                      className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                        vendor.buka
                          ? "bg-emerald-100 text-emerald-700"
                          : "bg-red-100 text-red-700"
                      }`}
                    >
                      {vendor.buka ? "Buka" : "Tutup"}
                    </span>
                  </div>
                  <p className="mt-1 text-sm text-zinc-600">
                    {vendor.alamatPatokan}
                  </p>
                  <p className="mt-1 text-xs text-zinc-500">
                    {vendor.jamBuka}-{vendor.jamTutup}
                    {vendor.layananAntar ? " - bisa diantar" : ""}
                  </p>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}
