"use client";

import { useEffect, useRef, useState } from "react";
import Image from "next/image";

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

async function kompresGambar(file) {
  const url = URL.createObjectURL(file);

  try {
    const img = await muatGambar(url);
    const skala = Math.min(1, MAX_SISI / Math.max(img.width, img.height));
    const lebar = Math.max(1, Math.round(img.width * skala));
    const tinggi = Math.max(1, Math.round(img.height * skala));

    const canvas = document.createElement("canvas");
    canvas.width = lebar;
    canvas.height = tinggi;

    const ctx = canvas.getContext("2d");
    ctx.drawImage(img, 0, 0, lebar, tinggi);

    const blob = await new Promise((resolve) =>
      canvas.toBlob(resolve, "image/jpeg", KUALITAS)
    );

    return blob;
  } finally {
    URL.revokeObjectURL(url);
  }
}

export default function FotoInput({ label, hint, onChange }) {
  const [preview, setPreview] = useState("");
  const [proses, setProses] = useState(false);
  const [pesan, setPesan] = useState("");
  const kameraRef = useRef(null);
  const galeriRef = useRef(null);
  const previewRef = useRef("");

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

    try {
      const blob = await kompresGambar(file);

      if (!blob) {
        setPesan("Gagal memproses gambar, coba foto lain.");
        return;
      }

      pasangPreview(blob);
      onChange(blob);
    } catch {
      setPesan("Gagal memproses gambar, coba foto lain.");
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

  return (
    <div>
      <p className="text-sm font-medium text-zinc-700">{label}</p>
      {hint ? <p className="mb-1 text-xs text-zinc-500">{hint}</p> : null}

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
          onClick={() => kameraRef.current?.click()}
          disabled={proses}
          className="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-medium text-white disabled:opacity-60"
        >
          Ambil foto
        </button>
        <button
          type="button"
          onClick={() => galeriRef.current?.click()}
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