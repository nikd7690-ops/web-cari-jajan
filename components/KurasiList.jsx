"use client";

import { useState } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import KategoriIcon from "@/components/KategoriIcon";
import { labelKategori } from "@/lib/kategori";

export default function KurasiList({ vendors }) {
  const router = useRouter();
  const [proses, setProses] = useState("");
  const [error, setError] = useState("");
  const [pesan, setPesan] = useState("");

  async function jalankan(id, aksi) {
    setError("");
    setPesan("");
    setProses(`${id}:${aksi}`);

    try {
      const res = await fetch(`/api/admin/vendors/${id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ aksi }),
      });
      const json = await res.json();

      if (!res.ok) {
        throw new Error(json.error || "Gagal memperbarui status warung");
      }

      setPesan(
        aksi === "approve"
          ? "Warung disetujui dan sudah tayang di peta."
          : "Warung ditolak."
      );
      router.refresh();
    } catch (err) {
      setError(err.message || "Terjadi kesalahan jaringan");
    } finally {
      setProses("");
    }
  }

  if (vendors.length === 0) {
    return (
      <p className="rounded-xl border border-zinc-200 bg-white p-4 text-sm text-zinc-600">
        Tidak ada warung yang menunggu kurasi.
      </p>
    );
  }

  return (
    <div className="space-y-3">
      {pesan ? (
        <p className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-xs text-emerald-800">
          {pesan}
        </p>
      ) : null}

      {error ? (
        <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
          {error}
        </p>
      ) : null}

      <ul className="space-y-2">
        {vendors.map((vendor) => {
          const sedangApprove = proses === `${vendor._id}:approve`;
          const sedangReject = proses === `${vendor._id}:reject`;
          const sibuk = Boolean(proses);

          return (
            <li
              key={vendor._id}
              className="rounded-xl border border-zinc-200 bg-white p-3"
            >
              <div className="flex items-start gap-3">
                <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded-lg bg-zinc-100">
                  {vendor.foto ? (
                    <Image
                      src={vendor.foto}
                      alt={`Foto ${vendor.namaToko}`}
                      fill
                      sizes="64px"
                      className="object-cover"
                    />
                  ) : (
                    <div className="flex h-full items-center justify-center text-center text-[10px] leading-tight text-zinc-400">
                      Tanpa foto
                    </div>
                  )}
                </div>

                <div className="min-w-0 flex-1">
                  <p className="line-clamp-1 font-semibold text-zinc-900">
                    {vendor.namaToko}
                  </p>
                  <p className="flex items-center gap-1 text-xs text-zinc-500">
                    <KategoriIcon
                      kategori={vendor.kategori}
                      className="h-3.5 w-3.5"
                    />
                    <span>{labelKategori(vendor.kategori)}</span>
                  </p>
                  <p className="mt-1 line-clamp-2 text-sm text-zinc-600">
                    {vendor.alamatPatokan}
                  </p>
                  <p className="mt-1 text-xs text-zinc-500">
                    Penjual: {vendor.submittedBy?.name || "-"} (
                    {vendor.submittedBy?.email || "-"})
                  </p>
                  <p className="text-xs text-zinc-500">
                    Jam {vendor.jamBuka}-{vendor.jamTutup} &middot;{" "}
                    {vendor.menus.length} menu
                  </p>
                </div>
              </div>

              <div className="mt-3 flex gap-2">
                <button
                  type="button"
                  onClick={() => jalankan(vendor._id, "reject")}
                  disabled={sibuk}
                  className="flex-1 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm font-semibold text-red-700 active:bg-red-100 disabled:opacity-60"
                >
                  {sedangReject ? "Menolak..." : "Reject"}
                </button>
                <button
                  type="button"
                  onClick={() => jalankan(vendor._id, "approve")}
                  disabled={sibuk}
                  className="flex-1 rounded-lg bg-emerald-600 px-3 py-2 text-sm font-semibold text-white active:bg-emerald-700 disabled:opacity-60"
                >
                  {sedangApprove ? "Menyetujui..." : "Approve"}
                </button>
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
