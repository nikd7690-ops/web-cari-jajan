"use client";

import { useEffect, useRef, useState } from "react";
import Image from "next/image";
import { ambilFoto, pakaiKameraNative } from "@/lib/kamera";

const MAX_SISI = 1280;
const KUALITAS = 0.8;

function muatGambar(src) {
  return new Promise((resolve, reject) => {
    const img = new window.Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error("Gagal memuat gambar"));
    img.src = src;
  });
}

async function kompresDariSrc(src) {
  const img = await muatGambar(src);
  const skala = Math.min(1, MAX_SISI / Math.max(img.width, img.height));
  const lebar = Math.max(1, Math.round(img.width * skala));
  const tinggi = Math.max(1, Math.round(img.height * skala));

  const canvas = document.createElement("canvas");
  canvas.width = lebar;
  canvas.height = tinggi;

  const ctx = canvas.getContext("2d");
  ctx.drawImage(img, 0, 0, lebar, tinggi);

  return new Promise((resolve) =>
    canvas.toBlob(resolve, "image/jpeg", KUALITAS)
  );
}

export default function FotoInput({ label, hint, onChange }) {
  const [preview, setPreview] = useState("");
  const [proses, setProses] = useState(false);
  const [pesan, setPesan] = useState("");
  const kameraRef = useRef(null);
  const galeriRef = useRef(null);
  const previewRef = useRef("");
  const native = pakaiKameraNative();

  useEffect(() => {
    return () => {
      if (previewRef.current) URL.revokeObjectURL(previewRef.current);
    };
  }, []);

  function pasangPreview(blob) {
    if (previewRef.current) URL.revokeObjectURL(previewRef.current);
    const url = URL.createObjectURL(blob);
    previewRef.current = url;
    setPreview(url);
  }

  async function simpanHasil(blob) {
    if (!blob) {
      setPesan("Gagal memproses gambar, coba foto lain.");
      return;
    }
    pasangPreview(blob);
    onChange(blob);
  }

  async function pilih(event) {
    const file = event.target.files?.[0];
    event.target.value = "";

    if (!file) return;

    if (!file.type.startsWith("image/")) {
      setPesan("File harus berupa gambar.");
      return;
    }

    setPesan("");
    setProses(true);

    const url = URL.createObjectURL(file);
    try {
      const blob = await kompresDariSrc(url);
      await simpanHasil(blob);
    } catch {
      setPesan("Gagal memproses gambar, coba foto lain.");
    } finally {
      URL.revokeObjectURL(url);
      setProses(false);
    }
  }

  async function ambilNative(sumber) {
    setPesan("");
    setProses(true);

    try {
      const dataUrl = await ambilFoto({ sumber });
      if (!dataUrl) return;
      const blob = await kompresDariSrc(dataUrl);
      await simpanHasil(blob);
    } catch (e) {
      const teks = String(e?.message || e || "");
      if (/cancel/i.test(teks)) {
        // dibatalkan user, abaikan
      } else if (/permission|denied/i.test(teks)) {
        setPesan("Izin kamera ditolak. Pakai galeri atau izinkan di Pengaturan HP.");
      } else {
        setPesan("Gagal mengambil foto, coba lagi.");
      }
    } finally {
      setProses(false);
    }
  }

  function hapus() {
    if (previewRef.current) URL.revokeObjectURL(previewRef.current);
    previewRef.current = "";
    setPreview("");
    setPesan("");
    onChange(null);
  }

  function klikKamera() {
    if (native) return ambilNative("kamera");
    kameraRef.current?.click();
  }

  function klikGaleri() {
    if (native) return ambilNative("galeri");
    galeriRef.current?.click();
  }

  return (
    <div>
      <p className="text-sm font-medium text-zinc-700">{label}</p>
      {hint ? <p className="mb-1 text-xs text-zinc-500">{hint}</p> : null}

      {!native ? (
        <>
          <input
            ref={kameraRef}
            type="file"
            accept="image/*"
            capture="environment"
            aria-label={`Ambil foto dari kamera untuk ${label}`}
            onChange={pilih}
            className="hidden"
          />
          <input
            ref={galeriRef}
            type="file"
            accept="image/*"
            aria-label={`Pilih foto dari galeri untuk ${label}`}
            onChange={pilih}
            className="hidden"
          />
        </>
      ) : null}

      <div className="mt-1 overflow-hidden rounded-xl border border-zinc-200 bg-zinc-50">
        {preview ? (
          <Image
            src={preview}
            alt="Pratinjau foto"
            width={400}
            height={300}
            unoptimized
            className="h-40 w-full object-cover"
          />
        ) : (
          <div className="flex h-40 items-center justify-center text-xs text-zinc-400">
            Belum ada foto
          </div>
        )}
      </div>

      <div className="mt-2 flex flex-wrap gap-2">
        <button
          type="button"
          onClick={klikKamera}
          disabled={proses}
          className="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-medium text-white disabled:opacity-60"
        >
          Ambil foto
        </button>
        <button
          type="button"
          onClick={klikGaleri}
          disabled={proses}
          className="rounded-lg border border-zinc-300 px-3 py-1.5 text-xs font-medium text-zinc-700 disabled:opacity-60"
        >
          Pilih dari galeri
        </button>
        {preview ? (
          <button
            type="button"
            onClick={hapus}
            className="rounded-lg border border-red-300 px-3 py-1.5 text-xs font-medium text-red-600"
          >
            Hapus
          </button>
        ) : null}
      </div>

      {proses ? (
        <p className="mt-1 text-xs text-zinc-500">Memproses foto...</p>
      ) : null}
      {pesan ? <p className="mt-1 text-xs text-red-600">{pesan}</p> : null}
    </div>
  );
}