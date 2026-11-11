"use client";

import { useEffect, useState } from "react";

const KAWASAN_JEMBER = [
  { nama: "Alun-alun Jember", lat: -8.1689, lng: 113.702 },
  { nama: "Tegal Boto", lat: -8.164, lng: 113.717 },
  { nama: "Kaliwates", lat: -8.183, lng: 113.677 },
  { nama: "Sumbersari", lat: -8.158, lng: 113.715 },
  { nama: "Patrang", lat: -8.145, lng: 113.7 },
  { nama: "Pasar Tanjung", lat: -8.175, lng: 113.708 },
];

function panduanIzin(status, native) {
  if (status === "denied") {
    return native
      ? "Izin lokasi ditolak. Buka Pengaturan HP → Aplikasi → Cari Jajan → Izin → Lokasi → Izinkan, lalu coba lagi."
      : "Izin lokasi diblokir untuk situs ini. Buka ikon gembok di address bar → Lokasi → Izinkan, lalu muat ulang.";
  }
  if (status === "insecure") {
    return "Lokasi hanya aktif lewat HTTPS. Silakan pilih kawasan manual.";
  }
  if (status === "unsupported") {
    return "Perangkat tidak mendukung geolokasi. Silakan pilih kawasan manual.";
  }
  return "";
}

export default function PilihAreaSheet({
  lokasiAktif,
  loadingLokasi,
  pesanLokasi,
  statusLokasi,
  isNative,
  onGunakanLokasi,
  onPilih,
  onClose,
}) {
  const [query, setQuery] = useState("");
  const [hasil, setHasil] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const kata = query.trim();
    let ignore = false;

    const timer = setTimeout(async () => {
      if (kata.length < 3) {
        if (ignore) return;
        setHasil([]);
        setError("");
        return;
      }

      setLoading(true);
      setError("");

      try {
        const res = await fetch(`/api/geocode?q=${encodeURIComponent(kata)}`);
        const json = await res.json();

        if (ignore) return;

        if (!res.ok) {
          setError(json.error || "Gagal mencari kawasan.");
          setHasil([]);
          return;
        }

        setHasil(json.data ?? []);
      } catch {
        if (ignore) return;
        setError("Gagal mencari kawasan. Periksa koneksi lalu coba lagi.");
        setHasil([]);
      } finally {
        if (!ignore) setLoading(false);
      }
    }, 500);

    return () => {
      ignore = true;
      clearTimeout(timer);
    };
  }, [query]);

  return (
    <div
      className="fixed inset-0 z-[1250] flex items-end bg-black/40"
      onClick={onClose}
      role="presentation"
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="judul-pilih-area"
        onClick={(e) => e.stopPropagation()}
        className="max-h-[82vh] w-full overflow-y-auto rounded-t-2xl bg-white p-4 pb-[calc(env(safe-area-inset-bottom)+1rem)] shadow-xl"
      >
        <div className="mx-auto mb-3 h-1.5 w-10 rounded-full bg-zinc-300" />

        <div className="flex items-start justify-between gap-2">
          <div>
            <h2 id="judul-pilih-area" className="text-lg font-bold text-zinc-900">
              Pilih area
            </h2>
            <p className="text-xs text-zinc-500">
              {lokasiAktif ? `Area aktif: ${lokasiAktif}` : "Belum ada area dipilih"}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Tutup pilih area"
            className="rounded-full px-2 py-1 text-zinc-500 active:bg-zinc-100"
          >
            Tutup
          </button>
        </div>

        <button
          type="button"
          onClick={onGunakanLokasi}
          disabled={
            loadingLokasi ||
            statusLokasi === "insecure" ||
            statusLokasi === "unsupported"
          }
          className="mt-3 w-full rounded-xl bg-emerald-600 py-3 text-sm font-semibold text-white active:bg-emerald-700 disabled:opacity-60"
        >
          {loadingLokasi ? "Mengambil lokasi..." : "Gunakan lokasiku"}
        </button>

        {panduanIzin(statusLokasi, isNative) ? (
          <p className="mt-2 rounded-lg border border-sky-200 bg-sky-50 px-3 py-2 text-xs text-sky-800">
            {panduanIzin(statusLokasi, isNative)}
          </p>
        ) : null}

        {pesanLokasi ? (
          <p className="mt-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
            {pesanLokasi}
          </p>
        ) : null}

        <div className="mt-4">
          <label
            htmlFor="cari-area"
            className="mb-1 block text-sm font-medium text-zinc-700"
          >
            Cari kawasan
          </label>
          <input
            id="cari-area"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Contoh: Kaliwates, Bandung, Surabaya"
            className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-emerald-600"
          />

          {loading ? (
            <p className="mt-2 text-xs text-zinc-500">Mencari kawasan...</p>
          ) : null}

          {error ? (
            <p className="mt-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
              {error}
            </p>
          ) : null}

          {hasil.length > 0 ? (
            <ul className="mt-2 space-y-1">
              {hasil.map((item, index) => (
                <li key={`${item.lat}-${item.lng}-${index}`}>
                  <button
                    type="button"
                    onClick={() =>
                      onPilih({ lat: item.lat, lng: item.lng, label: item.nama })
                    }
                    className="w-full rounded-lg border border-zinc-200 px-3 py-2 text-left text-sm text-zinc-700 active:bg-zinc-50"
                  >
                    <span className="line-clamp-2">{item.nama}</span>
                  </button>
                </li>
              ))}
            </ul>
          ) : null}

          {!loading && !error && query.trim().length >= 3 && hasil.length === 0 ? (
            <p className="mt-2 text-xs text-zinc-500">
              Tidak ada kawasan yang cocok.
            </p>
          ) : null}
        </div>

        <div className="mt-4">
          <p className="mb-2 text-sm font-medium text-zinc-700">
            Kawasan cepat (Jember)
          </p>
          <div className="flex flex-wrap gap-2">
            {KAWASAN_JEMBER.map((k) => (
              <button
                key={k.nama}
                type="button"
                onClick={() =>
                  onPilih({ lat: k.lat, lng: k.lng, label: k.nama })
                }
                className="rounded-full border border-zinc-300 bg-white px-3 py-1.5 text-xs text-zinc-700 active:bg-zinc-100"
              >
                {k.nama}
              </button>
            ))}
          </div>
        </div>

        <p className="mt-4 text-[11px] text-zinc-500">
          Pencarian area: &copy; kontributor OpenStreetMap (Nominatim)
        </p>
      </div>
    </div>
  );
}