"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import dynamic from "next/dynamic";
import Link from "next/link";
import PilihAreaSheet from "@/components/PilihAreaSheet";
import KategoriIcon from "@/components/KategoriIcon";
import { labelKategori } from "@/lib/kategori";
import {
  aturUlangIzinSesi,
  deteksiLokasi,
  mintaLokasi,
  pantauIzinLokasi,
} from "@/lib/geolokasi";

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

function apakahBukaDiWaktu(jamBuka, jamTutup, waktu) {
  if (waktu === "semua" || !jamBuka || !jamTutup) return true;

  const toMinutes = (jam) => {
    const [h, m] = jam.split(":").map(Number);
    return (h || 0) * 60 + (m || 0);
  };

  const b = toMinutes(jamBuka);
  const t = toMinutes(jamTutup);

  if (b === t) return true;

  const targetMulai = waktu === "pagi" ? 300 : waktu === "siang" ? 660 : 1020;
  const targetSelesai = waktu === "pagi" ? 660 : waktu === "siang" ? 1020 : 1440;

  if (t > b) {
    return b < targetSelesai && t > targetMulai;
  } else {
    if (waktu === "malam") return true;
    return t > targetMulai;
  }
}

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
  const [statusLokasi, setStatusLokasi] = useState("");
  const [isNative, setIsNative] = useState(false);
  const [filterWaktu, setFilterWaktu] = useState("semua"); // "semua", "pagi", "siang", "malam"
  const [offline, setOffline] = useState(false);
  const [petaGagal, setPetaGagal] = useState(false);
  const [onboarding, setOnboarding] = useState(true);
  const inputCariRef = useRef(null);
  const lokasiRef = useRef(null);

  useEffect(() => {
    lokasiRef.current = lokasi;
  }, [lokasi]);

  useEffect(() => {
    if (cariTerbuka) {
      inputCariRef.current?.focus();
    }
  }, [cariTerbuka]);

  useEffect(() => {
    async function cek() {
      await Promise.resolve();
      setOffline(!navigator.onLine);
    }
    cek();

    const online = () => setOffline(false);
    const offlineHandler = () => setOffline(true);
    window.addEventListener("online", online);
    window.addEventListener("offline", offlineHandler);
    return () => {
      window.removeEventListener("online", online);
      window.removeEventListener("offline", offlineHandler);
    };
  }, []);

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

  const jalankanLokasi = useCallback(async () => {
    const det = await deteksiLokasi();
    setStatusLokasi(det.status);
    setIsNative(det.native);

    if (det.status === "insecure" || det.status === "unsupported") {
      setAreaTerbuka(true);
      return;
    }

    try {
      const pos = await mintaLokasi();
      setLoading(true);
      setLokasi(pos);
      setAreaAktif("Lokasi saya");
      setStatusLokasi("granted");
    } catch (e) {
      const kode = e?.kode || "unavailable";
      if (kode === "denied" || kode === "insecure" || kode === "unsupported") {
        setStatusLokasi(kode);
        setLoading(false);
        setAreaTerbuka(true);
      } else {
        setLoading(false);
        setPesanLokasi(e?.message || "Gagal membaca lokasi. Coba lagi.");
      }
    }
  }, []);

  useEffect(() => {
    async function init() {
      await Promise.resolve();

      const pernah = localStorage.getItem("cj:onboarding") === "1";
      if (pernah) {
        setOnboarding(false);
        jalankanLokasi();
        return;
      }

      const det = await deteksiLokasi();
      setStatusLokasi(det.status);
      setIsNative(det.native);
      if (det.status === "granted") {
        setOnboarding(false);
        jalankanLokasi();
      }
    }

    init();
  }, [jalankanLokasi]);

  useEffect(() => {
    return pantauIzinLokasi(async (state) => {
      setStatusLokasi(state);
      if (state === "granted") {
        try {
          const pos = await mintaLokasi();
          setLokasi(pos);
          setAreaAktif("Lokasi saya");
          setLoading(true);
        } catch {
          // biarkan panduan tampil dari statusLokasi
        }
      }
    });
  }, []);

  useEffect(() => {
    if (!isNative) return;

    let batal = false;
    let hapus = () => {};

    (async () => {
      try {
        const { App } = await import("@capacitor/app");
        const listener = await App.addListener("appStateChange", async ({ isActive }) => {
          if (!isActive || batal) return;
          const det = await deteksiLokasi();
          if (batal) return;
          setStatusLokasi(det.status);
          if (det.status === "granted" && !lokasiRef.current) {
            jalankanLokasi();
          }
        });
        if (batal) {
          listener.remove();
        } else {
          hapus = () => listener.remove();
        }
      } catch {
        // abaikan bila plugin tidak tersedia
      }
    })();

    return () => {
      batal = true;
      hapus();
    };
  }, [isNative, jalankanLokasi]);

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
    return vendors.filter((vendor) => {
      const cocokKata = !kata || vendor.namaToko.toLowerCase().includes(kata);
      const cocokWaktu = apakahBukaDiWaktu(vendor.jamBuka, vendor.jamTutup, filterWaktu);
      return cocokKata && cocokWaktu;
    });
  }, [vendors, query, filterWaktu]);

  const tutupArea = useCallback(() => setAreaTerbuka(false), []);

  const pilihArea = (area) => {
    setError("");
    setLoading(true);
    setPesanLokasi("");
    setLokasi({ lat: area.lat, lng: area.lng });
    setAreaAktif(area.label);
    setAreaTerbuka(false);
  };

  const mintaLokasiUlang = async () => {
    setPesanLokasi("");
    setGeoLoading(true);
    if (isNative) aturUlangIzinSesi();

    try {
      const pos = await mintaLokasi({ retryPermission: true });
      setLoading(true);
      setLokasi(pos);
      setAreaAktif("Lokasi saya");
      setStatusLokasi("granted");
      setPesanLokasi("");
      setAreaTerbuka(false);
    } catch (e) {
      const kode = e?.kode || "unavailable";
      if (kode === "denied" || kode === "insecure" || kode === "unsupported") {
        setStatusLokasi(kode);
      } else {
        setPesanLokasi(e?.message || "Gagal membaca lokasi. Coba lagi.");
      }
    } finally {
      setGeoLoading(false);
    }
  };

  const akhiriOnboarding = (izinkan) => {
    try {
      localStorage.setItem("cj:onboarding", "1");
    } catch {
      // abaikan bila storage diblokir
    }
    setOnboarding(false);

    if (izinkan) {
      jalankanLokasi();
    } else {
      setAreaTerbuka(true);
    }
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
      {onboarding ? (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="judul-onboarding"
          className="absolute inset-0 z-[1400] flex flex-col justify-between bg-emerald-700 px-6 py-10 text-white"
        >
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-emerald-100">
              Cari Jajan
            </p>
            <h2 id="judul-onboarding" className="mt-2 text-2xl font-bold">
              Temukan warung terdekat
            </h2>
            <p className="mt-2 text-sm text-emerald-50">
              Izinkan lokasi untuk melihat warung UMKM di sekitarmu otomatis,
              atau pilih kawasan manual.
            </p>
          </div>

          <div className="space-y-2">
            <button
              type="button"
              onClick={() => akhiriOnboarding(true)}
              className="w-full rounded-xl bg-white py-3 text-sm font-semibold text-emerald-700 active:bg-emerald-50"
            >
              Izinkan lokasi
            </button>
            <button
              type="button"
              onClick={() => akhiriOnboarding(false)}
              className="w-full rounded-xl border border-white/40 py-3 text-sm font-medium text-white active:bg-white/10"
            >
              Lewati, pilih area manual
            </button>
            <p className="text-center text-[11px] text-emerald-100">
              Lokasi hanya dipakai untuk mencari warung terdekat dan bisa diubah
              kapan saja lewat menu Lokasi.
            </p>
          </div>
        </div>
      ) : null}
      {petaGagal ? (
        <div className="absolute inset-0 overflow-y-auto p-4 pb-24">
          <p className="mb-3 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
            Peta tidak dapat dimuat. Berikut daftar warung terdekat.
          </p>
          {vendorsTampil.length === 0 ? (
            <p className="py-6 text-center text-sm text-zinc-500">
              Belum ada warung.
            </p>
          ) : (
            <ul className="space-y-2">
              {vendorsTampil.map((vendor) => (
                <li key={vendor._id}>
                  <Link
                    href={`/warung/${vendor._id}`}
                    className="block rounded-xl border border-zinc-200 bg-white p-3"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <p className="font-semibold text-zinc-900">
                        {vendor.namaToko}
                      </p>
                      <span
                        className={`rounded-full px-2 py-0.5 text-[11px] font-medium ${
                          vendor.buka
                            ? "bg-emerald-100 text-emerald-700"
                            : "bg-red-100 text-red-700"
                        }`}
                      >
                        {vendor.buka ? "Buka" : "Tutup"}
                      </span>
                    </div>
                    <p className="mt-0.5 flex items-center gap-1 text-xs text-zinc-500">
                      <KategoriIcon
                        kategori={vendor.kategori}
                        className="h-3.5 w-3.5"
                      />
                      <span>{labelKategori(vendor.kategori)}</span>
                    </p>
                    <p className="mt-1 text-sm text-zinc-600">
                      {vendor.alamatPatokan}
                    </p>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      ) : (
        <div className="peta-beranda absolute inset-0">
          <MapComponent
            center={pusat}
            zoom={zoomPeta}
            userLocation={lokasi ? [lokasi.lat, lokasi.lng] : undefined}
            vendors={vendorsTampil}
            selectedId={selectedId}
            onSelectVendor={setSelectedId}
            onGagal={() => setPetaGagal(true)}
          />
        </div>
      )}

      <div className="absolute inset-x-0 top-0 z-[1050] p-3">
        <div className="mx-auto flex max-w-md items-center justify-between gap-2">
          {/* Filter Waktu: Pagi, Siang, Malam (Hanya Ikon) */}
          <div className="flex items-center gap-1 rounded-full border border-zinc-200 bg-white/95 p-1 shadow-md backdrop-blur">
            {[
              { id: "semua", label: "Semua", icon: "🕒" },
              { id: "pagi", label: "Pagi", icon: "🌅" },
              { id: "siang", label: "Siang", icon: "☀️" },
              { id: "malam", label: "Malam", icon: "🌙" },
            ].map((t) => (
              <button
                key={t.id}
                type="button"
                onClick={() => setFilterWaktu(t.id)}
                title={t.label}
                aria-label={t.label}
                className={`flex h-8 w-8 items-center justify-center rounded-full text-sm font-semibold transition-all ${
                  filterWaktu === t.id
                    ? "bg-emerald-600 text-white shadow-sm"
                    : "text-zinc-600 hover:bg-zinc-100"
                }`}
              >
                <span>{t.icon}</span>
              </button>
            ))}
          </div>

          <div className="flex items-center justify-end">
            <div
              className={`flex items-center overflow-hidden transition-all duration-300 ${
                cariTerbuka ? "mr-2 w-44 opacity-100" : "w-0 opacity-0"
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
    </div>

      {offline ? (
        <div className="absolute inset-x-0 top-16 z-[1100] px-3">
          <p className="mx-auto max-w-md rounded-full bg-amber-500/95 px-3 py-1.5 text-center text-xs font-medium text-white shadow">
            Mode offline: menampilkan data warung terakhir yang tersimpan.
          </p>
        </div>
      ) : null}

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
          statusLokasi={statusLokasi}
          isNative={isNative}
          onGunakanLokasi={mintaLokasiUlang}
          onPilih={pilihArea}
          onClose={tutupArea}
        />
      ) : null}
    </main>
  );
}