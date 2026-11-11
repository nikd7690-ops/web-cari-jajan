"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

export default function DeleteWarungButton({ vendorId, namaToko }) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function hapus() {
    const yakin = window.confirm(
      `Hapus warung "${namaToko}"? Tindakan ini tidak bisa dibatalkan.`
    );

    if (!yakin) return;

    setError("");
    setLoading(true);

    try {
      const res = await fetch(`/api/vendors/${vendorId}`, {
        method: "DELETE",
      });
      const json = await res.json();

      if (!res.ok) {
        setError(json.error || "Gagal menghapus warung.");
        return;
      }

      router.refresh();
    } catch {
      setError("Gagal menghapus, periksa koneksi lalu coba lagi.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex flex-col items-end">
      <button
        type="button"
        onClick={hapus}
        disabled={loading}
        className="rounded-lg border border-red-300 px-3 py-2 text-sm font-medium text-red-600 active:bg-red-50 disabled:opacity-60"
      >
        {loading ? "Menghapus..." : "Hapus"}
      </button>
      {error ? (
        <p className="mt-1 max-w-44 text-right text-xs text-red-600">{error}</p>
      ) : null}
    </div>
  );
}