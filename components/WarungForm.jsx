"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import dynamic from "next/dynamic";
import FotoInput from "@/components/FotoInput";
import { KATEGORI } from "@/lib/kategori";

const PinMap = dynamic(() => import("@/components/PinMap"), {
  ssr: false,
  loading: () => (
    <div className="flex h-60 items-center justify-center rounded-xl bg-zinc-100 text-sm text-zinc-500">
      Memuat peta...
    </div>
  ),
});

const PUSAT_JEMBER = { lat: -8.1689, lng: 113.702 };

function geserMenu(menus, index, field, value) {
  return menus.map((menu, i) =>
    i === index ? { ...menu, [field]: value, yakin: true } : menu
  );
}

export default function WarungForm({ mode = "tambah", vendorId, initial }) {
  const router = useRouter();

  const [namaToko, setNamaToko] = useState(initial?.namaToko || "");
  const [kategori, setKategori] = useState(initial?.kategori || "warung");
  const [alamatPatokan, setAlamatPatokan] = useState(
    initial?.alamatPatokan || ""
  );
  const [jamBuka, setJamBuka] = useState(initial?.jamBuka || "06:00");
  const [jamTutup, setJamTutup] = useState(initial?.jamTutup || "21:00");
  const [nomorWA, setNomorWA] = useState(initial?.nomorWA || "");
  const [layananAntar, setLayananAntar] = useState(
    Boolean(initial?.layananAntar)
  );
  const [menus, setMenus] = useState(
    initial?.menus?.length
      ? initial.menus.map((menu) => ({
          namaMenu: menu.namaMenu,
          harga: String(menu.harga),
        }))
      : [{ namaMenu: "", harga: "" }]
  );
  const [pin, setPin] = useState({
    lat: initial?.lat ?? PUSAT_JEMBER.lat,
    lng: initial?.lng ?? PUSAT_JEMBER.lng,
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [geoLoading, setGeoLoading] = useState(mode === "tambah");
  const [lokasiInfo, setLokasiInfo] = useState("");
  const [uploadInfo, setUploadInfo] = useState("");
  const [fotoWarung, setFotoWarung] = useState(null);
  const [fotoMenu, setFotoMenu] = useState(null);
  const [adaDraf, setAdaDraf] = useState(false);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiPesan, setAiPesan] = useState("");

  useEffect(() => {
    if (mode !== "tambah") return;

    let ignore = false;

    async function mintaLokasi() {
      await Promise.resolve();

      if (ignore) return;

      if (typeof navigator === "undefined" || !navigator.geolocation) {
        setLokasiInfo(
          "Perangkat tidak mendukung geolokasi. Geser pin manual di peta."
        );
        setGeoLoading(false);
        return;
      }

      navigator.geolocation.getCurrentPosition(
        (pos) => {
          if (ignore) return;
          setPin({ lat: pos.coords.latitude, lng: pos.coords.longitude });
          setLokasiInfo("Pin diisi dari lokasi kamu. Geser bila kurang tepat.");
          setGeoLoading(false);
        },
        () => {
          if (ignore) return;
          setLokasiInfo(
            "Izin lokasi ditolak. Geser pin manual atau tekan 'Gunakan lokasiku'."
          );
          setGeoLoading(false);
        },
        { enableHighAccuracy: true, timeout: 10000 }
      );
    }

    mintaLokasi();

    return () => {
      ignore = true;
    };
  }, [mode]);

  function tambahBaris() {
    setMenus((prev) => [...prev, { namaMenu: "", harga: "" }]);
  }

  function hapusBaris(index) {
    setMenus((prev) =>
      prev.length === 1 ? prev : prev.filter((_, i) => i !== index)
    );
  }

  async function bacaMenu() {
    if (!fotoMenu) {
      setAiPesan("Ambil atau pilih foto menu dulu.");
      return;
    }

    setAiPesan("");
    setAiLoading(true);

    try {
      const formData = new FormData();
      formData.append("file", fotoMenu, "menu.jpg");

      const res = await fetch("/api/menu-dari-foto", {
        method: "POST",
        body: formData,
      });
      const json = await res.json();

      if (!res.ok) {
        setAiPesan(json.error || "AI gagal membaca menu. Isi manual saja.");
        return;
      }

      const items = json.items ?? [];

      if (items.length === 0) {
        setAiPesan("AI tidak menemukan menu. Isi manual saja.");
        return;
      }

      const draf = items.map((item) => ({
        namaMenu: item.namaMenu,
        harga: item.harga === null ? "" : String(item.harga),
        yakin: Boolean(item.yakin),
      }));

      setMenus((prev) => {
        const terisi = prev.filter(
          (menu) => menu.namaMenu.trim() !== "" || menu.harga !== ""
        );
        const gabung = [...terisi, ...draf];
        return gabung.length > 0 ? gabung : [{ namaMenu: "", harga: "" }];
      });
      setAdaDraf(true);
      setAiPesan(`AI membaca ${items.length} menu. Periksa dulu sebelum simpan.`);
    } catch {
      setAiPesan("AI gagal membaca menu. Isi manual saja.");
    } finally {
      setAiLoading(false);
    }
  }

  function gunakanLokasi() {
    if (typeof navigator === "undefined" || !navigator.geolocation) {
      setLokasiInfo(
        "Perangkat tidak mendukung geolokasi. Geser pin manual di peta."
      );
      return;
    }

    setGeoLoading(true);

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setPin({ lat: pos.coords.latitude, lng: pos.coords.longitude });
        setLokasiInfo("Pin diisi dari lokasi kamu. Geser bila kurang tepat.");
        setGeoLoading(false);
      },
      () => {
        setLokasiInfo(
          "Izin lokasi ditolak. Geser pin manual di peta, lokasi tetap bisa diisi."
        );
        setGeoLoading(false);
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }

  async function onSubmit(event) {
    event.preventDefault();
    setError("");

    const adaBarisKosong = menus.some((menu) => {
      const adaNama = menu.namaMenu.trim() !== "";
      const adaHarga = menu.harga !== "";
      return adaNama !== adaHarga;
    });

    if (adaBarisKosong) {
      setError("Lengkapi nama dan harga tiap menu, atau hapus barisnya.");
      return;
    }

    const menuBersih = menus
      .filter((menu) => menu.namaMenu.trim() !== "")
      .map((menu) => ({
        namaMenu: menu.namaMenu.trim(),
        harga: Number(menu.harga),
        tersedia: true,
      }));

    setLoading(true);

    try {
      let fotoUrl = initial?.foto || "";

      if (fotoWarung) {
        setUploadInfo("Mengunggah foto...");
        const formData = new FormData();
        formData.append("file", fotoWarung, "warung.jpg");
        const unggah = await fetch("/api/upload", {
          method: "POST",
          body: formData,
        });
        const hasilUnggah = await unggah.json();

        if (!unggah.ok) {
          setUploadInfo("");
          setError(hasilUnggah.error || "Gagal mengunggah foto.");
          return;
        }

        fotoUrl = hasilUnggah.url;
        setUploadInfo("");
      }

      const payload = {
        namaToko,
        kategori,
        alamatPatokan,
        jamBuka,
        jamTutup,
        nomorWA,
        layananAntar,
        foto: fotoUrl,
        lat: pin.lat,
        lng: pin.lng,
        menus: menuBersih,
      };

      const res = await fetch(
        mode === "edit" ? `/api/vendors/${vendorId}` : "/api/vendors",
        {
          method: mode === "edit" ? "PUT" : "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        }
      );
      const json = await res.json();

      if (!res.ok) {
        setError(json.error || "Gagal menyimpan warung.");
        return;
      }

      router.push("/dashboard");
      router.refresh();
    } catch {
      setError("Gagal menyimpan, periksa koneksi lalu coba lagi.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="space-y-4">
      <div>
        <label className="mb-1 block text-sm font-medium text-zinc-700">
          Nama warung
        </label>
        <input
          type="text"
          required
          value={namaToko}
          onChange={(e) => setNamaToko(e.target.value)}
          className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-emerald-600"
          placeholder="Warung Bu Siti"
        />
      </div>

      <div>
        <label className="mb-1 block text-sm font-medium text-zinc-700">
          Kategori
        </label>
        <select
          value={kategori}
          onChange={(e) => setKategori(e.target.value)}
          className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-emerald-600"
        >
          {KATEGORI.map((item) => (
            <option key={item.value} value={item.value}>
              {item.label}
            </option>
          ))}
        </select>
      </div>

      <div>
        <label className="mb-1 block text-sm font-medium text-zinc-700">
          Alamat / patokan
        </label>
        <input
          type="text"
          required
          value={alamatPatokan}
          onChange={(e) => setAlamatPatokan(e.target.value)}
          className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-emerald-600"
          placeholder="Jl. Kalimantan, dekat masjid"
        />
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="mb-1 block text-sm font-medium text-zinc-700">
            Jam buka
          </label>
          <input
            type="time"
            required
            value={jamBuka}
            onChange={(e) => setJamBuka(e.target.value)}
            className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-emerald-600"
          />
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium text-zinc-700">
            Jam tutup
          </label>
          <input
            type="time"
            required
            value={jamTutup}
            onChange={(e) => setJamTutup(e.target.value)}
            className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-emerald-600"
          />
        </div>
      </div>

      <div>
        <label className="mb-1 block text-sm font-medium text-zinc-700">
          Nomor WhatsApp
        </label>
        <input
          type="tel"
          required
          value={nomorWA}
          onChange={(e) => setNomorWA(e.target.value)}
          className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 outline-none focus:border-emerald-600"
          placeholder="0812xxxxxxx"
        />
        <p className="mt-1 text-xs text-zinc-500">
          Akan disimpan sebagai 62xxxxxxxxxx.
        </p>
      </div>

      <label className="flex items-center gap-2 text-sm text-zinc-700">
        <input
          type="checkbox"
          checked={layananAntar}
          onChange={(e) => setLayananAntar(e.target.checked)}
          className="h-4 w-4 accent-emerald-600"
        />
        Bisa diantar
      </label>

      <div>
        <div className="mb-1 flex items-center justify-between">
          <label className="block text-sm font-medium text-zinc-700">
            Lokasi pin
          </label>
          <button
            type="button"
            onClick={gunakanLokasi}
            disabled={geoLoading}
            className="rounded-lg border border-emerald-600 px-2 py-1 text-xs font-medium text-emerald-700 disabled:opacity-60"
          >
            {geoLoading ? "Mengambil..." : "Gunakan lokasiku"}
          </button>
        </div>
        <PinMap value={pin} onChange={setPin} />
        <p className="mt-1 text-xs text-zinc-500">
          Geser pin bila GPS meleset. Peta: &copy; OpenStreetMap contributors
        </p>
        {lokasiInfo ? (
          <p className="mt-1 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
            {lokasiInfo}
          </p>
        ) : null}
      </div>

      <div>
        <div className="mb-2 flex items-center justify-between">
          <label className="block text-sm font-medium text-zinc-700">Menu</label>
          <button
            type="button"
            onClick={tambahBaris}
            className="rounded-lg border border-zinc-300 px-2 py-1 text-xs font-medium text-zinc-700"
          >
            + Menu
          </button>
        </div>

        <button
          type="button"
          onClick={bacaMenu}
          disabled={aiLoading || !fotoMenu}
          className="mb-2 w-full rounded-lg border border-emerald-600 px-3 py-2 text-sm font-medium text-emerald-700 disabled:opacity-50"
        >
          {aiLoading ? "AI membaca menu..." : "Baca menu dari foto"}
        </button>

        {adaDraf ? (
          <p className="mb-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs font-medium text-amber-800">
            Draf AI, periksa dulu
          </p>
        ) : null}

        {aiPesan ? (
          <p className="mb-2 text-xs text-zinc-600">{aiPesan}</p>
        ) : null}

        <div className="space-y-2">
          {menus.map((menu, index) => (
            <div
              key={index}
              className={`flex items-center gap-2 rounded-lg ${
                menu.yakin === false
                  ? "border border-amber-300 bg-amber-50 p-1"
                  : ""
              }`}
            >
              <input
                type="text"
                aria-label="Nama menu"
                value={menu.namaMenu}
                onChange={(e) =>
                  setMenus((prev) =>
                    geserMenu(prev, index, "namaMenu", e.target.value)
                  )
                }
                className="min-w-0 flex-1 rounded-lg border border-zinc-300 bg-white px-2 py-2 text-sm text-zinc-900 outline-none focus:border-emerald-600"
                placeholder="Nama menu"
              />
              <input
                type="number"
                min="0"
                aria-label="Harga menu"
                value={menu.harga}
                onChange={(e) =>
                  setMenus((prev) =>
                    geserMenu(prev, index, "harga", e.target.value)
                  )
                }
                className={`w-24 rounded-lg border bg-white px-2 py-2 text-sm text-zinc-900 outline-none focus:border-emerald-600 ${
                  menu.yakin === false ? "border-amber-400" : "border-zinc-300"
                }`}
                placeholder="Harga"
              />
              <button
                type="button"
                onClick={() => hapusBaris(index)}
                disabled={menus.length === 1}
                className="rounded-lg border border-red-300 px-2 py-2 text-sm text-red-600 disabled:opacity-40"
                aria-label="Hapus menu"
              >
                x
              </button>
            </div>
          ))}
        </div>
      </div>

      <div className="space-y-4">
        <FotoInput
          label="Foto warung (opsional)"
          hint="Ambil dari kamera atau pilih galeri. Foto dikompres otomatis dan disimpan."
          onChange={setFotoWarung}
        />
        <FotoInput
          label="Foto menu (hanya untuk draf AI)"
          hint="Dibaca AI untuk membuat draf menu. Tidak disimpan. Kalau kamera tidak terbuka, pilih dari galeri."
          onChange={setFotoMenu}
        />
      </div>

      {uploadInfo ? (
        <p className="rounded-lg border border-zinc-200 bg-zinc-50 px-3 py-2 text-sm text-zinc-600">
          {uploadInfo}
        </p>
      ) : null}

      {error ? (
        <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      ) : null}

      <button
        type="submit"
        disabled={loading}
        className="w-full rounded-xl bg-emerald-600 py-3 text-sm font-semibold text-white active:bg-emerald-700 disabled:opacity-60"
      >
        {loading
          ? "Menyimpan..."
          : mode === "edit"
            ? "Simpan perubahan"
            : "Tambah warung"}
      </button>
    </form>
  );
}