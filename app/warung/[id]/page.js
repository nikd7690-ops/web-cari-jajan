"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import KategoriIcon from "@/components/KategoriIcon";
import { labelKategori } from "@/lib/kategori";
import {
  buatTeksPesanan,
  formatRupiah,
  hitungJumlahItem,
  hitungTotal,
  kurangItem,
  normalisasiNomorWA,
  tambahItem,
} from "@/lib/keranjang";

export default function DetailWarung() {
  const { id } = useParams();
  const [vendor, setVendor] = useState(null);
  const [keranjang, setKeranjang] = useState([]);
  const [mode, setMode] = useState("Ambil sendiri");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let ignore = false;

    async function muat() {
      try {
        const res = await fetch(`/api/vendors/${id}`);
        const json = await res.json();

        if (ignore) return;

        if (!res.ok) {
          throw new Error(json.error || "Gagal mengambil data warung");
        }

        setVendor(json.data);
        setMode(json.data.layananAntar ? "Diantar" : "Ambil sendiri");
      } catch (err) {
        if (ignore) return;
        setError(err.message || "Terjadi kesalahan jaringan");
      } finally {
        if (!ignore) setLoading(false);
      }
    }

    muat();

    return () => {
      ignore = true;
    };
  }, [id]);

  const total = useMemo(() => hitungTotal(keranjang), [keranjang]);
  const jumlah = useMemo(() => hitungJumlahItem(keranjang), [keranjang]);

  const linkWhatsApp = useMemo(() => {
    if (!vendor || keranjang.length === 0) return "";
    const teks = buatTeksPesanan({
      namaToko: vendor.namaToko,
      keranjang,
      mode,
    });
    return `https://wa.me/${normalisasiNomorWA(
      vendor.nomorWA
    )}?text=${encodeURIComponent(teks)}`;
  }, [vendor, keranjang, mode]);

  if (loading) {
    return (
      <main className="mx-auto w-full max-w-2xl px-4 py-10 text-center text-sm text-zinc-500">
        Memuat warung...
      </main>
    );
  }

  if (error || !vendor) {
    return (
      <main className="mx-auto w-full max-w-2xl px-4 py-10">
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {error || "Warung tidak ditemukan"}
        </div>
        <Link
          href="/"
          className="mt-3 inline-block text-sm font-medium text-emerald-700 underline"
        >
          Kembali ke beranda
        </Link>
      </main>
    );
  }

  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-6">
      <Link
        href="/"
        className="text-sm font-medium text-emerald-700 underline"
      >
        &larr; Kembali
      </Link>

      {vendor.foto ? (
        <div className="relative mt-3 h-48 w-full overflow-hidden rounded-xl border border-zinc-200 bg-zinc-100">
          <Image
            src={vendor.foto}
            alt={`Foto ${vendor.namaToko}`}
            fill
            priority
            sizes="(max-width: 672px) 100vw, 672px"
            className="object-cover"
          />
        </div>
      ) : null}

      <header className="mt-3 mb-4">
        <h1 className="text-2xl font-bold text-zinc-900">{vendor.namaToko}</h1>
        <p className="flex items-center gap-1.5 text-sm text-zinc-600">
          <KategoriIcon kategori={vendor.kategori} className="h-4 w-4" />
          <span>{`${labelKategori(vendor.kategori)} - ${vendor.alamatPatokan}`}</span>
        </p>
        <div className="mt-2 flex flex-wrap items-center gap-2">
          <span
            className={`rounded-full px-2 py-0.5 text-xs font-medium ${
              vendor.buka
                ? "bg-emerald-100 text-emerald-700"
                : "bg-red-100 text-red-700"
            }`}
          >
            {vendor.buka ? "Buka" : "Tutup"}
          </span>
          <span className="text-xs text-zinc-500">
            Jam {vendor.jamBuka}-{vendor.jamTutup}
          </span>
          {!vendor.buka ? (
            <span className="text-xs font-medium text-red-600">
              Warung sedang tutup, pesanan mungkin baru diproses saat buka.
            </span>
          ) : null}
        </div>
      </header>

      <section className="mb-4">
        <h2 className="mb-2 text-sm font-semibold text-zinc-800">Menu</h2>
        {vendor.menus.length === 0 ? (
          <p className="py-4 text-sm text-zinc-500">Belum ada menu.</p>
        ) : (
          <ul className="space-y-2">
            {vendor.menus.map((menu) => {
              const item = keranjang.find((k) => k.id === menu._id);
              const adaDiskon = vendor.zeroWaste?.aktif && vendor.zeroWaste?.diskonPersen > 0;
              const diskonPersen = adaDiskon ? vendor.zeroWaste.diskonPersen : 0;
              const hargaFinal = adaDiskon
                ? Math.round((menu.harga * (100 - diskonPersen)) / 100)
                : menu.harga;

              return (
                <li
                  key={menu._id}
                  className="flex items-center justify-between gap-3 rounded-xl border border-zinc-200 bg-white p-3"
                >
                  <div>
                    <p
                      className={`font-medium ${
                        menu.tersedia
                          ? "text-zinc-900"
                          : "text-zinc-400 line-through"
                      }`}
                    >
                      {menu.namaMenu}
                    </p>
                    {adaDiskon && menu.tersedia ? (
                      <div className="flex items-center gap-2">
                        <span className="text-xs text-zinc-400 line-through">
                          {formatRupiah(menu.harga)}
                        </span>
                        <span className="rounded bg-red-600 px-1.5 py-0.5 text-[10px] font-bold text-white">
                          -{diskonPersen}%
                        </span>
                        <span className="text-sm font-bold text-red-600">
                          {formatRupiah(hargaFinal)}
                        </span>
                      </div>
                    ) : (
                      <p className="text-sm text-zinc-600">
                        {formatRupiah(menu.harga)}
                        {!menu.tersedia ? " - habis" : ""}
                      </p>
                    )}
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => setKeranjang((k) => kurangItem(k, menu._id))}
                      disabled={!item}
                      className="h-8 w-8 rounded-full border border-zinc-300 text-lg leading-none text-zinc-700 disabled:opacity-40"
                      aria-label={`Kurangi ${menu.namaMenu}`}
                    >
                      -
                    </button>
                    <span className="w-6 text-center text-sm font-semibold">
                      {item ? item.qty : 0}
                    </span>
                    <button
                      onClick={() =>
                        setKeranjang((k) =>
                          tambahItem(k, {
                            id: menu._id,
                            namaMenu: menu.namaMenu,
                            harga: hargaFinal,
                          })
                        )
                      }
                      disabled={!menu.tersedia}
                      className="h-8 w-8 rounded-full border border-zinc-300 text-lg leading-none text-zinc-700 disabled:opacity-40"
                      aria-label={`Tambah ${menu.namaMenu}`}
                    >
                      +
                    </button>
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </section>

      <section className="mb-4">
        <h2 className="mb-2 text-sm font-semibold text-zinc-800">
          Opsi pengambilan
        </h2>
        <div className="flex gap-2">
          {(vendor.layananAntar
            ? ["Diantar", "Ambil sendiri"]
            : ["Ambil sendiri"]
          ).map((opsi) => (
            <button
              key={opsi}
              onClick={() => setMode(opsi)}
              className={`rounded-full border px-3 py-1.5 text-sm ${
                mode === opsi
                  ? "border-emerald-600 bg-emerald-50 text-emerald-700"
                  : "border-zinc-300 bg-white text-zinc-700"
              }`}
            >
              {opsi}
            </button>
          ))}
        </div>
      </section>

      <section className="sticky bottom-0 border-t border-zinc-200 bg-white py-3">
        <div className="mb-2 flex items-center justify-between text-sm">
          <span className="text-zinc-600">{jumlah} item</span>
          <span className="font-semibold text-zinc-900">
            Total: {formatRupiah(total)}
          </span>
        </div>

        {keranjang.length === 0 ? (
          <button
            disabled
            className="w-full rounded-xl bg-zinc-200 py-3 text-sm font-semibold text-zinc-500"
          >
            Pilih menu dulu
          </button>
        ) : (
          <a
            href={linkWhatsApp}
            target="_blank"
            rel="noopener noreferrer"
            className="block w-full rounded-xl bg-emerald-600 py-3 text-center text-sm font-semibold text-white active:bg-emerald-700"
          >
            Pesan via WhatsApp
          </a>
        )}
      </section>
    </main>
  );
}
