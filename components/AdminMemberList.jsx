"use client";

import { useState } from "react";

export default function AdminMemberList({ initialMembers = [] }) {
  const [members, setMembers] = useState(initialMembers);
  const [loadingId, setLoadingId] = useState(null);
  const [search, setSearch] = useState("");

  const filtered = members.filter((m) => {
    if (!search) return true;
    const s = search.toLowerCase();
    return (
      m.name?.toLowerCase().includes(s) ||
      m.email?.toLowerCase().includes(s) ||
      m.role?.toLowerCase().includes(s)
    );
  });

  async function toggleBan(member) {
    const aksiBlokir = !member.isBanned;
    const konfirmasi = confirm(
      aksiBlokir
        ? `Blokir akun member "${member.name || member.email}"? Member ini tidak akan dapat login atau mengajukan warung.`
        : `Buka blokir untuk member "${member.name || member.email}"?`
    );
    if (!konfirmasi) return;

    setLoadingId(member._id);
    try {
      const res = await fetch(`/api/admin/members/${member._id}/ban`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ banned: aksiBlokir }),
      });
      const data = await res.json();
      if (res.ok) {
        setMembers((prev) =>
          prev.map((m) =>
            m._id === member._id ? { ...m, isBanned: aksiBlokir } : m
          )
        );
      } else {
        alert(data.error || "Gagal memproses status banned");
      }
    } catch {
      alert("Terjadi kesalahan jaringan");
    } finally {
      setLoadingId(null);
    }
  }

  async function hapusMember(member) {
    const konfirmasi = confirm(
      `Apakah Anda yakin ingin MENGHAPUS PERMANEN akun "${member.name || member.email}"?\n\nTindakan ini tidak dapat dibatalkan.`
    );
    if (!konfirmasi) return;

    setLoadingId(member._id);
    try {
      const res = await fetch(`/api/admin/members/${member._id}`, {
        method: "DELETE",
      });
      const data = await res.json();
      if (res.ok) {
        setMembers((prev) => prev.filter((m) => m._id !== member._id));
      } else {
        alert(data.error || "Gagal menghapus member");
      }
    } catch {
      alert("Terjadi kesalahan jaringan");
    } finally {
      setLoadingId(null);
    }
  }

  return (
    <div className="space-y-3">
      <input
        type="text"
        placeholder="Cari nama, email, atau peran..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="w-full rounded-xl border border-zinc-200 bg-white px-3.5 py-2 text-xs sm:text-sm text-zinc-900 focus:border-emerald-500 focus:outline-none"
      />

      <div className="divide-y divide-zinc-100 overflow-hidden rounded-xl border border-zinc-200 bg-white shadow-sm">
        {filtered.map((m) => (
          <div
            key={m._id}
            className={`flex items-center justify-between p-3 transition ${
              m.isBanned ? "bg-red-50/40" : ""
            }`}
          >
            <div className="min-w-0 flex-1 pr-3">
              <div className="flex flex-wrap items-center gap-1.5">
                <p className="truncate text-sm font-semibold text-zinc-900">
                  {m.name || "Tanpa Nama"}
                </p>
                {m.role === "admin" && (
                  <span className="rounded bg-rose-100 px-1.5 py-0.5 text-[10px] font-bold text-rose-700">
                    Admin
                  </span>
                )}
                {m.isBanned && (
                  <span className="rounded bg-red-100 px-1.5 py-0.5 text-[10px] font-bold text-red-700">
                    DIBLOKIR
                  </span>
                )}
                <span
                  className={`rounded px-1.5 py-0.5 text-[10px] font-medium ${
                    m.tipeAkun === "mitra"
                      ? "bg-emerald-100 text-emerald-700"
                      : "bg-blue-100 text-blue-700"
                  }`}
                >
                  {m.tipeAkun === "mitra" ? "Mitra" : "Member"}
                </span>
              </div>
              <p className="truncate text-xs text-zinc-500">{m.email}</p>
              <p className="mt-0.5 text-[11px] text-zinc-400">
                {m.totalWarung || 0} warung diajukan • Lv {m.level || 1} ({m.xp || 0} XP)
              </p>
            </div>

            {m.role !== "admin" && (
              <div className="flex shrink-0 items-center gap-1.5">
                <button
                  disabled={loadingId === m._id}
                  onClick={() => toggleBan(m)}
                  className={`rounded-lg px-2.5 py-1 text-xs font-semibold transition disabled:opacity-50 ${
                    m.isBanned
                      ? "bg-emerald-600 text-white hover:bg-emerald-700"
                      : "border border-red-300 bg-red-50 text-red-700 hover:bg-red-100"
                  }`}
                >
                  {m.isBanned ? "Buka Blokir" : "Blokir"}
                </button>
                <button
                  disabled={loadingId === m._id}
                  onClick={() => hapusMember(m)}
                  title="Hapus akun member"
                  aria-label="Hapus akun member"
                  className="rounded-lg border border-zinc-200 bg-white p-1 text-zinc-500 hover:border-red-300 hover:bg-red-50 hover:text-red-600 disabled:opacity-50 transition"
                >
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    className="h-4 w-4"
                    aria-hidden="true"
                  >
                    <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M10 11v6M14 11v6" />
                  </svg>
                </button>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
