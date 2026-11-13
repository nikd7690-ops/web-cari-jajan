"use client";

import { useState } from "react";
import Link from "next/link";

const labelAlasan = {
  lokasi_salah: "Titik Lokasi Peta Tidak Sesuai",
  sudah_tutup: "Warung Sudah Tutup / Gulung Tikar",
  menu_salah: "Daftar Menu / Harga Tidak Akurat",
  duplikat: "Warung Duplikat / Ganda",
  lainnya: "Laporan Lainnya",
};

export default function LaporanAdminList({ initialReports = [] }) {
  const [reports, setReports] = useState(initialReports);
  const [loadingId, setLoadingId] = useState(null);
  const [filterStatus, setFilterStatus] = useState("semua");

  const filtered = reports.filter((r) => {
    if (filterStatus === "semua") return true;
    return r.status === filterStatus;
  });

  async function updateStatus(id, newStatus) {
    setLoadingId(id);
    try {
      const res = await fetch(`/api/reports/${id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ status: newStatus }),
      });
      if (res.ok) {
        setReports((prev) =>
          prev.map((r) => (r._id === id ? { ...r, status: newStatus } : r))
        );
      } else {
        alert("Gagal memperbarui status laporan");
      }
    } catch (err) {
      alert("Terjadi kesalahan jaringan");
    } finally {
      setLoadingId(null);
    }
  }

  async function hapusLaporan(id) {
    if (!confirm("Hapus laporan ini secara permanen?")) return;
    setLoadingId(id);
    try {
      const res = await fetch(`/api/reports/${id}`, {
        method: "DELETE",
      });
      if (res.ok) {
        setReports((prev) => prev.filter((r) => r._id !== id));
      } else {
        alert("Gagal menghapus laporan");
      }
    } catch (err) {
      alert("Terjadi kesalahan jaringan");
    } finally {
      setLoadingId(null);
    }
  }

  return (
    <div className="space-y-4">
      {/* FILTER STATUS */}
      <div className="flex gap-2">
        {["semua", "pending", "selesai", "diabaikan"].map((s) => (
          <button
            key={s}
            onClick={() => setFilterStatus(s)}
            className={`rounded-lg px-3 py-1.5 text-xs font-semibold capitalize transition ${
              filterStatus === s
                ? "bg-zinc-900 text-white"
                : "border border-zinc-200 bg-white text-zinc-600 hover:bg-zinc-50"
            }`}
          >
            {s}
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <div className="rounded-xl border border-zinc-200 bg-white p-8 text-center text-sm text-zinc-500">
          Tidak ada laporan dalam status ini.
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((item) => (
            <div
              key={item._id}
              className="rounded-xl border border-zinc-200 bg-white p-4 shadow-sm"
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <span
                    className={`inline-block rounded px-2 py-0.5 text-[11px] font-semibold uppercase ${
                      item.status === "pending"
                        ? "bg-amber-100 text-amber-800"
                        : item.status === "selesai"
                          ? "bg-emerald-100 text-emerald-800"
                          : "bg-zinc-100 text-zinc-600"
                    }`}
                  >
                    {item.status}
                  </span>
                  <h3 className="mt-1 text-sm font-bold text-zinc-900">
                    {labelAlasan[item.alasan] || item.alasan}
                  </h3>
                </div>

                {item.vendorId ? (
                  <Link
                    href={`/warung/${item.vendorId._id || item.vendorId}`}
                    target="_blank"
                    className="shrink-0 text-xs font-medium text-emerald-600 underline"
                  >
                    Lihat Warung &rarr;
                  </Link>
                ) : null}
              </div>

              {item.vendorId?.nama && (
                <p className="mt-1 text-xs text-zinc-600">
                  Target Warung: <span className="font-semibold text-zinc-800">{item.vendorId.nama}</span>
                  {item.vendorId.alamat ? ` • ${item.vendorId.alamat}` : ""}
                </p>
              )}

              {item.deskripsi && (
                <div className="mt-2 rounded-lg bg-zinc-50 p-2 text-xs text-zinc-700">
                  &ldquo;{item.deskripsi}&rdquo;
                </div>
              )}

              <div className="mt-3 flex items-center justify-between border-t border-zinc-100 pt-2 text-[11px] text-zinc-400">
                <span>
                  Pelapor: {item.userId?.name || item.userId?.email || "Anonim / Pengguna"}
                </span>
                <span>
                  {new Date(item.createdAt).toLocaleDateString("id-ID", {
                    day: "numeric",
                    month: "short",
                    year: "numeric",
                  })}
                </span>
              </div>

              {/* ACTION BUTTONS */}
              <div className="mt-3 flex flex-wrap items-center gap-2 pt-1">
                {item.status !== "selesai" && (
                  <button
                    disabled={loadingId === item._id}
                    onClick={() => updateStatus(item._id, "selesai")}
                    className="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
                  >
                    Tandai Selesai
                  </button>
                )}
                {item.status !== "diabaikan" && (
                  <button
                    disabled={loadingId === item._id}
                    onClick={() => updateStatus(item._id, "diabaikan")}
                    className="rounded-lg border border-zinc-300 px-3 py-1.5 text-xs font-medium text-zinc-700 hover:bg-zinc-50 disabled:opacity-50"
                  >
                    Abaikan
                  </button>
                )}
                <button
                  disabled={loadingId === item._id}
                  onClick={() => hapusLaporan(item._id)}
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
