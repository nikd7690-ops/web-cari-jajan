"use client";

import { useState } from "react";
import Link from "next/link";
import Image from "next/image";

export default function AdminWarungList({ initialVendors = [] }) {
  const [vendors, setVendors] = useState(initialVendors);
  const [filterStatus, setFilterStatus] = useState("semua");
  const [search, setSearch] = useState("");
  const [loadingId, setLoadingId] = useState(null);

  const filtered = vendors.filter((v) => {
    const cocokStatus = filterStatus === "semua" || v.status === filterStatus;
    const cocokSearch =
      !search ||
      v.nama.toLowerCase().includes(search.toLowerCase()) ||
      v.kategori?.toLowerCase().includes(search.toLowerCase()) ||
      v.alamat?.toLowerCase().includes(search.toLowerCase());
    return cocokStatus && cocokSearch;
  });

  async function updateStatus(id, action) {
    setLoadingId(id);
    try {
      const res = await fetch(`/api/admin/vendors/${id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ action }),
      });
      if (res.ok) {
        const nextStatus = action === "approve" ? "approved" : "rejected";
        setVendors((prev) =>
          prev.map((v) => (v._id === id ? { ...v, status: nextStatus } : v))
        );
      } else {
        alert("Gagal mengubah status warung");
      }
    } catch {
      alert("Terjadi kesalahan jaringan");
    } finally {
      setLoadingId(null);
    }
  }

  async function hapusWarung(id, nama) {
    if (!confirm(`Hapus warung "${nama}" secara permanen? Data menu dan foto akan terhapus.`)) return;
    setLoadingId(id);
    try {
      const res = await fetch(`/api/admin/vendors/${id}`, {
        method: "DELETE",
      });
      if (res.ok) {
        setVendors((prev) => prev.filter((v) => v._id !== id));
      } else {
        alert("Gagal menghapus warung");
      }
    } catch {
      alert("Terjadi kesalahan jaringan");
    } finally {
      setLoadingId(null);
    }
  }

  return (
    <div className="space-y-4">
      {/* SEARCH & FILTER */}
      <div className="flex flex-col gap-2.5 sm:flex-row sm:items-center sm:justify-between">
        <input
          type="text"
          placeholder="Cari warung, kategori, atau alamat..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="rounded-xl border border-zinc-200 bg-white px-3.5 py-2 text-xs sm:text-sm text-zinc-900 focus:border-emerald-500 focus:outline-none"
        />

        <div className="flex flex-wrap gap-1.5">
          {["semua", "approved", "pending", "rejected"].map((s) => (
            <button
              key={s}
              onClick={() => setFilterStatus(s)}
              className={`rounded-lg px-2.5 py-1 text-xs font-semibold capitalize transition ${
                filterStatus === s
                  ? "bg-zinc-900 text-white"
                  : "border border-zinc-200 bg-white text-zinc-600 hover:bg-zinc-50"
              }`}
            >
              {s}
            </button>
          ))}
        </div>
      </div>

      {filtered.length === 0 ? (
        <div className="rounded-xl border border-zinc-200 bg-white p-8 text-center text-sm text-zinc-500">
          Tidak ada warung yang cocok dengan filter.
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((v) => (
            <div
              key={v._id}
              className="rounded-xl border border-zinc-200 bg-white p-4 shadow-sm"
            >
              <div className="flex items-start gap-3">
                {v.foto ? (
                  <div className="relative h-14 w-14 shrink-0 overflow-hidden rounded-lg bg-zinc-100">
                    <img
                      src={v.foto}
                      alt={v.nama}
                      className="h-full w-full object-cover"
                    />
                  </div>
                ) : (
                  <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-lg bg-zinc-100 text-lg">
                    🍲
                  </div>
                )}

                <div className="min-w-0 flex-1">
                  <div className="flex items-center justify-between gap-2">
                    <h3 className="truncate text-sm font-bold text-zinc-900">
                      {v.nama}
                    </h3>
                    <span
                      className={`shrink-0 rounded px-2 py-0.5 text-[10px] font-semibold uppercase ${
                        v.status === "approved"
                          ? "bg-emerald-100 text-emerald-800"
                          : v.status === "pending"
                            ? "bg-amber-100 text-amber-800"
                            : "bg-red-100 text-red-800"
                      }`}
                    >
                      {v.status}
                    </span>
                  </div>

                  <p className="mt-0.5 truncate text-xs text-zinc-500">
                    {v.kategori} • {v.alamat || "Alamat belum diatur"}
                  </p>

                  <p className="mt-0.5 text-[11px] text-zinc-400">
                    Pengaju: {v.submittedBy?.name || v.submittedBy?.email || "Tidak diketahui"} • Jam: {v.jamBuka || "08:00"} - {v.jamTutup || "21:00"}
                  </p>
                </div>
              </div>

              {/* ACTION BUTTONS */}
              <div className="mt-3 flex flex-wrap items-center gap-2 border-t border-zinc-100 pt-2.5">
                <Link
                  href={`/warung/${v._id}`}
                  target="_blank"
                  className="rounded-lg border border-zinc-200 bg-white px-3 py-1 text-xs font-medium text-zinc-700 hover:bg-zinc-50"
                >
                  Detail
                </Link>

                {v.status !== "approved" && (
                  <button
                    disabled={loadingId === v._id}
                    onClick={() => updateStatus(v._id, "approve")}
                    className="rounded-lg bg-emerald-600 px-3 py-1 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
                  >
                    Setujui
                  </button>
                )}

                {v.status !== "rejected" && (
                  <button
                    disabled={loadingId === v._id}
                    onClick={() => updateStatus(v._id, "reject")}
                    className="rounded-lg border border-zinc-300 px-3 py-1 text-xs font-medium text-zinc-700 hover:bg-zinc-50 disabled:opacity-50"
                  >
                    Tolak
                  </button>
                )}

                <button
                  disabled={loadingId === v._id}
                  onClick={() => hapusWarung(v._id, v.nama)}
                  className="ml-auto text-xs font-medium text-red-600 hover:underline disabled:opacity-50"
                >
                  Hapus
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
